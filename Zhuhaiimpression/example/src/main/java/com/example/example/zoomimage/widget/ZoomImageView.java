package com.example.example.zoomimage.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * @author: tuacy
 * @date: 2015/11/10 13:45
 * @version: V1.0
 */
public class ZoomImageView extends ImageView
	implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

	private float                mInitScale            = 0.0f;
	private float                mMaxScale             = 0.0f;
	private Matrix               mScaleMatrix          = null;
	private ScaleGestureDetector mScaleGestureDetector = null;

	private int     mLastPointCount = 0;
	private float   mLastX          = 0;
	private float   mLastY          = 0;
	private int     mTouchSlop      = 0;
	private boolean mIsCanDrag      = false;


	public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setScaleType(ScaleType.MATRIX);
		mScaleMatrix = new Matrix();
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		setOnTouchListener(this);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	public ZoomImageView(Context context) {
		this(context, null);
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().removeOnGlobalLayoutListener(this);
	}

	@Override
	public void onGlobalLayout() {
		/** view width and height */
		int width = getWidth();
		int height = getHeight();

		Drawable drawable = getDrawable();
		/** get picture width and height */
		int dw = drawable.getIntrinsicWidth();
		int dh = drawable.getIntrinsicHeight();

		/** get scale */
		float scale = 1.0f;
		if (dw > width && dh < height) {
			scale = width * 1.0f / dw;
		}
		if (dh > height && dw < width) {
			scale = height * 1.0f / dh;
		}
		if ((dw > width && dh > height) || (dw < width && dh < height)) {
			scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
		}

		mInitScale = scale;
		mMaxScale = mInitScale * 4;

		int dx = getWidth() / 2 - dw / 2;
		int dy = getHeight() / 2 - dh / 2;
		mScaleMatrix.postTranslate(dx, dy);
		mScaleMatrix.postScale(scale, scale, getWidth() / 2, getHeight() / 2);
		setImageMatrix(mScaleMatrix);
		getViewTreeObserver().removeOnGlobalLayoutListener(this);
	}

	public float getScale() {
		float[] scales = new float[9];
		mScaleMatrix.getValues(scales);
		return scales[Matrix.MSCALE_X];
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scale = getScale();
		float scaleFactory = detector.getScaleFactor();
		if (null == getDrawable()) {
			return true;
		}

		if ((scale < mMaxScale && scaleFactory > 1.0f) || (scale > mInitScale && scaleFactory < 1.0f)) {
			if (scale * scaleFactory > mMaxScale) {
				scaleFactory = mMaxScale / scale;
			}

			if (scale * scaleFactory < mInitScale) {
				scaleFactory = mInitScale / scale;
			}
			mScaleMatrix.postScale(scaleFactory, scaleFactory, detector.getFocusX(), detector.getFocusY());
			setImageMatrix(mScaleMatrix);
			checkBorderAndCenterWhenScale();
		}
		return true;
	}

	private RectF getMatrixRectF() {
		RectF rectF = new RectF();
		Drawable drawable = getDrawable();
		if (null != drawable) {
			rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicWidth());
			mScaleMatrix.mapRect(rectF);
		}
		return rectF;
	}

	private void checkBorderAndCenterWhenScale() {
		RectF rectF = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;

		int width = getWidth();
		int height = getHeight();

		if (rectF.width() >= width) {
			if (rectF.left > 0) {
				deltaX = -rectF.left;
			}
			if (rectF.right < width) {
				deltaX = width - rectF.right;
			}
		}
		if (rectF.height() > height) {
			if (rectF.top > 0) {
				deltaY = -rectF.top;
			}
			if (rectF.bottom < height) {
				deltaY = height - rectF.bottom;
			}
		}
		if (rectF.width() < width) {
			deltaX = width / 2.0f - rectF.right + rectF.width() / 2.0f;
		}

		if (rectF.height() < height) {
			deltaY = height / 2.0f - rectF.bottom + rectF.height() / 2.0f;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
		setImageMatrix(mScaleMatrix);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mScaleGestureDetector.onTouchEvent(event);
		float x = 0;
		float y = 0;
		int pointerCount = event.getPointerCount();
		for (int i = 0; i < pointerCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}
		x /= pointerCount;
		y /= pointerCount;
		if (mLastPointCount != pointerCount) {
			mLastX = x;
			mLastY = y;
			mIsCanDrag = false;
		}
		mLastPointCount = pointerCount;
		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				float dx = x - mLastX;
				float dy = y - mLastY;
				if (!mIsCanDrag) {
					mIsCanDrag = isMoveAction(dx, dy);
				}
				if (mIsCanDrag) {
					mIsCanDrag = false;
				}
				break;
			case MotionEvent.ACTION_UP:
				break;
			case MotionEvent.ACTION_DOWN:
				break;
		}
		return true;
	}

	private boolean isMoveAction(float dx, float dy) {
		return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
	}
}