package vn.kalapa.ekyc.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import vn.kalapa.R;


public class KLPDecisionRow extends LinearLayout {
    private static final String TAG = KLPDecisionRow.class.getSimpleName();
    TextView tvKey;

    public KLPDecisionRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    void initView(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.list_decision, this, true);
        tvKey = findViewById(R.id.tv_title);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.KLPDecisionRow);
            String firstLabel = typedArray.getString(R.styleable.KLPDecisionRow_tvRule);
            Log.d(TAG, "Label " + firstLabel);
            tvKey.setText(firstLabel);
            typedArray.recycle();
        }
    }

    public KLPDecisionRow(Context context) {
        super(context);
        initView(context, null);
    }

    public void setRuleValue(String value) {
        tvKey.setText(value);
    }
}
