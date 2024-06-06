package vn.kalapa.ekyc.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import vn.kalapa.R;


public class KLPResultRow extends LinearLayout {
    private static final String TAG = KLPResultRow.class.getSimpleName();
    TextView tvKey;
    TextView tvValue;

    public KLPResultRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.list_item, this, true);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.KLPResultRow);
        String firstLabel = typedArray.getString(R.styleable.KLPResultRow_tvKey);
        String secondLabel = typedArray.getString(R.styleable.KLPResultRow_tvValue);
        tvKey = findViewById(R.id.tv_title);
        tvValue = findViewById(R.id.tv_value);
        Log.d(TAG, "Label " + firstLabel);
        tvKey.setText(firstLabel);
        tvValue.setText(secondLabel);
        typedArray.recycle();
    }

    public void setRecordValue(String value) {
        tvValue.setText(value);
    }
}