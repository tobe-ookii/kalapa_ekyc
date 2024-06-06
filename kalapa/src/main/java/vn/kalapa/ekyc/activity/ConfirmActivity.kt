package vn.kalapa.ekyc.activity

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.icu.util.Calendar
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView.INVISIBLE
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import org.json.JSONObject
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.KalapaSDK.Companion.frontResult
import vn.kalapa.ekyc.KalapaSDK.Companion.kalapaResult
import vn.kalapa.ekyc.KalapaSDKResultCode
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.FrontResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.networks.Client
import vn.kalapa.ekyc.networks.KalapaAPI
import vn.kalapa.ekyc.nfcsdk.models.NFCResultData
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView
import java.util.*


class ConfirmActivity : BaseActivity(), View.OnClickListener, Client.RequestListener, DialogListener, OnEditorActionListener {
    private var isFinishedConfirm = false
    private lateinit var edName: EditText
    private lateinit var edDOB: LinearLayout
    private lateinit var edDOE: LinearLayout
    private lateinit var edDOI: LinearLayout
    private lateinit var radioGroupSex: RadioGroup
    private lateinit var radioMale: RadioButton
    private lateinit var radioFemale: RadioButton
    private lateinit var edDOBDay: EditText
    private lateinit var edIDNumber: EditText
    private lateinit var edDOIDay: EditText
    private lateinit var edDOEDay: EditText
    private lateinit var edPlaceOfIssue: EditText
    private lateinit var edAddress: EditText
    private lateinit var btnPersonalConfirm: Button
    private lateinit var tvConfirm0: TextView
    private lateinit var tvConfirm1: TextView
    private lateinit var tvConfirm2: TextView
    private lateinit var tvConfirm3: TextView
    private lateinit var tvConfirm4: TextView
    private lateinit var tvConfirm5: TextView
    private lateinit var tvConfirm6: TextView
    private lateinit var tvConfirm7: TextView
    private lateinit var tvConfirm8: TextView
    private lateinit var tvConfirm9: TextView
    private lateinit var tvConfirm10: TextView
    private lateinit var ivCloseEKYC: ImageView
    private lateinit var edHometown: EditText
    private lateinit var tvTitle: TextView

    //    private lateinit var noteLabel: TextView
    private lateinit var edQrCode: EditText
    private lateinit var containerView: LinearLayout

    private var dob: Date? = null
    private var doi: Date? = null
    private var doe: Date? = null
    private var result = KalapaResult()
    var isLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)
        initView()
        ProgressView.showProgress(this@ConfirmActivity)
        KalapaAPI.getData(
            "/api/kyc/get-result",
            this@ConfirmActivity
        )

    }

    override fun onBackPressed() {
//        super.onBackPressed()
        Helpers.showEndKYC(this, object : DialogListener {
            override fun onYes() {
                finish()
            }

            override fun onNo() {

            }
        })
    }

    private fun initView() {

        containerView = findViewById(R.id.containerView)
//        Helpers.setIndicator(findViewById(R.id.progress_indicator), 4)
        containerView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
        tvTitle = findViewById(R.id.tv_title)
        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvTitle.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_confirm))
        edName = findViewById(R.id.edName)
        edName.setOnEditorActionListener(this)
//        edName.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        edDOB = findViewById(R.id.edDOB)
        edDOI = findViewById(R.id.edDOI)
        edDOI.setOnClickListener(this)
        edDOE = findViewById(R.id.edDOE)
        edDOE.setOnClickListener(this)
        edDOB.setOnClickListener(this)
        radioGroupSex = findViewById(R.id.radioGroupSex)
        radioMale = findViewById(R.id.radio_male)
        radioFemale = findViewById(R.id.radio_female)
        edDOBDay = findViewById(R.id.edDOBDay)
        edIDNumber = findViewById(R.id.edIDNumber)
        edIDNumber.setOnEditorActionListener(this)
        edDOIDay = findViewById(R.id.edDOIDay)
        edDOIDay.setOnClickListener(this)
        edDOEDay = findViewById(R.id.edDOEDay)
        edDOEDay.setOnClickListener(this)
        edPlaceOfIssue = findViewById(R.id.edPlaceOfIssue)
        edPlaceOfIssue.setOnEditorActionListener(this)

        edAddress = findViewById(R.id.edAddress)
        edAddress.setOnEditorActionListener(this)

        edQrCode = findViewById(R.id.edQrCode)
        edQrCode.setOnEditorActionListener(this)

        edHometown = findViewById(R.id.edHometown)
        edHometown.setOnEditorActionListener(this)

//        title2TextView.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
//        tvDataMessage?.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        edQrCode.setTextColor(resources.getColor(R.color.black))
        edName.setTextColor(resources.getColor(R.color.black))

        edAddress.setTextColor(resources.getColor(R.color.black))
        edPlaceOfIssue.setTextColor(resources.getColor(R.color.black))
        edDOIDay.setTextColor(resources.getColor(R.color.black))

        edDOBDay.setTextColor(resources.getColor(R.color.black))
        edDOBDay.setOnClickListener(this)
        edIDNumber.setTextColor(resources.getColor(R.color.black))

        btnPersonalConfirm = findViewById(R.id.btnPersonalConfirm)
        btnPersonalConfirm.setOnClickListener(this)
        tvConfirm0 = findViewById(R.id.tv_confirm_0)
        tvConfirm1 = findViewById(R.id.tv_confirm_1)
        tvConfirm2 = findViewById(R.id.tv_confirm_2)
        tvConfirm3 = findViewById(R.id.tv_confirm_3)
        tvConfirm4 = findViewById(R.id.tv_confirm_4)
        tvConfirm5 = findViewById(R.id.tv_confirm_5)
        tvConfirm6 = findViewById(R.id.tv_confirm_6)
        tvConfirm7 = findViewById(R.id.tv_confirm_7)
        tvConfirm8 = findViewById(R.id.tv_confirm_8)
        tvConfirm9 = findViewById(R.id.tv_confirm_9)
        tvConfirm10 = findViewById(R.id.tv_confirm_10)
        ivCloseEKYC = findViewById(R.id.iv_close_ekyc)
        ivCloseEKYC.setColorFilter(Color.parseColor(KalapaSDK.config.mainTextColor))
        ivCloseEKYC.setOnClickListener {
            Helpers.showEndKYC(this, object : DialogListener {
                override fun onYes() {
                    finish()
                }

                override fun onNo() {

                }
            })
        }
//        noteLabel = findViewById(R.id.noteLabel)

//        var sdk = Build.VERSION.SDK_INT
//        val imageBytes = BitmapUtil.convert(KalapaSDK.config.background)
//        val background = BitmapDrawable(imageBytes)
//
//        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
//            containerView.setBackgroundDrawable(background)
//        } else {
//            containerView.background = background
//        }
        this.btnPersonalConfirm.let {
            ViewCompat.setBackgroundTintList(
                it,
                ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
            )
        }
        Helpers.setRadioButtonTintList(this.radioMale, Color.parseColor(KalapaSDK.config.mainColor))
        Helpers.setRadioButtonTintList(this.radioFemale, Color.parseColor(KalapaSDK.config.mainColor))
    }

    override fun onEmulatorDetected() {
        KalapaSDK.handler.onError(KalapaSDKResultCode.EMULATOR_DETECTED)
    }


    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onPause() {
        super.onPause()
    }

    fun refreshUI() {
        try {
            tvConfirm0.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm2.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm3.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm4.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm5.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm6.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm7.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm8.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm9.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvConfirm10.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
//            noteLabel.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
            radioMale.setTextColor(Color.BLACK)
            radioFemale.setTextColor(Color.BLACK)
//            btnPersonalConfirm.setBackgroundColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            btnPersonalConfirm.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var mLastClickTime: Long = 0
    fun missingField(field: String) {
        Toast.makeText(
            this,
            getString(R.string.klp_error_cannot_leave_empty) + " '${field}'",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun hideKeyboard() {
        Helpers.printLog("Hide Keyboard")
        val view = this.currentFocus
        if (view != null) {
            Helpers.printLog("Begin Hide Keyboard")
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } else
            Helpers.printLog("View is null. Can't Hide Keyboard")

    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onClick(v: View) {
        hideKeyboard()
        Helpers.printLog("OnClick ${SystemClock.elapsedRealtime()} - $mLastClickTime")
        if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        // click info button
        val id: Int = v.id
        if (id == R.id.btnPersonalConfirm) {
            if (edName.text.isEmpty()) {
                missingField(tvConfirm1.text as String)
                return
            } else if (radioGroupSex.checkedRadioButtonId <= 0) {
                missingField(tvConfirm2.text as String)
                return
            } else if (edDOBDay.text.isEmpty()) {
                missingField(tvConfirm3.text as String)
                return
            } else if (edIDNumber.text.isEmpty()) {
                missingField(tvConfirm4.text as String)
                return
            } else if (edDOIDay.text.isEmpty()) {
                missingField(tvConfirm5.text as String)
                return
            } else if (edPlaceOfIssue.text.isEmpty()) {
                missingField(tvConfirm6.text as String)
                return
            } else if (edAddress.text.isEmpty()) {
                missingField(tvConfirm7.text as String)
                return
            }

//            edDOBDay = findViewById(R.id.edDOBDay)
//            edIDNumber = findViewById(R.id.edIDNumber)
//            edDOIDay = findViewById(R.id.edDOIDay)
//            edPlaceOfIssue = findViewById(R.id.edPlaceOfIssue)
//            edAddress = findViewById(R.id.edAddress)
//            edQrCode = findViewById(R.id.edQrCode)


            val sex = radioGroupSex.checkedRadioButtonId
            val gd =
                if (sex == R.id.radio_male) resources.getString(R.string.klp_field_male) else resources.getString(
                    R.string.klp_field_female
                )

            ProgressView.showProgress(this@ConfirmActivity)
            KalapaAPI.confirm(
                "/api/kyc/confirm",
                edName.text.toString().trim(),
                edIDNumber.text.toString().trim(),
                gd,
                edDOBDay.text.toString().trim(),
                edAddress.text.toString().trim(),
                edDOIDay.text.toString().trim(),
                edPlaceOfIssue.text.toString().trim(),
                edHometown.text.toString().trim(),
                edDOEDay.text.toString().trim(),
                object : Client.ConfirmListener {
                    override fun fail(error: KalapaError) {
                        ProgressView.hideProgress()
                        Helpers.showDialog(
                            this@ConfirmActivity,
                            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_error)),
                            error.getMessageError(),
                            R.drawable.sad_face
                        )
                    }

                    override fun timeout() {
                        this@ConfirmActivity.timeout()
                    }


                    override fun success(confirmResult: ConfirmResult) {
                        // KALAPA OUTPUT
                        Helpers.printLog("confirmResult")
//                        Helpers.printLog(confirmResult.toJson())
                        result.birthday = frontResult.myFields?.birthday!!
                        if (frontResult.myFields != null && frontResult.myFields!!.birthday != null) {
                            dob = Common.parseDate(frontResult.myFields?.birthday!!)
                        }
                        result.gender = frontResult.myFields?.gender!!
                        result.home = frontResult.myFields?.home!!
                        result.type = frontResult.myFields?.type!!
                        result.decision = confirmResult.decision_detail?.decision
                        result.resident = frontResult.myFields?.resident!!
                        result.session = confirmResult.session
                        result.idNumber = frontResult.myFields?.idNumber!!
                        result.qr_code = frontResult.qrCode
                        isFinishedConfirm = true
                        result.mrz_data = frontResult.mrzData
                        result.doe = frontResult.myFields?.doe!!
                        result.doi = frontResult.myFields?.doi!!
                        if (frontResult.myFields?.features != null) {
                            result.features = frontResult.myFields?.features!!
                        }
                        if (frontResult.myFields != null && frontResult.myFields!!.doi != null) {
                            doi = Common.parseDate(frontResult.myFields?.doi!!)
                        }
                        if (frontResult.myFields != null && frontResult.myFields!!.doe != null) {
                            doe = Common.parseDate(frontResult.myFields?.doe!!)
                        }
                        result.name = frontResult.myFields?.name!!
                        result.poi = frontResult.myFields?.poi!!
                        result.home_entities = frontResult.myFields?.homeEntities
                        result.resident_entities = frontResult.myFields?.residentEntities
                        result.decisionDetail = confirmResult.decision_detail?.details
                        result.nfc_data = confirmResult.nfc_data
                        kalapaResult = result
//                        Kalapa.klpHandler.onResult(result)
                        Helpers.printLog("Confirm Result NFC ${confirmResult.nfc_data}")
                        ProgressView.hideProgress()
                        finish()
                    }
                })
        }

        if (id == R.id.edDOB || id == R.id.edDOBDay) {
            val c = Calendar.getInstance()
            Helpers.printLog("DOB: $dob")
            if (dob != null) {
                c.time = dob
            }
            var year = c.get(Calendar.YEAR)
            var month = c.get(Calendar.MONTH)
            var day = c.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(
                this,
                android.R.style.Theme_Holo_Dialog,
                { view, year, monthOfYear, dayOfMonth ->
                    // Display Selected date in textbox
                    this.edDOBDay.setText(
                        "" + dayOfMonth + "/" + String.format(
                            "%02d",
                            monthOfYear + 1
                        ) + "/" + year
                    )
                }, year, month, day
            )
            dpd.show()
        }

        if (id == R.id.edDOE || id == R.id.edDOEDay) {
            val c = Calendar.getInstance()
            Helpers.printLog("DOE: $doe")
            if (doe != null) {
                c.time = doe
            }
            var year = c.get(Calendar.YEAR)
            var month = c.get(Calendar.MONTH)
            var day = c.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(
                this,
                android.R.style.Theme_Holo_Dialog,
                { view, year, monthOfYear, dayOfMonth ->
                    // Display Selected date in textbox
                    this.edDOEDay.setText(
                        "" + dayOfMonth + "/" + String.format(
                            "%02d",
                            monthOfYear + 1
                        ) + "/" + year
                    )
                }, year, month, day
            )
            dpd.show()
        }

        if (id == R.id.edDOI || id == R.id.edDOIDay) {
            val c = Calendar.getInstance()
            if (doi != null) {
                c.time = doi
            }
            Helpers.printLog("DOI: $doi")
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)


            val dpd = DatePickerDialog(
                this,
                android.R.style.Theme_Holo_Dialog,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                    // Display Selected date in textbox
                    this.edDOIDay.setText(
                        "" + dayOfMonth + "/" + String.format(
                            "%02d",
                            monthOfYear + 1
                        ) + "/" + year
                    )

                },
                year,
                month,
                day
            )

            dpd.show()
        }

    }

    // method request success
    override fun success(data: JSONObject) {

        Helpers.printLog("card_type : " + data.toString())
        val frontResult = FrontResult.fromJson(data.toString())
        if (frontResult != null) {
            KalapaSDK.frontResult = frontResult
            runOnUiThread {
                this.edName.setText(frontResult.myFields?.name)
                this.edDOBDay.setText(frontResult.myFields?.birthday)
                if (KalapaSDK.frontResult!!.myFields != null && KalapaSDK.frontResult.myFields!!.birthday != null) {
                    dob = Common.parseDate(KalapaSDK.frontResult.myFields?.birthday!!)
                }
                if (KalapaSDK.frontResult!!.myFields != null && KalapaSDK.frontResult.myFields!!.doi != null) {
                    doi = Common.parseDate(KalapaSDK.frontResult.myFields?.doi!!)
                }

                if (KalapaSDK.frontResult.myFields != null && KalapaSDK.frontResult.myFields!!.doe != null) {
                    doe = Common.parseDate(KalapaSDK.frontResult.myFields?.doe!!)
                    this.edDOEDay.setText(frontResult.myFields?.doe)
                } else {
                    edDOEDay.visibility = INVISIBLE
                }
                this.edDOIDay.setText(frontResult.myFields?.doi)
                this.edIDNumber.setText(frontResult.myFields?.idNumber)
                this.edPlaceOfIssue.setText(frontResult.myFields?.poi)
                this.edAddress.setText(frontResult.myFields?.resident)
                this.edHometown.setText(frontResult.myFields?.home)

                if (frontResult.qrCode?.data?.decoded_text != null)
                    this.edQrCode.setText(frontResult.qrCode?.data?.decoded_text)
                else
                    this.edQrCode.setText(frontResult.qrCode?.error?.message)


                if (frontResult.myFields?.gender == "Nữ") {
                    this.radioGroupSex.check(R.id.radio_female)
                }
                if (frontResult.myFields?.gender == "Nam") {
                    this.radioGroupSex.check(R.id.radio_male)
                }

            }
            // Try NFC here instead
        }
        ProgressView.hideProgress()
    }


    override fun fail(error: KalapaError) {
        Helpers.printLog("Error message: " + error.message)
        if (error.code == 12) {
            Helpers.showDialog(
                this@ConfirmActivity,
                resources.getString(R.string.klp_title_error),
                error.getMessageError() + ", " + resources.getString(R.string.klp_message_confirm_please_retry),
                R.drawable.sad_face,
                this
            )
        } else {
            Helpers.showDialog(this@ConfirmActivity, KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_error)), error.getMessageError(), R.drawable.sad_face)
        }
//        messageLabel.setText(error.message)
//        reviewImageLayout.show()
        ProgressView.hideProgress()

    }

    override fun timeout() {
        ProgressView.hideProgress()
    }

    override fun onYes() {
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1000)
//        Kalapa.klpHandler.onExpired()
    }

    override fun finish() {
//        if (Kalapa.showResult && Kalapa.isConfirmResultInitialized() && isFinishedConfirm) {
//            intent = Intent(this@ConfirmActivity, ResultActivity::class.java)
//            startActivity(intent)
//        } else {
//            if (KalapaSDK.result?.idNumber != null) {
//                Kalapa.klpHandler.onPreComplete()
//                Kalapa.klpHandler.onComplete(Kalapa.result!!)
//                if (Kalapa.kalapaListener != null)
//                    Kalapa.kalapaListener!!.completion(Kalapa.result!!)
//            }
//        }
        KalapaSDK.handler.onComplete(kalapaResult = kalapaResult)
        super.finish()
    }

    override fun onNo() {
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1000)
//        KalapaSDK.captureHandler
    }

    override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
        val result: Int = p1 and EditorInfo.IME_MASK_ACTION
        when (result) {
            EditorInfo.IME_ACTION_DONE -> {
                hideKeyboard()
            }

            EditorInfo.IME_ACTION_NEXT -> {
                hideKeyboard()
            }
        }
        return true
    }
}
