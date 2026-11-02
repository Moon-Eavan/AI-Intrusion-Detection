package com.example.photoviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * 외부 라이브러리 없이 ScaleGestureDetector + Matrix만으로 구현한
 * 최소한의 Pinch Zoom ImageView. (DetailActivity의 큰 이미지 확대용)
 *
 * 이미지가 설정되면 먼저 뷰 크기에 맞춰 가운데 정렬/축소(fitCenter와 유사)한 뒤,
 * 그 위에 pinch 제스처로 추가 확대/이동(pan)이 가능하도록 한다.
 */
public class ZoomableImageView extends AppCompatImageView {
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;

    private float baseScale = 1f;
    private float baseTranslateX = 0f;
    private float baseTranslateY = 0f;

    private float scaleFactor = 1f;
    private float posX = 0f;
    private float posY = 0f;
    private float lastTouchX;
    private float lastTouchY;

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        // 레이아웃이 끝난 뒤 뷰의 실제 width/height를 기준으로 기본 배치를 계산한다.
        post(this::initializeBaseMatrix);
    }

    private void initializeBaseMatrix() {
        Drawable drawable = getDrawable();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (drawable == null || viewWidth == 0 || viewHeight == 0) {
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }

        baseScale = Math.min((float) viewWidth / drawableWidth, (float) viewHeight / drawableHeight);
        baseTranslateX = (viewWidth - drawableWidth * baseScale) / 2f;
        baseTranslateY = (viewHeight - drawableHeight * baseScale) / 2f;

        scaleFactor = 1f;
        posX = 0f;
        posY = 0f;
        applyMatrix();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && scaleFactor > MIN_SCALE) {
                    posX += event.getX() - lastTouchX;
                    posY += event.getY() - lastTouchY;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    applyMatrix();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /** 새 이미지를 로드했을 때 확대/이동 상태를 초기 배치로 되돌린다. */
    public void resetZoom() {
        initializeBaseMatrix();
    }

    private void applyMatrix() {
        matrix.reset();
        matrix.postScale(baseScale * scaleFactor, baseScale * scaleFactor);
        matrix.postTranslate(baseTranslateX + posX, baseTranslateY + posY);
        setImageMatrix(matrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float previousScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

            // 손가락 사이 중심점(focus)을 기준으로 확대/축소되도록 보정한다.
            float scaleChange = scaleFactor / previousScale;
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            posX = focusX + (posX - focusX) * scaleChange;
            posY = focusY + (posY - focusY) * scaleChange;

            applyMatrix();
            return true;
        }
    }
}
