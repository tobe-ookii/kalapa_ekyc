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


public class KLPCustomMultipleChoices extends RadioGroup {
    private static final String TAG = KLPCustomMultipleChoices.class.getSimpleName();
    public RadioButton rbOne;
    public RadioButton rbSecond;
    public RadioButton rbThird;
    private String mainColor;
    private String textColor;

    private int selectedIndex;

    public void setMainColor(String mainColor) {
        this.mainColor = mainColor;
        refreshColor();
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
        refreshColor();
    }

    public KLPCustomMultipleChoicesChangeListener listener = null;

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void refreshColor() {
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(Color.parseColor(mainColor)));
        rbOne.setTextColor(Color.parseColor(rbOne.isChecked() ? textColor : mainColor));
        rbOne.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
        rbSecond.setTextColor(Color.parseColor(rbSecond.isChecked() ? textColor : mainColor));
        rbSecond.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
        rbThird.setTextColor(Color.parseColor(rbThird.isChecked() ? textColor : mainColor));
        rbThird.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
        this.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainColor)));
    }


    public KLPCustomMultipleChoices(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.kalapa_custom_multiple_choice, this, true);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.KLPCustomSwitch);
        String firstLabel = typedArray.getString(R.styleable.KLPCustomSwitch_rb_first_label);
        String secondLabel = typedArray.getString(R.styleable.KLPCustomSwitch_rb_second_label);
        mainColor = "#62A583";
        textColor = "#65657B";

        rbOne = findViewById(R.id.rb_first_label);
        rbSecond = findViewById(R.id.rb_second_label);
        rbThird = findViewById(R.id.rb_third_label);

        rbOne.setOnClickListener(view -> switchChangeListener(0));
        rbOne.setText(firstLabel);

        rbSecond.setOnClickListener(view -> switchChangeListener(1));
        rbSecond.setText(secondLabel);

        rbThird.setOnClickListener(view -> switchChangeListener(2));
        rbThird.setText(secondLabel);

        refreshColor();
        typedArray.recycle();
    }

    public void switchChangeListener(int INDEX) {
        switch (INDEX) {
            case 0:
                selectedIndex = INDEX;
                check(rbOne.getId());
                rbSecond.setTextColor(Color.parseColor(mainColor));
                rbThird.setTextColor(Color.parseColor(mainColor));
                rbOne.setTextColor(Color.parseColor(textColor));
                ViewCompat.setBackgroundTintList(rbOne, ColorStateList.valueOf(Color.parseColor(mainColor)));
                if (listener != null) listener.onValueChanged(INDEX);
                break;
            case 1:
                selectedIndex = INDEX;
                check(rbSecond.getId());
                rbOne.setTextColor(Color.parseColor(mainColor));
                rbThird.setTextColor(Color.parseColor(mainColor));

                rbSecond.setTextColor(Color.parseColor(textColor));
                ViewCompat.setBackgroundTintList(rbSecond, ColorStateList.valueOf(Color.parseColor(mainColor)));
                if (listener != null) listener.onValueChanged(INDEX);
                break;
            case 2:
                selectedIndex = INDEX;
                check(rbThird.getId());
                rbOne.setTextColor(Color.parseColor(mainColor));
                rbSecond.setTextColor(Color.parseColor(mainColor));

                rbThird.setTextColor(Color.parseColor(textColor));
                ViewCompat.setBackgroundTintList(rbThird, ColorStateList.valueOf(Color.parseColor(mainColor)));
                if (listener != null) listener.onValueChanged(INDEX);
        }
    }

    public interface KLPCustomMultipleChoicesChangeListener {
        void onValueChanged(int selectedIndex);
    }

}

