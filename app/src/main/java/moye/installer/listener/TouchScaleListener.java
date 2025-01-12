package moye.installer.listener;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class TouchScaleListener implements View.OnTouchListener {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.animate().scaleX(0.93f).scaleY(0.93f).setInterpolator(new DecelerateInterpolator()).setDuration(170).start();
                break;
            default:
                view.animate().scaleX(1).scaleY(1).setDuration(170).start();
                break;
        }
        return false;
    }
}
