package vn.kalapa.ekyc.networks

import android.annotation.SuppressLint
import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import org.json.JSONObject
import retrofit2.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.*
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.managers.AESCryptor
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.CreateSessionResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.MyError
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.FileUtil
import vn.kalapa.ekyc.utils.Helpers
import java.io.BufferedReader
import java.io.IOException
import java.lang.reflect.Type
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException

class JsonResponseBodyConverter<T> : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T? {
        // Create a new buffered reader and String Builder
        val reader = BufferedReader(value.charStream())
        val stringBuilder = StringBuilder()

        // Check if the line we are reading is not null
        var inputLine: String?
        do {
            inputLine = reader.readLine()
            stringBuilder.append(inputLine)
        } while (inputLine != null)

        reader.close()
        value.close()

        val obj = JSONObject(stringBuilder.toString())
        return obj as T
    }
}

class JsonConvertFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return JsonResponseBodyConverter<Any>()
    }
}

class Client {
    companion object {
        fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String
                ) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String
                ) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//                    .addInterceptor(ErrorInterceptor())
//                    .addInterceptor(HeaderInterceptor())
//                    .addInterceptor(HttpLoggingInterceptor().apply {
//                        level =
//                                if (BuildConfig.DEBUG) {
//                                    HttpLoggingInterceptor.Level.BODY
//                                } else {
//                                    HttpLoggingInterceptor.Level.NONE
//                                }
//                    })
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
            builder.addInterceptor(GzipRequestInterceptor())

            return builder
        }
    }

    internal class GzipRequestInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val originalRequest: Request = chain.request()
            if (originalRequest.body == null
                || originalRequest.header("Content-Encoding") != null
            ) {
                return chain.proceed(originalRequest)
            }
            if (originalRequest.header("Content-Type").equals("application/x-protobuf")) {
                val compressedRequest: Request = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method, gzip(originalRequest.body!!))
                    .build()
                return chain.proceed(compressedRequest)
            }
            return chain.proceed(originalRequest)
        }

        private fun gzip(body: RequestBody): RequestBody {
            return object : RequestBody() {
                override fun contentType(): MediaType? {
                    return body.contentType()
                }

                override fun contentLength(): Long {
                    return -1 // We don't know the compressed length in advance!
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    val gzipSink = GzipSink(sink).buffer()
                    body.writeTo(gzipSink)
                    gzipSink.close()
                }
            }
        }
    }

    private var retrofit: Retrofit

    constructor(domain: String, converter: Converter.Factory = JsonConvertFactory()) {
        retrofit = Retrofit.Builder()
            .baseUrl("$domain/")
            .addConverterFactory(converter)
            .client(getUnsafeOkHttpClient().build())
            .build()
    }

    constructor(domain: String) {
        retrofit = Retrofit.Builder()
            .baseUrl("$domain/")
            .addConverterFactory(JsonConvertFactory())
            .client(getUnsafeOkHttpClient().build())
            .build()
    }


    fun post(
        endPoint: String,
        headers: Map<String, String>,
        params: Map<String, Any>,
        listener: RequestListener
    ) {
        val api = retrofit.create(API::class.java)
        Helpers.printLog("Start POST")
        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            KalapaSDK.config.baseURL + endPoint
        }
        val jsonParams = JSONObject(params)
        Helpers.printLog(" PATH: $fullURL \n Authorization ${headers.get("Authorization")}\n ${jsonParams.toString()}")
        var body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonParams.toString())
        Helpers.printLog("Start REQUEST")
        api.post(fullURL, headers, body).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                handleOnResponse(response, listener)
                Helpers.printLog(call.toString())
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                Helpers.printLog(call.toString())
                handleOnFailure(t, listener)
            }
        })
    }

    fun getImage(
        endPoint: String,
        headers: Map<String, String>,
        listener: RequestImageListener,
        postRequest: (() -> Unit)? = null
    ) {
        val api = retrofit.create(API::class.java)

        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            KalapaSDK.config.baseURL + endPoint
        }
        Helpers.printLog("get url:", fullURL)
        Helpers.printLog("headers:", headers)
        api.getImage(fullURL, headers).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val byteArray = response.body()?.bytes()
                    // Handle the byte array here
                    if (byteArray != null) listener.success(byteArray)
                    else listener.fail(KalapaError.UnknownError)
                } else {
                    // Handle unsuccessful response
                    listener.fail(KalapaError.UnknownError)
                }
                if (postRequest != null) postRequest()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                listener.fail(KalapaError.UnknownError)
                if (postRequest != null) postRequest()
            }

        })
    }

    fun get(
        endPoint: String,
        headers: Map<String, String>,
        listener: RequestListener,
        postRequest: (() -> Unit)? = null
    ) {
        val api = retrofit.create(API::class.java)

        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            KalapaSDK.config.baseURL + endPoint
        }
        Helpers.printLog("get url:", fullURL)
        Helpers.printLog("headers:", headers)
        api.get(fullURL, headers).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                handleOnResponse(response, listener)
                if (postRequest != null) postRequest()
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                handleOnFailure(t, listener)
                if (postRequest != null) postRequest()
            }
        })
    }

    fun postFormData(
        endPoint: String,
        headers: Map<String, String>,
        image: Bitmap,
        listener: RequestListener
    ) {
        val api = retrofit.create(API::class.java)

        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            KalapaSDK.config.baseURL + endPoint
        }

        Helpers.printLog("postFormData url:", fullURL)
        Helpers.printLog("headers:", headers)
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        val imageInByteArray = BitmapUtil.convertBitmapToBytes(image)
        // image.convertToByteArray()
        requestBody.addFormDataPart(
            if (endPoint.contains("selfie")) "image" else "image",
            if (endPoint.contains("selfie")) "SELFIE.jpeg" else if (endPoint.contains("front")) "FRONT.jpeg" else "BACK.jpeg",
            imageInByteArray.toRequestBody()
        )
        val hashImage = FileUtil.hash(imageInByteArray)
        val signature = AESCryptor.encryptText(hashImage)
        if ((signature != null) && signature.isNotEmpty()) {
            requestBody.addFormDataPart("signature", signature)
            Helpers.printLog("Hash & Signature: $hashImage $signature")
        }
        api.post(fullURL, headers, requestBody.build()).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                Helpers.printLog("onResponse ${response.code()} ${response.body()}")
                handleOnResponse(response, listener)
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                Helpers.printLog("handleOnFailure ${t.message}")
                handleOnFailure(t, listener)
            }

        })
    }

    private fun handleOnResponse(response: Response<JSONObject>, listener: RequestListener?) {
        if (listener != null) {
            val url = response.raw().request.url
            if (response.code() == 200) {
                try {
                    val errorJson = (response.body() as JSONObject).get("error")
                    val errorCode = (errorJson as JSONObject).getInt("code")
                    Helpers.printLog("request success ${response.body()} - $errorJson")
                    if (errorCode == 401 || errorCode == 403) {
                        listener.fail(KalapaError(-1, "Wrong Token"))
                    } else {
                        listener.success(response.body() as JSONObject)
                    }
                } catch (exception: Exception) {
                    listener.success(response.body() as JSONObject)
                }
            } else if (response.code() == 401) {
                Helpers.printLog("Session is expired ... $url")
                listener.timeout()
            } else if (url.toString().contains("/api/auth/get-token")) {
                if (response.code() == 403 || response.code() == 401) {
                    listener.fail(KalapaError(-1, "Wrong Token"))
                }
            } else if (response.code() == 400) {
                if (response.errorBody()?.string() != null && response.errorBody()!!.string()
                        .isNotEmpty()
                ) {
                    Helpers.printLog(
                        "request fail: response.errorBody() ${response.code()} - ${
                            response.errorBody()?.string()
                        }"
                    )
                    val myError = MyError.fromJson(response.errorBody()?.string()!!)
                    if (myError?.message != null && myError.code != null) {
                        listener.fail(KalapaError(myError.code, myError.message))
                    } else
                        listener.fail(KalapaError.NetworkError)
                } else
                    listener.fail(KalapaError.NetworkError)
            } else {
                Helpers.printLog("request fail ${response.code()} - ${response.body()}")
                listener.fail(KalapaError.NetworkError)
            }
        }
    }

    private fun handleOnFailure(t: Throwable, listener: RequestListener?) {
        t.printStackTrace()

        if (listener != null) {
            val message = t.message as String
            listener.fail(KalapaError(-1, message))
        }
    }

    fun post(
        endPoint: String,
        headers: Map<String, String>,
        params: ByteArray,
        listener: RequestListener
    ) {

        val api = retrofit.create(API::class.java)

        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            KalapaSDK.config.baseURL + endPoint
        }

        Helpers.printLog("post url:", fullURL)
        Helpers.printLog("headers:", headers)
        Helpers.printLog("params:", params.toString())
        val body = RequestBody.create("application/x-protobuf".toMediaTypeOrNull(), params)
        api.post(fullURL, headers, body).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                handleOnResponse(response, listener)
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                handleOnFailure(t, listener)
            }
        })
    }

    private interface API {
        @GET
        fun get(@Url url: String, @HeaderMap headers: Map<String, String>): Call<JSONObject>

        @GET
        fun getImage(@Url url: String, @HeaderMap headers: Map<String, String>): Call<ResponseBody>

        @POST
        fun post(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @Body params: RequestBody
        ): Call<JSONObject>

        @Multipart
        @POST
        fun post(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @Part image: MultipartBody.Part
        ): Call<JSONObject>

    }

    interface RequestListener {
        fun success(jsonObject: JSONObject)
        fun fail(error: KalapaError)
        fun timeout()
    }


    interface RequestImageListener {
        fun success(byteArray: ByteArray)
        fun fail(error: KalapaError)
        fun timeout()
    }

    interface CreateSessionListener {
        fun success(createSessionResult: CreateSessionResult)
        fun fail(error: KalapaError)
        fun timeout()
    }

    interface ConfirmListener {
        fun success(confirmResult: ConfirmResult)
        fun fail(error: KalapaError)
        fun timeout()
    }

}