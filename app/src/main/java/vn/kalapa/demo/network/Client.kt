package vn.kalapa.demo.network

import android.annotation.SuppressLint
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import org.json.JSONObject
import retrofit2.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.*
import vn.kalapa.demo.utils.LogUtils
import vn.kalapa.ekyc.models.*
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
        val baseURL = "https://ekyc-api.kalapa.vn/"
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
        LogUtils.printLog("Start POST")
        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            baseURL + endPoint
        }
        val jsonParams = JSONObject(params)
        LogUtils.printLog(" PATH: $fullURL \n Authorization ${headers.get("Authorization")}\n ${jsonParams.toString()}")
        var body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonParams.toString())
        LogUtils.printLog("Start REQUEST")
        api.post(fullURL, headers, body).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                handleOnResponse(response, listener)
                LogUtils.printLog(call.toString())
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                LogUtils.printLog(call.toString())
                handleOnFailure(t, listener)
            }
        })
    }

    fun get(endPoint: String, headers: Map<String, String>, listener: RequestListener) {
        val api = retrofit.create(API::class.java)

        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            baseURL + endPoint
        }
        LogUtils.printLog("get url:", fullURL)
        LogUtils.printLog("headers:", headers)
//        if (headers.contains("Content-Type") && !headers["Content-Type"].isNullOrEmpty()
//            && headers["Content-Type"].equals("application/x-protobuf")) {
//            val objAsBytes: ByteArray = jsonParams.toString().toByteArray()
//            body = RequestBody.create(MediaType.parse("application/x-protobuf"), objAsBytes)
//        }
        api.get(fullURL, headers).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                handleOnResponse(response, listener)
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                handleOnFailure(t, listener)
            }
        })
    }


    fun postFormData(
        endPoint: String,
        headers: Map<String, String>,
        requestFile: RequestBody,
        requestFileParams: String?,
        listener: RequestListener
    ) {
        val api = retrofit.create(API::class.java)
        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            baseURL + endPoint
        }
        LogUtils.run {
            printLog("postFormData url:", fullURL)
            printLog("headers:", headers)
        }
        val body = MultipartBody.Part.createFormData(requestFileParams ?: "image", "${requestFileParams ?: "image"}.png", requestFile)
        LogUtils.printLog("post url:", fullURL)
        LogUtils.printLog("headers:", headers)
        api.post(fullURL, headers, body).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                LogUtils.printLog("onResponse")
                handleOnResponse(response, listener)
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                LogUtils.printLog("handleOnFailure")
                handleOnFailure(t, listener)
            }

        })
    }


    fun postXWWWFormData(
        endPoint: String,
        headers: Map<String, String>,
        myParams: Map<String, String>,
        listener: RequestListener
    ) {
        val builder = FormBody.Builder()
        for (k in myParams.keys) {
            if (myParams[k] != null) builder.addEncoded(k, myParams[k]!!)
        }
        val api = retrofit.create(API::class.java)
        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            baseURL + endPoint
        }
        LogUtils.printLog("post url:", fullURL)
        LogUtils.printLog("headers:", headers)
        // createFormData("image", image.name, requestFile)
        api.post(fullURL, headers, builder.build()).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                LogUtils.printLog("onResponse ${response.code()} ${response.body()}")
                handleOnResponse(response, listener)
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                LogUtils.printLog("handleOnFailure")
                handleOnFailure(t, listener)
            }

        })


    }

    fun postFormData(
        endPoint: String,
        headers: Map<String, String>,
        myParams: Map<String, String>,
        requestFile: RequestBody?,
        requestFileParams: String?,
        listener: RequestListener
    ) {
        val api = retrofit.create(API::class.java)
        val fullURL = if (endPoint.startsWith("http", true)) {
            endPoint
        } else {
            baseURL + endPoint
        }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
        myParams.forEach { (s, v) -> requestBody.addFormDataPart(s, v) }
        if (requestFile != null) requestBody.addFormDataPart(requestFileParams ?: "image", "${requestFileParams ?: "image"}.png", requestFile)
        // createFormData("image", image.name, requestFile)
        LogUtils.printLog("post url:", fullURL)
        LogUtils.printLog("headers:", headers)
        api.post(fullURL, headers, requestBody.build()).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                LogUtils.printLog("onResponse ${response.code()} ${response.body()}")
                handleOnResponse(response, listener)
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                LogUtils.printLog("handleOnFailure")
                handleOnFailure(t, listener)
            }

        })
    }

    private fun handleOnResponse(response: Response<JSONObject>, listener: RequestListener?) {
        if (listener != null) {
            LogUtils.printLog("response:", response.code(), response.body().toString())
            if (response.code() == 200) {
                try {
                    val errorJson = (response.body() as JSONObject).get("error")
                    val dataJson = (response.body() as JSONObject).get("data")
                    val errorCode = (errorJson as JSONObject).getInt("code")
                    val errorMessage = (errorJson as JSONObject).getString("message")
                    LogUtils.printLog("handleOnResponse request success ${response.body()} - $errorJson")
                    if (errorCode == 401 || errorCode == 403) {
                        listener.fail("Wrong Token")
                    } else {
                        if (errorCode == 0)
                            listener.success(dataJson as JSONObject)
                        else
                            listener.fail(errorMessage)
                    }
                } catch (exception: Exception) {
                    listener.success(response.body() as JSONObject)
                }
            } else if (response.code() == 401) {
                LogUtils.printLog("Session is expired ...")
                listener.timeout()
            } else if (response.code() == 404) {
                listener.fail("notfound")
            } else {
                listener.fail("NetworkError")
            }
        }
    }

    private fun handleOnFailure(t: Throwable, listener: RequestListener?) {
        t.printStackTrace()

        if (listener != null) {
            val message = t.message as String
            listener.fail(message)
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
            baseURL + endPoint
        }

        LogUtils.printLog("post url:", fullURL)
        LogUtils.printLog("headers:", headers)
        LogUtils.printLog("params:", params.toString())
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
        fun fail(error: String)
        fun timeout()
    }

}