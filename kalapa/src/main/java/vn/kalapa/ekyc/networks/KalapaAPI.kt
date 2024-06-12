package vn.kalapa.ekyc.networks

import android.graphics.Bitmap
import android.os.Build
import org.json.JSONObject
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.managers.AESCryptor
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.CreateSessionResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.NFCRawData
import vn.kalapa.ekyc.utils.Helpers

class KalapaAPI {

    companion object {
        var client = Client(KalapaSDK.config.baseURL) //Client(KalapaSDK.config.baseURL)
        val NFC_PATH = "/api/nfc/get-token"
        val VERSION_PATH = "/api/kyc/get-version"


        fun nfcCheck(
            endPoint: String,
            body: NFCRawData,
            listener: Client.RequestListener
        ) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            val encryptedBody = mapOf("data" to AESCryptor.encryptText(body.toJson()))
            client.post(endPoint, header, encryptedBody, listener)
        }

        fun configurationDocumentAndFaceSetting(
            endPoint: String,
            token: String,
            acceptedDocuments: Array<String>,
            faceMatchingThreshold: Int,
            onSuccess: (result: JSONObject) -> Unit,
            onFail: (error: KalapaError?) -> Unit
        ) {
            val url = "$endPoint/api/configuration"
            val client = Client(url)
            val header = mapOf(
                "Authorization" to token,
                "Content-Type" to "application/json",
                "mobile_device" to Build.MODEL
            )
            val body = mapOf(
                "accepted_documents" to acceptedDocuments,
                "face_matching_thr" to faceMatchingThreshold.toString()
            )
            Helpers.printLog("configurationDocumentAndFaceSetting $url $token $acceptedDocuments $faceMatchingThreshold")
            client.post(url, header, body, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    Helpers.printLog("getSession success")
                    onSuccess(jsonObject)
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog("configurationDocumentAndFaceSetting error $error")
                    onFail(error)
                }

                override fun timeout() {
                    onFail(KalapaError(403, "Token không đúng, vui lòng thử lại"))
                }
            })
        }

        fun doRequestGetSession(
            endPoint: String,
            token: String,
            captureImage: Boolean,
            useNFC: Boolean,
            verifyCheck: String,
            fraudCheck: String,
            normalCheckOnly: String,
            cardSidesCheck: Boolean,
            acceptedDocuments: Array<String>,
            faceMatchingThreshold: Int,
            onSuccess: (sessionResult: CreateSessionResult) -> Unit,
            onFail: (error: KalapaError?) -> Unit
        ): String? {
            client = Client(endPoint)
            val url = "$endPoint/api/auth/get-token"
            val client = Client(url)
            val header = mapOf(
                "Authorization" to token,
                "Content-Type" to "application/json",
                "mobile_device" to Build.MODEL
            )
            val body = mapOf(
                "app_token" to token,
                "allow_sdk_full_results" to "true",
                "verify_check" to verifyCheck,
                "fraud_check" to fraudCheck,
                "strict_quality_check" to if (normalCheckOnly == "true") "false" else "true",
                "scan_full_information" to cardSidesCheck.toString(),
                "mobile_device" to Build.MODEL,
                "accepted_documents" to acceptedDocuments,
                "face_matching_thr" to faceMatchingThreshold.toString(),
                "flow" to if (captureImage && useNFC) "nfc_ekyc" else if (captureImage) "ekyc" else "nfc_only"
            )
            Helpers.printLog("Get Session $url")

            client.post(url, header, body, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    Helpers.printLog("getSession success")
                    var sessionResult: CreateSessionResult = CreateSessionResult.fromJson(jsonObject.toString())!!
                    Helpers.printLog(sessionResult.token)
                    onSuccess(sessionResult)
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog("Call getSession error $error")
                    onFail(error)
                }

                override fun timeout() {
                    onFail(KalapaError(403, "Token không đúng, vui lòng thử lại"))
                }
            })
            return ""
        }

        fun doRequestGetSession(
            endPoint: String,
            token: String,
            onSuccess: (sessionResult: CreateSessionResult) -> Unit,
            onFail: (error: KalapaError?) -> Unit
        ): String? {
            client = Client(endPoint)
            val url = "$endPoint/api/auth/get-token"
            val client = Client(url)
            val header = mapOf(
                "Authorization" to token,
                "Content-Type" to "application/json",
                "mobile_device" to Build.MODEL
            )
            val captureImage = false
            val useNFC = true

            return doRequestGetSession(endPoint,token,captureImage,useNFC,"true","true","true",true, arrayOf(""),50,onSuccess,onFail)
        }
        fun confirm(
            endPoint: String,
            name: String,
            id_number: String,
            gender: String,
            birthday: String,
            resident: String,
            doi: String,
            poi: String,
            home: String,
            doe: String,
            listener: Client.ConfirmListener
        ) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            val body = mapOf<String, String>(
                "birthday" to birthday,
                "doi" to doi,
                "gender" to gender,
                "id_number" to id_number,
                "name" to name,
                "poi" to poi,
                "resident" to resident
            )
            client.post(
                "$endPoint?get_full_result=true",
                header,
                body,
                object : Client.RequestListener {
                    override fun success(jsonObject: JSONObject) {
                        Helpers.printLog("ConfirmResult success IDCard $jsonObject")
                        Helpers.printLog(jsonObject.toString())
                        var confirmResult: ConfirmResult =
                            ConfirmResult.fromJson(jsonObject.toString())!!
                        listener.success(confirmResult)
                    }

                    override fun timeout() {
                        listener.timeout()
                    }

                    override fun fail(error: KalapaError) {
                        Helpers.printLog("Call confirm error $error")
                        Helpers.printLog(error)
                        listener.fail(error)
                    }
                })
        }

        fun confirmPassport(
            endPoint: String,
            name: String,
            id_number: String,
            gender: String,
            birthday: String,
            pob: String,
            doe: String,
            poi: String,
            listener: Client.ConfirmListener
        ) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            val body = mapOf<String, String>(
                "dob" to birthday,
                "doe" to doe,
                "gender" to gender,
                "id_number" to id_number,
                "name" to name,
                "poi" to poi,
                "pob" to pob
            )
            client.post(endPoint, header, body, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    Helpers.printLog("ConfirmResult success PASSPORT")
                    Helpers.printLog(jsonObject.toString())
                    var confirmResult: ConfirmResult =
                        ConfirmResult.fromJson(jsonObject.toString())!!
                    listener.success(confirmResult)
                }

                override fun timeout() {
                    listener.timeout()
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog(error.message)
                    Helpers.printLog(error.code)
                    Helpers.printLog("ConfirmResult error")
                    listener.fail(error)
                }
            })
        }


        fun confirm(endPoint: String, listener: Client.ConfirmListener) {
            // Use when wanna ignore confirm step
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            val body = mapOf<String, String>(
            )
            client.post(endPoint, header, body, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    Helpers.printLog("ConfirmResult success from liveness")
                    Helpers.printLog(jsonObject.toString())
                    var confirmResult: ConfirmResult =
                        ConfirmResult.fromJson(jsonObject.toString())!!
                    listener.success(confirmResult)
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog("Call confirm error $error")
                    listener.fail(error)
                }

                override fun timeout() {
                    listener.timeout()
                }
            })
        }


        fun imageCheck(endPoint: String, image: Bitmap, listener: Client.RequestListener) {
            val header = mapOf(
                "Authorization" to KalapaSDK.session
            )

            client.postFormData(endPoint, header, image, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    Helpers.printLog("response:", jsonObject)
                    val Eobject = jsonObject["error"] as JSONObject
                    val code = Eobject["code"] as Int
                    Helpers.printLog("code:", code)
                    if (code == 0) {
                        listener.success(jsonObject["data"] as JSONObject)
                    } else {
                        if (arrayOf(
                                1,
                                2,
                                3,
                                4,
                                5,
                                6,
                                7,
                                8,
                                9,
                                10,
                                11,
                                12,
                                13,
                                14,
                                15,
                                16,
                                19,
                                20,
                                21,
                                23,
                                27,
                                32,
                                40,
                                42,
                                41,
                                43,
                                61,
                                62,
                                63,
                                64,
                                65,
                                80,
                                400,
                                500
                            ).contains(code)
                        ) {
                            listener.fail(KalapaError(code))
                        } else {
                            Helpers.printLog(
                                "code:",
                                KalapaError(code, Eobject["message"] as String)
                            )
//                            listener.fail(KalapaError(code, Eobject["message"] as String))
                            listener.fail(KalapaError.UnknownError)
                        }
                    }
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog("Call postFormData error $error")
                    listener.fail(error)
                }

                override fun timeout() {
                    Helpers.printLog("Call postFormData timeout")
                    listener.timeout()
                }
            })
        }

        fun selfieCheck(endPoint: String, image: Bitmap, listener: Client.RequestListener) {
            val header = mapOf(
                "Authorization" to KalapaSDK.session
            )
            client.postFormData(endPoint, header, image, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    Helpers.printLog("response:", jsonObject)
                    val Eobject = jsonObject["error"] as JSONObject
                    val code = Eobject["code"] as Int
                    Helpers.printLog("code:", code)
                    if (code == 0) {
                        listener.success(jsonObject["data"] as JSONObject)
                    } else {
                        if (arrayOf(
                                1,
                                2,
                                3,
                                4,
                                5,
                                6,
                                7,
                                8,
                                9,
                                10,
                                11,
                                12,
                                13,
                                14,
                                15,
                                16,
                                19,
                                20,
                                21,
                                23,
                                27,
                                32,
                                40,
                                42,
                                41,
                                43,
                                61,
                                62,
                                63,
                                64,
                                65,
                                80,
                                400,
                                500
                            ).contains(code)
                        ) {
                            listener.fail(KalapaError(code))
                        } else {
                            Helpers.printLog(
                                "code:",
                                KalapaError(code, Eobject["message"] as String)
                            )
//                            listener.fail(KalapaError(code, Eobject["message"] as String))
                            listener.fail(KalapaError.UnknownError)
                        }
                    }
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog("Call selfieCheck error $error")
                    listener.fail(error)
                }

                override fun timeout() {
                    Helpers.printLog("Call selfieCheck timeout")
                    listener.timeout()
                }

            })
        }

        fun passportCheck(endPoint: String, image: String, listener: Client.RequestListener) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            Helpers.printLog("Call passportCheck")
            val body = mapOf<String, String>(
                "image" to image
            )
            client.post(endPoint, header, body, handleDataResultListener(listener))
        }

        fun getData(endPoint: String, listener: Client.RequestListener) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            Helpers.printLog("Call getData")

            client.get(endPoint, header, handleDataResultListener(listener))
        }

        fun getNFCLocation(endPoint: String, listener: Client.RequestListener) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            Helpers.printLog("Call getNFCLocation")

            client.get(endPoint + "?device=${Helpers.getDeviceModel()}", header, handleDataResultListener(listener))
        }

        fun backCheck(endPoint: String, image: String, listener: Client.RequestListener) {
            val header = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to KalapaSDK.session
            )
            Helpers.printLog("Call backCheck")
            val body = mapOf<String, String>(
                "image" to "${image}"
            )
            client.post(endPoint, header, body, handleDataResultListener(listener))
        }

        private fun handleDataResultListener(listener: Client.RequestListener): Client.RequestListener {
            return object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    try {
                        val error = jsonObject.getJSONObject("error")
                        val code = error.getInt("code")

                        if (code == 0) {
                            val data = jsonObject.getJSONObject("data")
                            listener.success(data)
                        } else {
                            Helpers.printLog("err2")
//                            listener.fail(KalapaError(code, error.getString("message")))
                            listener.fail(KalapaError(code))
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        listener.fail(KalapaError(-3, ex.message as String))
                    }
                }

                override fun fail(error: KalapaError) {
                    Helpers.printLog("err1")
                    listener.fail(error)
                }

                override fun timeout() {
                    listener.timeout()
                }
            }
        }

    }


}