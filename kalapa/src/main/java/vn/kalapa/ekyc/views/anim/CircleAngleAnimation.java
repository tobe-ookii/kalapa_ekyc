package vn.kalapa.ekyc.views.anim;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import vn.kalapa.ekyc.views.CircleView;

public class CircleAngleAnimation extends Animation {

    private CircleView circle;

    private float oldAngle;
    private float newAngle;

    public CircleAngleAnimation(CircleView circle, int newAngle) {
        this.oldAngle = circle.getAngle();
        this.newAngle = newAngle;
        this.circle = circle;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float angle = oldAngle + ((newAngle - oldAngle) * interpolatedTime);
        circle.setAngle(angle);
        circle.requestLayout();
    }
}