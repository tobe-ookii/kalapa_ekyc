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
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.FrontResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.networks.Client
import vn.kalapa.ekyc.networks.KalapaAPI
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView
import java.util.*


class ConfirmActivity : BaseActivity(), View.OnClickListener, Client.RequestListener,
    DialogListener, OnEditorActionListener {
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
            intent.getStringExtra("leftover_session") ?: "",
            this@ConfirmActivity
        )

    }

    override fun onBackPressed() {
//        super.onBackPressed()
        Helpers.showEndKYC(this, object : DialogListener {
            override fun onYes() {
                KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
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
        tvTitle.text =
            KLPLanguageManager.get(resources.getString(R.string.klp_confirm_title))
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
        tvConfirm0.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvConfirm1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        // klp_results_info_name
        tvConfirm1.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_name))
        tvConfirm2.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // klp_results_info_gender
        tvConfirm2.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_gender))
        tvConfirm3.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // klp_results_info_dob
        tvConfirm3.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_dob))
        tvConfirm4.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // klp_results_info_id
        tvConfirm4.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_id))
        tvConfirm5.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // results_info_doi
        tvConfirm5.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_doi))
        tvConfirm6.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // results_info_poi
        tvConfirm6.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_poi))

        tvConfirm7.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // results_info_res
        tvConfirm7.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_res))
        tvConfirm8.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvConfirm8.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_res))
        tvConfirm9.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor)) // results_info_home
        tvConfirm9.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_home))
        tvConfirm10.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvConfirm10.text = KLPLanguageManager.get(resources.getString(R.string.klp_results_info_doe)) //results_info_doe
        radioMale.text = KLPLanguageManager.get(resources.getString(R.string.klp_confirm_gender_m))
        radioFemale.text = KLPLanguageManager.get(resources.getString(R.string.klp_confirm_gender_f))
        this.btnPersonalConfirm.let {
            ViewCompat.setBackgroundTintList(it, ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor)))
        }
        btnPersonalConfirm.text = KLPLanguageManager.get(resources.getString(R.string.klp_button_confirm))
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
            getString(R.string.klp_confirm_warning) + " '${field}'",
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
            val sex = radioGroupSex.checkedRadioButtonId
            val gd = KLPLanguageManager.get(
                if (sex == R.id.radio_male)
                    resources.getString(R.string.klp_confirm_gender_m)
                else resources.getString(
                    R.string.klp_confirm_gender_f
                )
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
                intent.getStringExtra("leftover_session") ?: "",
                object : Client.ConfirmListener {
                    override fun fail(error: KalapaError) {
                        ProgressView.hideProgress()
                        Helpers.showDialog(
                            this@ConfirmActivity,
                            KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown)),
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
                        result.birthday = frontResult.fields?.birthday!!
                        if (frontResult.fields != null && frontResult.fields!!.birthday != null) {
                            dob = Common.parseDate(frontResult.fields?.birthday!!)
                        }
                        result.gender = frontResult.fields?.gender!!
                        result.home = frontResult.fields?.home!!
                        result.type = frontResult.fields?.type!!
                        result.decision = confirmResult.decision_detail?.decision
                        result.resident = frontResult.fields?.resident!!
                        result.session = confirmResult.session
                        result.idNumber = frontResult.fields?.id_number!!
                        result.qr_code = frontResult.qr_code
                        isFinishedConfirm = true
                        result.mrz_data = frontResult.mrz_data
                        result.doe = frontResult.fields?.doe!!
                        result.doi = frontResult.fields?.doi!!
                        if (frontResult.fields?.features != null) {
                            result.features = frontResult.fields?.features!!
                        }
                        if (frontResult.fields != null && frontResult.fields!!.doi != null) {
                            doi = Common.parseDate(frontResult.fields?.doi!!)
                        }
                        if (frontResult.fields != null && frontResult.fields!!.doe != null) {
                            doe = Common.parseDate(frontResult.fields?.doe!!)
                        }
                        result.name = frontResult.fields?.name!!
                        result.poi = frontResult.fields?.poi!!
                        result.home_entities = frontResult.fields?.home_entities
                        result.resident_entities = frontResult.fields?.resident_entities
                        result.decision_detail = confirmResult.decision_detail?.details
                        result.nfc_data = confirmResult.nfc_data
                        if (confirmResult.selfie_data != null)
                            result.selfie_data = confirmResult.selfie_data.data
                        kalapaResult = result
//                        Kalapa.klpHandler.onResult(result)
                        Helpers.printLog("Confirm Selfie Data ${confirmResult.selfie_data}")
                        Helpers.printLog("Confirm Result NFC ${confirmResult.nfc_data}")
                        ProgressView.hideProgress()
                        KalapaSDK.handler.onComplete(kalapaResult = kalapaResult)
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
                this.edName.setText(frontResult.fields?.name)
                this.edDOBDay.setText(frontResult.fields?.birthday)
                if (KalapaSDK.frontResult!!.fields != null && KalapaSDK.frontResult.fields!!.birthday != null) {
                    dob = Common.parseDate(KalapaSDK.frontResult.fields?.birthday!!)
                }
                if (KalapaSDK.frontResult!!.fields != null && KalapaSDK.frontResult.fields!!.doi != null) {
                    doi = Common.parseDate(KalapaSDK.frontResult.fields?.doi!!)
                }

                if (KalapaSDK.frontResult.fields != null && KalapaSDK.frontResult.fields!!.doe != null) {
                    doe = Common.parseDate(KalapaSDK.frontResult.fields?.doe!!)
                    this.edDOEDay.setText(frontResult.fields?.doe)
                } else {
                    edDOEDay.visibility = INVISIBLE
                }
                this.edDOIDay.setText(frontResult.fields?.doi)
                this.edIDNumber.setText(frontResult.fields?.id_number)
                this.edPlaceOfIssue.setText(frontResult.fields?.poi)
                this.edAddress.setText(frontResult.fields?.resident)
                this.edHometown.setText(frontResult.fields?.home)

                if (frontResult.qr_code?.data?.decoded_text != null)
                    this.edQrCode.setText(frontResult.qr_code?.data?.decoded_text)
                else
                    this.edQrCode.setText(frontResult.qr_code?.error?.message)


                if (frontResult.fields?.gender == "Nữ") {
                    this.radioGroupSex.check(R.id.radio_female)
                }
                if (frontResult.fields?.gender == "Nam") {
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
                resources.getString(R.string.klp_error_unknown),
                error.getMessageError() + ", " + resources.getString(R.string.klp_button_retry),
                R.drawable.sad_face,
                this
            )
        } else {
            Helpers.showDialog(
                this@ConfirmActivity,
                KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown)),
                error.getMessageError(),
                R.drawable.sad_face
            )
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
