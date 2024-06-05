package vn.kalapa.ekyc.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import vn.kalapa.R;

public class CircleView extends View {

    private static final int START_ANGLE_POINT = 270;

    private final Paint paint;
    private final RectF rect;

    private float angle;

    private static Float frameWidth = 0F;
    private static Float frameHeight = 0F;

    private static Integer frameAngle = 0;
    private static Float strokeWidth = 0F;


    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleView);
        frameWidth = typedArray.getDimension(R.styleable.CircleView_frameWidth, 0);
        frameHeight = typedArray.getDimension(R.styleable.CircleView_frameHeight, 0);
        frameAngle = typedArray.getInteger(R.styleable.CircleView_viewAngle, 0);
        strokeWidth = typedArray.getDimension(R.styleable.CircleView_viewStroke, 20F);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        //Circle color
//        paint.setColor(Color.parseColor("#88D445"));
        paint.setColor(Color.BLUE);

        rect = new RectF(strokeWidth, strokeWidth, frameWidth + strokeWidth, frameHeight + strokeWidth);
//        rect = new RectF(0, 0, frameWidth, frameHeight);
        //Initial Angle (optional, it can be zero)
        angle = frameAngle;
    }

    public void setPaintColor(int color) {
        if (paint != null)
            paint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(rect, START_ANGLE_POINT, angle, true, paint);
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }
}