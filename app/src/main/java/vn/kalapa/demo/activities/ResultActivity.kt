package vn.kalapa.demo.activities

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import com.google.gson.JsonObject
import vn.kalapa.demo.ExampleGlobalClass
import vn.kalapa.demo.R
import vn.kalapa.demo.models.NFCVerificationData
import vn.kalapa.demo.utils.Common.MY_KEY_BTN_TEXT_COLOR
import vn.kalapa.demo.utils.Common.MY_KEY_MAIN_COLOR
import vn.kalapa.demo.utils.Helpers.Companion.getValuePreferences
import vn.kalapa.demo.utils.LogUtils
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.views.KLPDecisionRow
import vn.kalapa.ekyc.views.KLPResultRow

class ResultActivity : AppCompatActivity() {
    val TAG = "ResultActivity"
    lateinit var btnFinish: Button
    lateinit var ivSelfie: ImageView
    lateinit var ivFront: ImageView
    lateinit var ivBack: ImageView
    lateinit var rootContainer: View
    var language: String = "vi"
    lateinit var tvResult: TextView
    lateinit var tvOcrTitle: TextView
    lateinit var tvNFCTitle: TextView
    lateinit var tvRuleTitle: TextView
    lateinit var rowId: KLPResultRow
    lateinit var rowName: KLPResultRow
    lateinit var rowDob: KLPResultRow
    lateinit var rowHometown: KLPResultRow
    lateinit var rowAddress: KLPResultRow
    lateinit var rowDoi: KLPResultRow
    lateinit var rowPoi: KLPResultRow
    lateinit var rowPersonalIdentification: KLPResultRow
    lateinit var rowCardType: KLPResultRow
    lateinit var containerViolatedRule: LinearLayout
    lateinit var containerNFCData: LinearLayout
    lateinit var containerOCRInfo: LinearLayout
    private lateinit var container_eid_photo: CardView
    private lateinit var container_selfie_photo: CardView
    private lateinit var iv_eid_photo: ImageView
    private lateinit var row_nfc_id: KLPResultRow
    private lateinit var row_nfc_old_id: KLPResultRow
    private lateinit var row_nfc_name: KLPResultRow
    private lateinit var row_nfc_dob: KLPResultRow
    private lateinit var row_nfc_gender: KLPResultRow
    private lateinit var row_nfc_nationality: KLPResultRow
    private lateinit var row_nfc_hometown: KLPResultRow
    private lateinit var row_nfc_address: KLPResultRow
    private lateinit var row_nfc_doi: KLPResultRow
    private lateinit var row_nfc_personal_identification: KLPResultRow
    private lateinit var row_nfc_mother_name: KLPResultRow
    private lateinit var row_nfc_father_name: KLPResultRow
    private lateinit var row_nfc_spouse_name: KLPResultRow
    private lateinit var row_nfc_nation: KLPResultRow
    private lateinit var row_nfc_religion: KLPResultRow
    private lateinit var tv_result_banner: TextView
    private lateinit var row_is_matched: KLPResultRow
    private lateinit var row_matching_score: KLPResultRow
    private lateinit var tv_title_face_matching: TextView
    lateinit var containerMatchingHolder: LinearLayout

    private lateinit var nfcDataResult: JsonObject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        setupBinding()
        setNFCValue(if (ExampleGlobalClass.isNFCDataInitialized()) ExampleGlobalClass.nfcData else null)
        setValue()
        refreshColor()
    }

    @SuppressLint("ResourceType")
    fun refreshColor() {
        var mainColor =
            getValuePreferences(MY_KEY_MAIN_COLOR) ?: getString(R.color.mainColor)
        var txtColor = getValuePreferences(MY_KEY_BTN_TEXT_COLOR) ?: getString(R.color.white)
        tvNFCTitle.setTextColor(Color.parseColor(mainColor))

        tvOcrTitle.setTextColor(Color.parseColor(mainColor))
        tvRuleTitle.setTextColor(Color.parseColor(mainColor))

        btnFinish.setTextColor(Color.parseColor(txtColor))
        ViewCompat.setBackgroundTintList(
            btnFinish,
            ColorStateList.valueOf(Color.parseColor(mainColor))
        )

        tv_title_face_matching = findViewById(R.id.tv_title_face_matching)
        tv_title_face_matching.setTextColor(Color.parseColor(mainColor))
        tv_title_face_matching.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_face_matching_title))
//        ViewCompat.setBackgroundTintList(
//            containerNFCInfo,
//            ColorStateList.valueOf(Color.parseColor(mainColor))
//        )
//        ViewCompat.setBackgroundTintList(
//            containerOCRInfo,
//            ColorStateList.valueOf(Color.parseColor(mainColor))
//        )
//        ViewCompat.setBackgroundTintList(
//            containerViolatedRule,
//            ColorStateList.valueOf(Color.parseColor(mainColor))
//        )

    }

    fun setupBinding() {
        tv_result_banner = findViewById(R.id.tv_result_banner)
        btnFinish = findViewById(R.id.btn_finish)
        ivSelfie = findViewById(R.id.iv_selfie)
        ivFront = findViewById(R.id.iv_front_id)
        ivBack = findViewById(R.id.iv_back_id)
//        Log.d(TAG, "Selfie Path: " + FileUtil.getFaceFile(this@ResultActivity).absolutePath)
//        Log.d(TAG, "Front Path: " + FileUtil.getCardFile(this@ResultActivity).absolutePath)
//        Log.d(TAG, "Back Path: " + FileUtil.getCardBackFile(this@ResultActivity).absolutePath)

        rootContainer = findViewById(R.id.root_container)
        rootContainer.setBackgroundColor(resources.getColor(R.color.ekyc_demo_color))
        tvResult = findViewById(R.id.tv_result)
        rowId = findViewById(R.id.row_id)
        rowName = findViewById(R.id.row_name)
        rowDob = findViewById(R.id.row_dob)
        rowHometown = findViewById(R.id.row_hometown)
        rowAddress = findViewById(R.id.row_address)
        rowDoi = findViewById(R.id.row_doi)
        rowPoi = findViewById(R.id.row_poi)
        rowPersonalIdentification = findViewById(R.id.row_personal_identification)
        rowCardType = findViewById(R.id.row_card_type)
        containerViolatedRule = findViewById(R.id.holder_not_qualified_rule)
        containerNFCData = findViewById(R.id.holder_nfc_data)
        containerOCRInfo = findViewById(R.id.ll_ocr_holder)
        tvOcrTitle = findViewById(R.id.tv_title_ocr)
        tvNFCTitle = findViewById(R.id.tv_title_nfc)
        tvRuleTitle = findViewById(R.id.tv_title_rule)
        btnFinish?.setOnClickListener {
            qualifiedToUpgrade()
        }
        language = if (btnFinish?.text == "Done") "en" else "vi"
        row_nfc_id = findViewById(R.id.row_nfc_id)
        row_nfc_old_id = findViewById(R.id.row_nfc_old_id)
        row_nfc_name = findViewById(R.id.row_nfc_name)
        row_nfc_dob = findViewById(R.id.row_nfc_dob)
        row_nfc_gender = findViewById(R.id.row_nfc_gender)
        row_nfc_nationality = findViewById(R.id.row_nfc_nationality)
        row_nfc_hometown = findViewById(R.id.row_nfc_hometown)
        row_nfc_address = findViewById(R.id.row_nfc_address)
        row_nfc_doi = findViewById(R.id.row_nfc_doi)
        row_nfc_personal_identification = findViewById(R.id.row_nfc_personal_identification)
        row_nfc_mother_name = findViewById(R.id.row_nfc_mother_name)
        row_nfc_father_name = findViewById(R.id.row_nfc_father_name)
        row_nfc_spouse_name = findViewById(R.id.row_nfc_spouse_name)
        row_nfc_nation = findViewById(R.id.row_nfc_nation)
        row_nfc_religion = findViewById(R.id.row_nfc_religion)
        container_eid_photo = findViewById(R.id.container_eid_photo)
        container_selfie_photo = findViewById(R.id.container_selfie_photo)
        containerMatchingHolder = findViewById(R.id.container_matching_holder)
        iv_eid_photo = findViewById(R.id.iv_eid_photo)
        row_is_matched = findViewById(R.id.row_is_matched)
        row_matching_score = findViewById(R.id.row_matching_score)
    }

    private fun setNFCValue(nfcVerificationData: NFCVerificationData?) {
        val nfcResult = nfcVerificationData?.nfc_data?.card_data
        LogUtils.printLog("NFC Result: $nfcResult")
        if (nfcResult != null && !nfcResult.id_number.isNullOrEmpty()) {
            containerNFCData.visibility = View.VISIBLE
            if (nfcResult.id_number != null && nfcResult.id_number!!.isNotEmpty())
                row_nfc_id.setRecordValue(nfcResult.id_number) else row_nfc_id.visibility = View.GONE
            if (nfcResult.old_id_number != null && nfcResult.old_id_number!!.isNotEmpty()) row_nfc_old_id.setRecordValue(
                nfcResult.old_id_number
            ) else row_nfc_old_id.setRecordValue("-")
            if (nfcResult.name != null && nfcResult.name!!.isNotEmpty()) row_nfc_name.setRecordValue(
                nfcResult.name
            ) else row_nfc_name.visibility = View.GONE
            if (nfcResult.date_of_birth != null && nfcResult.date_of_birth!!.isNotEmpty()) row_nfc_dob.setRecordValue(
                nfcResult.date_of_birth
            ) else row_nfc_dob.visibility = View.GONE
            if (nfcResult.gender != null && nfcResult.gender!!.isNotEmpty()) row_nfc_gender.setRecordValue(
                nfcResult.gender
            ) else row_nfc_gender.visibility = View.GONE
            if (nfcResult.nationality != null && nfcResult.nationality!!.isNotEmpty()) row_nfc_nationality.setRecordValue(
                nfcResult.nationality
            ) else row_nfc_nationality.visibility = View.GONE
            if (nfcResult.hometown != null && nfcResult.hometown!!.isNotEmpty()) row_nfc_hometown.setRecordValue(
                nfcResult.hometown
            ) else row_nfc_hometown.visibility = View.GONE
            if (nfcResult.address != null && nfcResult.address!!.isNotEmpty()) row_nfc_address.setRecordValue(
                nfcResult.address
            ) else row_nfc_address.visibility = View.GONE
            if (nfcResult.date_of_issuance != null && nfcResult.date_of_issuance!!.isNotEmpty()) row_nfc_doi.setRecordValue(
                nfcResult.date_of_issuance
            ) else row_nfc_doi.visibility = View.GONE
            if (nfcResult.personal_identification != null && nfcResult.personal_identification!!.isNotEmpty()) row_nfc_personal_identification.setRecordValue(
                nfcResult.personal_identification
            ) else row_nfc_personal_identification.visibility = View.GONE
            if (nfcResult.mother_name != null && nfcResult.mother_name!!.isNotEmpty()) row_nfc_mother_name.setRecordValue(
                nfcResult.mother_name
            ) else row_nfc_mother_name.visibility = View.GONE
            if (nfcResult.father_name != null && nfcResult.father_name!!.isNotEmpty()) {
                row_nfc_father_name.setRecordValue(nfcResult.father_name)
                if (nfcResult.father_name.isNullOrEmpty()) row_nfc_father_name.hideLastRow()
            } else row_nfc_father_name.visibility = View.GONE
            if (nfcResult.spouse_name != null && nfcResult.spouse_name!!.isNotEmpty()) {
                row_nfc_spouse_name.setRecordValue(nfcResult.spouse_name)
                row_nfc_spouse_name.hideLastRow()
            } else row_nfc_spouse_name.visibility = View.GONE
            if (nfcResult.religion != null && nfcResult.religion!!.isNotEmpty()) row_nfc_religion.setRecordValue(
                nfcResult.religion
            ) else row_nfc_religion.visibility = View.GONE
            if (nfcResult.nation != null && nfcResult.nation!!.isNotEmpty()) row_nfc_nation.setRecordValue(
                nfcResult.nation
            ) else row_nfc_nation.visibility = View.GONE
            if (nfcResult.face_image != null && nfcResult.face_image!!.isNotEmpty())
                try {
                    container_eid_photo.visibility = View.VISIBLE
                    iv_eid_photo.setImageBitmap(BitmapUtil.base64ToBitmap(nfcResult.face_image!!))
                } catch (e: Exception) {
                    container_eid_photo.visibility = View.GONE
                    e.printStackTrace()
                }
        } else
            containerNFCData.visibility = View.GONE
        // is_valid & matching_score
        if (nfcVerificationData?.is_match != null && nfcVerificationData.matching_score != null) {
            containerMatchingHolder.visibility = View.VISIBLE
            // Set selfie
            LogUtils.printLog("Selfie: ${nfcVerificationData.matching_score} ")
            row_is_matched.visibility = View.VISIBLE
            row_matching_score.visibility = View.VISIBLE
            row_is_matched.setRecordValue(
                if (nfcVerificationData.is_match!!) KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes)) else KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))
            )
            row_matching_score.setRecordValue(nfcVerificationData.matching_score.toString())
        } else containerMatchingHolder.visibility = View.GONE
    }

    fun setValue() {
        tv_result_banner.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_title_1))
        tvNFCTitle.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_nfc_title))
        tvRuleTitle.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_details))
        btnFinish.setText(KLPLanguageManager.get(resources.getString(R.string.klp_button_confirm)))
        tvOcrTitle.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_ocr_title))
        if (ExampleGlobalClass.faceImage != null) {
            ivSelfie.visibility = View.VISIBLE
            container_selfie_photo.visibility = View.VISIBLE
            ivSelfie.setImageBitmap(ExampleGlobalClass.faceImage)
        } else
            container_selfie_photo.visibility = View.GONE

        if (ExampleGlobalClass.frontImage != null) {
            ivFront.visibility = View.VISIBLE
            ivFront.setImageBitmap(ExampleGlobalClass.frontImage)
            findViewById<View>(R.id.cv_front_id).visibility = View.VISIBLE
        } else
            ivFront.visibility = View.GONE

        if (ExampleGlobalClass.backImage != null) {
            ivBack.visibility = View.VISIBLE
            ivBack.setImageBitmap(ExampleGlobalClass.backImage)
            findViewById<View>(R.id.cv_back_id).visibility = View.VISIBLE
        } else
            ivBack.visibility = View.GONE

        if (ExampleGlobalClass.isKalapaResultInitialized()) {
            val myFields = ExampleGlobalClass.kalapaResult
            containerOCRInfo.visibility = if (myFields.idNumber.isNullOrEmpty()) View.GONE else View.VISIBLE
            if (myFields.selfie_data != null && myFields.selfie_data!!.is_matched != null && myFields.selfie_data!!.matching_score != null) {
                // Set selfie
                containerMatchingHolder.visibility = View.VISIBLE
                row_is_matched.visibility = View.VISIBLE
                row_matching_score.visibility = View.VISIBLE
                row_is_matched.setRecordValue(
                    if (myFields.selfie_data!!.is_matched!!) KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes)) else KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))
                )
                row_matching_score.setRecordValue(myFields.selfie_data!!.matching_score.toString())
            } else containerMatchingHolder.visibility = View.GONE


            if (myFields.idNumber != null) rowId.setRecordValue(myFields.idNumber) else rowId.visibility =
                View.GONE
            if (myFields.name != null) rowName.setRecordValue(myFields.name) else rowName.visibility =
                View.GONE
            if (myFields.birthday != null) rowDob.setRecordValue(myFields.birthday) else rowDob.visibility =
                View.GONE
            if (myFields.home != null) rowHometown.setRecordValue(myFields.home) else rowHometown.visibility =
                View.GONE
            if (myFields.resident != null) rowAddress.setRecordValue(myFields.resident) else rowAddress.visibility =
                View.GONE
            if (myFields.doi != null) rowDoi.setRecordValue(myFields.doi) else rowDoi.visibility =
                View.GONE
            if (myFields.poi != null) rowPoi.setRecordValue(myFields.poi) else rowPoi.visibility =
                View.GONE
            if (myFields.features != null) rowPersonalIdentification.setRecordValue(myFields.features) else rowPersonalIdentification.visibility =
                View.GONE
            if (myFields.type != null) {
                rowCardType.setRecordValue(myFields.type)
                rowCardType.hideLastRow()
            } else rowCardType.visibility = View.GONE

            if (myFields.decision != null && myFields.decision_detail != null) {
                findViewById<View>(R.id.container_decision).visibility = View.VISIBLE
                tvResult.visibility = View.VISIBLE

                tvResult.text = when (myFields.decision) {
                    "APPROVED" -> KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_approved))
                    "MANUAL" -> KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_manual))
                    "REJECTED" -> KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_rejected))
                    else -> KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_manual))
                }

                if (myFields.decision == "APPROVED") {
                    containerViolatedRule.visibility = View.GONE
                    tvResult.setTextColor(resources.getColor(R.color.ekyc_green))
                } else {
                    tvResult.setTextColor(resources.getColor(if (myFields.decision == "REJECTED") R.color.ekyc_red else R.color.ekyc_warning))
                    containerViolatedRule.visibility = View.VISIBLE
                    var decisionDetails = myFields.decision_detail
                    var count = 0
                    decisionDetails!!.forEach {
                        if (it.is_pass != null && (it.is_pass == 0)) { // || it.isPass == -1
                            // Add into Linear Layout
                            var row = KLPDecisionRow(this@ResultActivity)
                            row.setRuleValue(if (language == "en") it.description else it.description_vi)
                            containerViolatedRule.addView(row)
                            count++
                        }
                    }
                    if (count == 0) containerViolatedRule.visibility = View.GONE
                }
            }
        } else
            containerOCRInfo.visibility = View.GONE


    }

    private fun qualifiedToUpgrade() {
//        if (Kalapa.flowType == KalapaFlowType.EKYC && Kalapa.result != null && Kalapa.result!!.decision == "APPROVED") {
//            Helpers.showDialog(this@ResultActivity, KLPLanguageManager.get(resources.getString(R.string.klp_upgrade_your_account), KLPLanguageManager.get(resources.getString(R.string.klp_do_you_want_to_verify_nfc_to_upgrade),
//                KLPLanguageManager.get(resources.getString(R.string.klp_btn_upgrade), KLPLanguageManager.get(resources.getString(R.string.klp_btn_skip), R.drawable.ic_nfc, object : DialogListener {
//                    override fun onYes() {
//                        KalapaNFC.configure(KalapaNFCConfig.Builder(this@ResultActivity).withBaseUrl(Kalapa.baseURL).withLanguage(if (Kalapa.language.contains("vi")) Language.VI else Language.EN).withMinNFCRetries(3).build(), null)
////                            .start(this@ResultActivity, Kalapa.session, KalapaFlowType.NFC_ONLY)
//                            .start(this@ResultActivity, Kalapa.session, KalapaFlowType.NFC_ONLY)
//                        finish()
//                    }
//
//                    override fun onNo() {
//                        finish()
//                    }
//
//                })
//
//        } else
        finish()
    }

    fun setDumpValue(decision: String) {
        rowId.setRecordValue("001094018640")
        rowName.setRecordValue("NGUYỄN GIA TÚ")
        rowDob.setRecordValue("18/08/1994")
        rowHometown.setRecordValue("Trịnh Xá, Thành Phố Phủ Lý, Hà Nam")
        rowAddress.setRecordValue("Số 6 Ngõ 92/8 Nguyễn Khánh Toàn Tổ 5 Quan Hoa Cg Hn")
        rowDoi.setRecordValue("10/07/2021")
        rowPoi.setRecordValue("Cục Trưởng Cục Cảnh Sát Quản Lý Hành Chính Về Trật Tự Xã Hội")
        rowPersonalIdentification.setRecordValue("Nốt Ruồi C:1cm Trên Sau Đuôi Mắt Trái")

        if (decision == "APPROVED") {
            containerViolatedRule.visibility = View.GONE
            tvResult.setTextColor(resources.getColor(R.color.ekyc_green))
            tvResult.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_approved))
        } else {
            tvResult.setTextColor(resources.getColor(if (decision == "REJECTED") R.color.ekyc_red else R.color.ekyc_warning))
            tvResult.text =
                if (decision == "REJECTED") KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_rejected)) else KLPLanguageManager.get(resources.getString(R.string.klp_results_decision_manual))
            containerViolatedRule.visibility = View.VISIBLE
            var decisionDetails = arrayOf(
                "Face on id and selfie are matched with high confidence",
                "Front image is not too blurry",
                "User photo on front image is original",
                "ID number and name have high OCR's confidence scores",
                "Ảnh giấy tờ mặt trước không chụp lại từ màn hình khác",
                "Ảnh giấy tờ mặt trước không quá mờ",
                "Ảnh giấy tờ mặt sau không chói sáng",
                "ID number and name have high OCR's confidence scores"
            )
            for (i in decisionDetails) {
                var row = KLPDecisionRow(this@ResultActivity)
                row.setRuleValue(i)
                containerViolatedRule.addView(row)
            }
        }
    }

}
