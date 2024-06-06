package vn.kalapa.ekyc.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import vn.kalapa.R
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.utils.Helpers


enum class GuideType {
    FRONT,
    BACK,
    SELFIE,
    MRZ,
    PASSPORT
}

class BottomGuideFragment(var layoutType: GuideType) : Fragment() {
    private lateinit var tv_note_0: TextView
    private lateinit var tv_note_1: TextView
    private lateinit var tv_note_2: TextView
    private lateinit var tv_note_3: TextView
    private lateinit var tv_note_4: TextView
    private lateinit var tv_note_5: TextView
    private lateinit var tv_note_6: TextView
    private lateinit var tv_note_7: TextView
    private lateinit var tv_note_31: TextView
    private lateinit var iv_guide: ImageView
    private lateinit var iv_guide_1: ImageView
    private lateinit var iv_guide_2: ImageView
    private lateinit var iv_guide_3: ImageView
    private lateinit var noteLabel: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Helpers.printLog("Bottom Guide Fragment.... $layoutType")
        val view = inflater.inflate(
            R.layout.activity_guide_id,
            container,
            false
        )
        var container = view.findViewById<View>(R.id.container)
        var btnUnderstand = view.findViewById<Button>(R.id.nextButton)
        btnUnderstand.setOnClickListener {
            Helpers.printLog("Btn Next Inject...")
            activity?.onBackPressed()
        }

        container.setOnClickListener {
            Helpers.printLog("On OutSide Touch")
            container.setBackgroundColor(resources.getColor(R.color.transparent))
            activity?.onBackPressed()
        }
        Handler().postDelayed({
            container.setBackgroundColor(resources.getColor(R.color.black20))
        }, 350)


        noteLabel = view.findViewById(R.id.noteLabel)
        tv_note_0 = view.findViewById(R.id.tv_note_0)
        tv_note_1 = view.findViewById(R.id.tv_note_1)
        tv_note_2 = view.findViewById(R.id.tv_note_2)
        tv_note_3 = view.findViewById(R.id.tv_note_3)
        tv_note_4 = view.findViewById(R.id.tv_note_4)
        tv_note_5 = view.findViewById(R.id.tv_note_5)
        tv_note_6 = view.findViewById(R.id.tv_note_6)
        tv_note_7 = view.findViewById(R.id.tv_note_7)
        tv_note_31 = view.findViewById(R.id.tv_note_31)
        iv_guide = view.findViewById(R.id.iv_guide_0)
        iv_guide_1 = view.findViewById(R.id.iv_guide_1)
        iv_guide_2 = view.findViewById(R.id.iv_guide_2)
        iv_guide_3 = view.findViewById(R.id.iv_guide_3)

        // setup color
        val mainTextColor = KalapaSDK.config.mainTextColor
        var textArray = this.getStringFromLayout()

        tv_note_0.setTextColor(Color.parseColor(mainTextColor))
        tv_note_1.setTextColor(Color.parseColor(mainTextColor))
        tv_note_2.setTextColor(Color.parseColor(mainTextColor))
        tv_note_3.setTextColor(Color.parseColor(mainTextColor))
        tv_note_4.setTextColor(Color.parseColor(mainTextColor))
        tv_note_5.setTextColor(Color.parseColor(mainTextColor))
        tv_note_6.setTextColor(Color.parseColor(mainTextColor))
        tv_note_7.setTextColor(Color.parseColor(mainTextColor))
        tv_note_31.setTextColor(Color.parseColor(mainTextColor))
        noteLabel.setTextColor(Color.parseColor(mainTextColor))
        if (textArray.size == 10) {
            noteLabel.text = textArray[0]
            tv_note_0.text = textArray[1]
            tv_note_1.text = textArray[2]
            tv_note_2.text = textArray[3]
            tv_note_3.text = textArray[4]
            tv_note_31.text = textArray[5]
            tv_note_4.text = textArray[6]
            tv_note_5.text = textArray[7]
            tv_note_6.text = textArray[8]
            tv_note_7.text = textArray[9]
        }
        btnUnderstand.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_button))
        btnUnderstand.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
        ViewCompat.setBackgroundTintList(
            btnUnderstand,
            ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
        )
        updateLayout()
        return view
    }

    private fun updateLayout() {
        var drawableArray = this.getResourcesFromLayout()
        if (drawableArray.size == 4) {
            iv_guide.scaleType = ImageView.ScaleType.CENTER_INSIDE
//            iv_guide.background = resources.getDrawable(drawableArray[0])
            iv_guide.setBackgroundResource(drawableArray[0])
            iv_guide_1.scaleType = ImageView.ScaleType.CENTER_INSIDE
            iv_guide_1.setBackgroundResource(drawableArray[1])
//            iv_guide_1.background = resources.getDrawable(drawableArray[1])
            iv_guide_2.scaleType = ImageView.ScaleType.CENTER_INSIDE
            iv_guide_2.setBackgroundResource(drawableArray[2])
//            iv_guide_2.background = resources.getDrawable(drawableArray[2])
            iv_guide_3.scaleType = ImageView.ScaleType.CENTER_INSIDE
            iv_guide_3.setBackgroundResource(drawableArray[3])
//            iv_guide_3.background = resources.getDrawable(drawableArray[3])
        }
//        Helpers.printLog("Bottom Guide drawableArray.... ${drawableArray[0]} - ${drawableArray[1]} - ${drawableArray[2]} - ${drawableArray[3]}")
        noteLabel.text =
            if (layoutType == GuideType.FRONT) KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_front))
            else if (layoutType == GuideType.BACK) KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_back))
            else if (layoutType == GuideType.MRZ) KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_mrz))
            else if (layoutType == GuideType.PASSPORT) KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_passport))
            else KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_selfie))
//        if (layoutType == GuideType.SELFIE) {
//            tv_note_1.text = resources.getString(R.string.klp_guide_liveness_1)
//            tv_note_2.text = resources.getString(R.string.klp_guide_liveness_2)
//            tv_note_3.text = resources.getString(R.string.klp_guide_liveness_3)
//            tv_note_4.text = resources.getString(R.string.klp_guide_liveness_4)
//
//            tv_note_5.text = resources.getString(R.string.klp_guide_liveness_2_1)
//            tv_note_6.text = resources.getString(R.string.klp_guide_liveness_3_1)
//            tv_note_7.text = resources.getString(R.string.klp_guide_liveness_4_1)
//        }
    }

    private fun getStringFromLayout(): List<String> {
        when (layoutType) {
            GuideType.PASSPORT -> return listOf(
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_passport)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_title)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_passport_1)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_passport_2)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_passport_3)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_passport_4)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_reject_title)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_reject_1)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_reject_2)),
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_reject_3))
            )

            else -> return listOf(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            )
        }
    }

    private fun getResourcesFromLayout(): IntArray {
        when (layoutType) {
            GuideType.FRONT -> return intArrayOf(
                R.drawable.guide_id,
                R.drawable.idguide1,
                R.drawable.idguide2,
                R.drawable.idguide3
            )

            GuideType.BACK, GuideType.MRZ -> return intArrayOf(
                R.drawable.guide_id_back,
                R.drawable.guide_id_back1,
                R.drawable.guide_id_back2,
                R.drawable.guide_id_back3,
            )

            GuideType.PASSPORT -> return intArrayOf(
                R.drawable.klp_passport_guide_0,
                R.drawable.klp_passport_guide_3,
                R.drawable.klp_passport_guide_2,
                R.drawable.klp_passport_guide_1,
            )

            GuideType.SELFIE -> return intArrayOf(
//                R.drawable.guide_selfie,
//                R.drawable.guide_selfie1,
//                R.drawable.guide_selfie2,
//                R.drawable.guide_selfie3
            )
        }
        return intArrayOf()
    }


}