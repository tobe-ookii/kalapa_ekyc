package vn.kalapa.ekyc.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.core.view.ViewCompat;

import vn.kalapa.R;


public class KLPCustomSwitch extends RadioGroup {
    private static final String TAG = KLPCustomSwitch.class.getSimpleName();
    public RadioButton rbOne;
    public RadioButton rbOther;
    private String mainColor;
    private String textColor;
    Boolean isPositiveCheck;

    public void setMainColor(String mainColor) {
        this.mainColor = mainColor;
        refreshColor();
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
        refreshColor();
    }

    public KLPCustomSwitchChangeListener listener = null;

    public boolean isPostitiveCheck() {
        return isPositiveCheck;
    }

    public void refreshColor() {
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(Color.parseColor(mainColor)));
        rbOne.setTextColor(Color.parseColor(rbOne.isChecked() ? textColor : mainColor));
        rbOther.setTextColor(Color.parseColor(rbOther.isChecked() ? textColor : mainColor));
        rbOne.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
        rbOther.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
        this.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
    }
//    val background: String = "#FFFFFF",
//    val mainColor: String = "#62A583",
//    val mainTextColor: String = "#65657B",
//    val btnTextColor: String = "#FFFFFF",
//    val language: String = "vi",
//    val scenario: String = "REGISTER"

    public KLPCustomSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.custom_switch, this, true);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.KLPCustomSwitch);
        String firstLabel = typedArray.getString(R.styleable.KLPCustomSwitch_rb_first_label);
        String secondLabel = typedArray.getString(R.styleable.KLPCustomSwitch_rb_second_label);
        mainColor = "#62A583";
        textColor = "#65657B";
        rbOne = findViewById(R.id.rb_first_label);
        rbOther = findViewById(R.id.rb_second_label);
        rbOne.setOnClickListener(view -> switchChangeListener(true));
        rbOther.setOnClickListener(view -> switchChangeListener(false));
        rbOne.setText(firstLabel);
        rbOther.setText(secondLabel);
        refreshColor();
        typedArray.recycle();
    }

    public void switchChangeListener(boolean CUSTOM_SWITCH_ONE) {
        if (CUSTOM_SWITCH_ONE) {
            isPositiveCheck = true;
            check(rbOne.getId());
            rbOther.setTextColor(Color.parseColor(mainColor));
            rbOne.setTextColor(Color.parseColor(textColor));
            ViewCompat.setBackgroundTintList(rbOne, ColorStateList.valueOf(Color.parseColor(mainColor)));
        } else {
            check(rbOther.getId());
            isPositiveCheck = false;
            rbOne.setTextColor(Color.parseColor(mainColor));
            rbOther.setTextColor(Color.parseColor(textColor));
            ViewCompat.setBackgroundTintList(rbOther, ColorStateList.valueOf(Color.parseColor(mainColor)));
        }
        if (listener != null) listener.onValueChanged(isPositiveCheck);
    }

    public interface KLPCustomSwitchChangeListener {
        void onValueChanged(boolean isPositiveCheck);
    }

}

