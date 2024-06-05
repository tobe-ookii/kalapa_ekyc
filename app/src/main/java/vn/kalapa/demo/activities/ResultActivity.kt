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
import vn.kalapa.demo.utils.Helpers.Companion.getValuePreferences
import vn.kalapa.demo.utils.LogUtils
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.views.KLPDecisionRow
import vn.kalapa.ekyc.views.KLPResultRow

class ResultActivity : AppCompatActivity() {
    val TAG = "ResultActivity"
    var btnFinish: Button? = null
    lateinit var ivSelfie: ImageView
    lateinit var ivFront: ImageView
    lateinit var ivBack: ImageView
    lateinit var rootContainer: View
    var language: String = "vi"
    lateinit var tvResult: TextView
    lateinit var tvResultStatus: TextView
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
    lateinit var containerNFCInfo: LinearLayout
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

    private lateinit var row_nfc_is_valid: KLPResultRow
    private lateinit var row_is_matched: KLPResultRow
    private lateinit var row_matching_score: KLPResultRow
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
            getValuePreferences(Common.MY_KEY_MAIN_COLOR) ?: getString(R.color.mainColor)
        var txtColor = getValuePreferences(Common.MY_KEY_BTN_TEXT_COLOR) ?: getString(R.color.white)
        tvNFCTitle.setTextColor(Color.parseColor(mainColor))
        tvOcrTitle.setTextColor(Color.parseColor(mainColor))
        tvRuleTitle.setTextColor(Color.parseColor(mainColor))
        if (btnFinish != null) {
            btnFinish!!.setTextColor(Color.parseColor(txtColor))
            ViewCompat.setBackgroundTintList(
                btnFinish!!,
                ColorStateList.valueOf(Color.parseColor(mainColor))
            )
        }
        ViewCompat.setBackgroundTintList(
            containerNFCInfo,
            ColorStateList.valueOf(Color.parseColor(mainColor))
        )
        ViewCompat.setBackgroundTintList(
            containerOCRInfo,
            ColorStateList.valueOf(Color.parseColor(mainColor))
        )
        ViewCompat.setBackgroundTintList(
            containerViolatedRule,
            ColorStateList.valueOf(Color.parseColor(mainColor))
        )

    }

    fun setupBinding() {
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
        tvResultStatus = findViewById(R.id.tv_result_status)
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
        containerNFCInfo = findViewById(R.id.holder_nfc_data)
        containerOCRInfo = findViewById(R.id.ll_ocr_holder)
        tvOcrTitle = findViewById(R.id.tv_title_ocr)
        tvNFCTitle = findViewById(R.id.tv_title_nfc)
        tvRuleTitle = findViewById(R.id.tv_title_rule)
        btnFinish?.setOnClickListener {
            qualifiedToUpgrade()
        }
        language = if (btnFinish?.text == "Done") "en" else "vi"
        if (this::nfcDataResult.isInitialized) {
            containerNFCInfo.visibility = View.VISIBLE
        } else {
            containerNFCInfo.visibility = View.GONE
        }
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
        row_nfc_is_valid = findViewById(R.id.row_nfc_is_valid)
        row_is_matched = findViewById(R.id.row_is_matched)
        row_matching_score = findViewById(R.id.row_matching_score)
    }

    private fun setNFCValue(nfcVerificationData: NFCVerificationData?) {
        val nfcResult = nfcVerificationData?.data?.data
        LogUtils.printLog("NFC Result: $nfcResult")
        if (nfcResult != null) {
            containerNFCInfo.visibility = View.VISIBLE
            if (nfcResult.idCardNo != null && nfcResult.idCardNo!!.isNotEmpty())
                row_nfc_id.setRecordValue(nfcResult.idCardNo) else row_nfc_id.visibility = View.GONE
            if (nfcResult.oldIdCardNo != null && nfcResult.oldIdCardNo!!.isNotEmpty()) row_nfc_old_id.setRecordValue(
                nfcResult.oldIdCardNo
            ) else row_nfc_old_id.setRecordValue(resources.getString(R.string.no_information))
            if (nfcResult.name != null && nfcResult.name!!.isNotEmpty()) row_nfc_name.setRecordValue(
                nfcResult.name
            ) else row_nfc_name.visibility = View.GONE
            if (nfcResult.dateOfBirth != null && nfcResult.dateOfBirth!!.isNotEmpty()) row_nfc_dob.setRecordValue(
                nfcResult.dateOfBirth
            ) else row_nfc_dob.visibility = View.GONE
            if (nfcResult.gender != null && nfcResult.gender!!.isNotEmpty()) row_nfc_gender.setRecordValue(
                nfcResult.gender
            ) else row_nfc_gender.visibility = View.GONE
            if (nfcResult.nationality != null && nfcResult.nationality!!.isNotEmpty()) row_nfc_nationality.setRecordValue(
                nfcResult.nationality
            ) else row_nfc_nationality.visibility = View.GONE
            if (nfcResult.placeOfOrigin != null && nfcResult.placeOfOrigin!!.isNotEmpty()) row_nfc_hometown.setRecordValue(
                nfcResult.placeOfOrigin
            ) else row_nfc_hometown.visibility = View.GONE
            if (nfcResult.residenceAddress != null && nfcResult.residenceAddress!!.isNotEmpty()) row_nfc_address.setRecordValue(
                nfcResult.residenceAddress
            ) else row_nfc_address.visibility = View.GONE
            if (nfcResult.dateOfIssuance != null && nfcResult.dateOfIssuance!!.isNotEmpty()) row_nfc_doi.setRecordValue(
                nfcResult.dateOfIssuance
            ) else row_nfc_doi.visibility = View.GONE
            if (nfcResult.personalSpecificIdentification != null && nfcResult.personalSpecificIdentification!!.isNotEmpty()) row_nfc_personal_identification.setRecordValue(
                nfcResult.personalSpecificIdentification
            ) else row_nfc_personal_identification.visibility = View.GONE
            if (nfcResult.motherName != null && nfcResult.motherName!!.isNotEmpty()) row_nfc_mother_name.setRecordValue(
                nfcResult.motherName
            ) else row_nfc_mother_name.visibility = View.GONE
            if (nfcResult.fatherName != null && nfcResult.fatherName!!.isNotEmpty()) row_nfc_father_name.setRecordValue(
                nfcResult.fatherName
            ) else row_nfc_father_name.visibility = View.GONE
            if (nfcResult.spouseName != null && nfcResult.spouseName!!.isNotEmpty()) row_nfc_spouse_name.setRecordValue(
                nfcResult.spouseName
            ) else row_nfc_spouse_name.visibility = View.GONE
            if (nfcResult.religion != null && nfcResult.religion!!.isNotEmpty()) row_nfc_religion.setRecordValue(
                nfcResult.religion
            ) else row_nfc_religion.visibility = View.GONE
            if (nfcResult.ethnic != null && nfcResult.ethnic!!.isNotEmpty()) row_nfc_nation.setRecordValue(
                nfcResult.ethnic
            ) else row_nfc_nation.visibility = View.GONE
            if (nfcResult.image != null && nfcResult.image!!.isNotEmpty())
                try {
                    container_eid_photo.visibility = View.VISIBLE
                    iv_eid_photo.setImageBitmap(BitmapUtil.base64ToBitmap(nfcResult.image!!))
                } catch (e: Exception) {
                    container_eid_photo.visibility = View.GONE
                    e.printStackTrace()
                }
        } else {
            containerNFCInfo.visibility = View.GONE
        }
        // is_valid & matching_score
        if (nfcVerificationData?.isMatch != null && nfcVerificationData.matchingScore != null) {
            // Set selfie
            LogUtils.printLog("Selfie: ${nfcVerificationData.matchingScore} ")
            row_is_matched.visibility = View.VISIBLE
            row_matching_score.visibility = View.VISIBLE
            row_is_matched.setRecordValue(if (nfcVerificationData.isMatch!!) resources.getString(R.string.klp_demo_yes) else resources.getString(R.string.klp_demo_no))
            row_matching_score.setRecordValue(nfcVerificationData.matchingScore.toString())
        } else containerMatchingHolder.visibility = View.GONE


        if (nfcVerificationData?.data?.isValid != null) {
            // Set Card is valid or not
            LogUtils.printLog("isValid: ${nfcVerificationData.data?.isValid} ")
            row_nfc_is_valid.visibility = View.VISIBLE
            row_nfc_is_valid.setRecordValue(if (nfcVerificationData.data?.isValid!!) resources.getString(R.string.klp_demo_yes) else resources.getString(R.string.klp_demo_no))
        } else row_nfc_is_valid.visibility = View.GONE
    }

    fun setValue() {
        if (ExampleGlobalClass.isFaceImageInitialized()) {
            ivSelfie.visibility = View.VISIBLE
            container_selfie_photo.visibility = View.VISIBLE
            ivSelfie.setImageBitmap(ExampleGlobalClass.faceImage)
        }else
            container_selfie_photo.visibility = View.GONE
//        ivSelfie.setImageURI(Uri.parse(FileUtil.getFaceFile(this@ResultActivity).absolutePath))
//        ivFront.setImageURI(Uri.parse(FileUtil.getCardFile(this@ResultActivity).absolutePath))
//        ivBack.setImageURI(Uri.parse(FileUtil.getCardBackFile(this@ResultActivity).absolutePath))

//        findViewById<View>(R.id.cv_front_id).visibility = if (Kalapa.frontResult == null) View.GONE else View.VISIBLE
//        findViewById<View>(R.id.cv_back_id).visibility = if (Kalapa.backResult == null) View.GONE else View.VISIBLE
//        if (Kalapa.frontResult == null && Kalapa.backResult == null) {
//            containerOCRInfo.visibility = View.GONE
//        }


//        if (Kalapa.frontResult?.myFields != null) {
//            val myFields = Kalapa.frontResult?.myFields!!
//            if (myFields.idNumber != null) rowId.setRecordValue(myFields.idNumber) else rowId.visibility = View.GONE
//            if (myFields.name != null) rowName.setRecordValue(myFields.name) else rowName.visibility = View.GONE
//            if (myFields.birthday != null || myFields.dob != null) rowDob.setRecordValue(if (myFields.birthday != null) myFields.birthday else myFields.dob) else rowDob.visibility = View.GONE
//            if (myFields.home != null) rowHometown.setRecordValue(myFields.home) else rowHometown.visibility = View.GONE
//            if (myFields.resident != null) rowAddress.setRecordValue(myFields.resident) else rowAddress.visibility = View.GONE
//            if (myFields.doi != null) rowDoi.setRecordValue(myFields.doi) else rowDoi.visibility = View.GONE
//            if (myFields.poi != null) rowPoi.setRecordValue(myFields.poi) else rowPoi.visibility = View.GONE
//            if (myFields.features != null) rowPersonalIdentification.setRecordValue(myFields.features) else rowPersonalIdentification.visibility = View.GONE
//            if (myFields.type != null) rowCardType.setRecordValue(myFields.type) else rowCardType.visibility = View.GONE
//        }

//        if (Kalapa.confirmResult?.decision_detail != null) {
//            if (Kalapa.confirmResult?.decision_detail!!.decision != null) {
//                if (Kalapa.confirmResult?.decision_detail!!.decision == "APPROVED") {
//                    containerViolatedRule.visibility = View.GONE
//                    tvResult.setTextColor(resources.getColor(R.color.ekyc_green))
//                    tvResult.text = resources.getString(R.string.klp_demo_approved)
//                } else {
//                    tvResult.setTextColor(resources.getColor(if (Kalapa.confirmResult?.decision_detail!!.decision == "REJECTED") R.color.ekyc_red else R.color.ekyc_warning))
//                    tvResult.text = if (Kalapa.confirmResult?.decision_detail!!.decision == "REJECTED") resources.getString(R.string.klp_demo_rejected) else resources.getString(R.string.klp_demo_manual)
//                    tvResultStatus.text = if (Kalapa.confirmResult?.decision_detail!!.decision == "REJECTED") resources.getString(R.string.klp_your_application_were) else resources.getString(R.string.klp_your_application_is)
//                    containerViolatedRule.visibility = View.VISIBLE
//                    var decisionDetails = Kalapa.confirmResult?.decision_detail!!.details!!
//                    var count = 0
//                    decisionDetails.forEach {
//                        if (it.isPass != null && (it.isPass == 0)) { // || it.isPass == -1
//                            // Add into Linear Layout
//                            var row = KLPDecisionRow(this@ResultActivity)
//                            row.setRuleValue(if (language == "en") it.description else it.descriptionVi)
//                            containerViolatedRule.addView(row)
//                            count++
//                        }
//                    }
//                    if (count == 0) containerViolatedRule.visibility = View.GONE
//                }
//            }
//
//        }
    }

    private fun qualifiedToUpgrade() {
//        if (Kalapa.flowType == KalapaFlowType.EKYC && Kalapa.result != null && Kalapa.result!!.decision == "APPROVED") {
//            Helpers.showDialog(this@ResultActivity, resources.getString(R.string.klp_upgrade_your_account), resources.getString(R.string.klp_do_you_want_to_verify_nfc_to_upgrade),
//                resources.getString(R.string.klp_btn_upgrade), resources.getString(R.string.klp_btn_skip), R.drawable.ic_nfc, object : DialogListener {
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
//
//    override fun finish() {
//        if (Kalapa.result != null) {
//            if (Kalapa.kalapaListener != null) {
//                Kalapa.kalapaListener!!.completion(Kalapa.result!!)
//            }
//            Kalapa.klpHandler.onPreComplete()
//            Kalapa.klpHandler.onComplete(Kalapa.result!!)
//        }
//        super.finish()
//    }

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
            tvResult.text = resources.getString(R.string.klp_demo_approved)
        } else {
            tvResult.setTextColor(resources.getColor(if (decision == "REJECTED") R.color.ekyc_red else R.color.ekyc_warning))
            tvResult.text =
                if (decision == "REJECTED") resources.getString(R.string.klp_demo_rejected) else resources.getString(
                    R.string.klp_demo_manual
                )
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
