package vn.kalapa.ekyc.models

import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.R


data class KalapaError(val code: Int, var message: String = "") {
    companion object {
        val NetworkError = KalapaError(
            -1,
            KalapaSDK.config.context.getString(R.string.klp_error_network)
        ) // "Xảy ra lỗi, vui lòng thử lại..."
        val UnknownError = KalapaError(
            -1,
            KalapaSDK.config.context.getString(R.string.klp_error_unknown_short)
        )
    }


    constructor(code: Int) : this(code, "") {
//        Helpers.printLog("$code Get String: ${rootActivity.getString(R.string.klp_error_doc_not_found)}")
        when (code) {
            1, 2 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_not_found) // "Không tìm thấy giấy tờ"
            3, 4 -> message =
                KalapaSDK.config.context.getString(R.string.klp_card_missing_fields) // "Không tìm thấy giấy tờ"
            5 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_face_not_found) // "Không tìm thấy mặt trên giấy tờ"
            6 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_cut_corner) // "Giấy tờ chụp bị mất góc"
            7 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_blurry) //"Giấy tờ chụp bị mờ"
            8 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_over_exposed) // "Giấy tờ chụp bị chói sáng"
            9 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_photocopy) //"Giấy tờ bị chụp từ bản photocopy"
            10 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_code_10)

            11 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_invalid) //"Giấy tờ ko hợp lệ, mặt ko đúng vị trí"
            12 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_id_name_missing) //"Không tìm thấy số id hoặc tên trên giấy tờ"
            13 -> message =
                KalapaSDK.config.context.getString(R.string.klp_card_abnormal_emblem) //"Không tìm thấy số id hoặc tên trên giấy tờ"
            14 -> message =
                KalapaSDK.config.context.getString(R.string.klp_card_missing_fields) //"Giấy tờ bị che thông tin"
            15 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_invalid) //"Ảnh chân dung không hợp lệ"
            16 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_no_face_too_far) // "Ảnh chân dung không có mặt hoặc mặt không đủ gần"
            18 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_eyes_closed) //"Ảnh chân dung tư thế đầu không ngay ngắn"
            19 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_head_not_straight) //"Ảnh chân dung tư thế đầu không ngay ngắn"
            20 -> message = KalapaSDK.config.context.getString(R.string.klp_error_selfie_face_from_other_screen) //"Ảnh chân dung bị chụp từ màn hình thiết bị khác"
            21 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_face_not_matched) //"Mặt trên ảnh chân dung và trên giấy tờ không giống nhau"
            23 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_face_dark_or_not_fully) //"Mặt bị tối, mờ hoặc không đầy đủ"
            32 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_code_32)

            27 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_selfie_face_too_close_to_bounds) //"Mặt quá gần biên"
            40, 42, 41 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_input_invalid) //"Giấy tờ không hợp lệ"
            43 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_doc_mrz_invalid) //"Không đọc được thông tin MRZ"
            61 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_code_61)

            62 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_code_62)

            63 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_code_63)

            64 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_code_64)

            65 -> message = KalapaSDK.config.context.getString(R.string.klp_error_code_65)
            80 -> message = KalapaSDK.config.context.getString(R.string.klp_error_code_80)
            400 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_input_invalid) //"Đầu vào không hợp lệ"
            500 -> message =
                KalapaSDK.config.context.getString(R.string.klp_error_service_temporary_down) //"Lỗi hệ thống, vui lòng thử lại"
        }
    }

    fun getMessageError(): String {
        when (code) {
            -1 -> {
                if (message == "SSL handshake timed out" || message.contains("SSL handshake aborted")
                    || message.contains("Unable to resolve host") || message.contains("Software caused connection abort")
                )
                    return KalapaSDK.config.context.getString(R.string.klp_error_network) //"Kết nối mạng của bạn đang không ổn định, vui lòng thử lại sau"
                if (message == "Wrong Token")
                    return KalapaSDK.config.context.getString(R.string.klp_error_token) //"Token sai hoặc hết hạn, vui lòng liên hệ để được hỗ trợ"
                if (message == "Session Expired")
                    return KalapaSDK.config.context.getString(R.string.klp_timeout_body) //"Phiên làm việc đã hết hạn, vui lòng thực hiện lại"
                if (message == "timeout")
                    return KalapaSDK.config.context.getString(R.string.klp_error_service_temporary_down) //"Hệ thống hiện đang bận, vui lòng thử lại"
            }
        }
        return message
    }
}