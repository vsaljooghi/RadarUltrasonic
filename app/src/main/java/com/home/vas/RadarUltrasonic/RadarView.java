package com.home.vas.RadarUltrasonic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import java.util.Arrays;

public class RadarView extends View{

	public static final String TAG = "RADAR_VIEW";
	private static final int MAX_MEASUR = 181;		// stores max number of measurement points that can be drawn on radar
	private static final int  mMaxDistance = 200;  //200 cm, maximum distance ultrasonic sensor can measure
	int counterMeasurements = 0;
	double theta = 0;

	private float mWidth = 0.0f;
	private float mHeight = 0.0f;
	private float mCmToPix = 0.0f;
	private Paint mGridLinesPaint;
	private Paint mGridDistancePaint;
	private Paint mGridDegreePaint;
	private Paint mMeasurementPaint;
	private Paint mLinePaint;

	private float cx;
	private float cy;
	private Context mContext;

	private float[] d1x = new float[MAX_MEASUR];
	private float[] d1y = new float[MAX_MEASUR];

	private float[] d2x = new float[MAX_MEASUR];
	private float[] d2y = new float[MAX_MEASUR];

	public RadarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		loadPaints();
		Arrays.fill(d1x,-1);
		Arrays.fill(d1y,-1);

		Arrays.fill(d2x,-1);
		Arrays.fill(d2y,-1);
	}

	private void loadPaints() {
		mGridLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGridLinesPaint.setStyle(Style.STROKE);
		mGridLinesPaint.setStrokeWidth(2.0f);
		mGridLinesPaint.setColor(ContextCompat.getColor(mContext, R.color.Green));
		mGridLinesPaint.setAlpha(127);

		mGridDistancePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGridDistancePaint.setColor(ContextCompat.getColor(mContext, R.color.Blue));
		mGridDistancePaint.setTextSize(16.0f);

		mGridDegreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGridDegreePaint.setColor(ContextCompat.getColor(mContext, R.color.Green));
		mGridDegreePaint.setTextSize(16.0f);

		mMeasurementPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mMeasurementPaint.setStyle(Style.STROKE);
		mMeasurementPaint.setColor(Color.RED);
		mMeasurementPaint.setStrokeWidth(4.0f);

		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setColor(Color.WHITE);
		mLinePaint.setStrokeWidth(10.0f);
		mLinePaint.setAlpha(127);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawCircle(cx, cy, mHeight/8.0f, mGridLinesPaint);
		canvas.drawCircle(cx, cy, mHeight*2.0f/8.0f, mGridLinesPaint);
		canvas.drawCircle(cx, cy, mHeight*3.0f/8.0f, mGridLinesPaint);
		canvas.drawCircle(cx, cy, mHeight*4.0f/8.0f-2, mGridLinesPaint);

		int alpha = 0;
		for (double beta = 0.0; beta < 2.0*Math.PI; beta += (30.0/180.0)*Math.PI) {
			float cxEnd = cx + mHeight/2.0f*(float)Math.cos(beta);
			float cyEnd = cy - mHeight/2.0f*(float)Math.sin(beta);

			canvas.drawLine(cx, cy, cxEnd,cyEnd, mGridLinesPaint);

			if(beta == 0.0)
				cxEnd -= 12;
			if(beta == (90.0/180.0)*Math.PI)
				cyEnd += 12;

			canvas.drawText(String.valueOf(alpha)+(char)0x00B0, cxEnd, cyEnd, mGridDegreePaint);
			alpha += 30;
		}

		canvas.drawText(String.valueOf(mMaxDistance) , cx - mHeight*4.0f/8.0f , cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(3*mMaxDistance/4), cx - mHeight*3.0f/8.0f, cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(2*mMaxDistance/4), cx - mHeight*2.0f/8.0f, cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(1*mMaxDistance/4), cx - mHeight/8.0f, cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(1*mMaxDistance/4), cx + mHeight/8.0f-18, cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(2*mMaxDistance/4), cx + mHeight*2.0f/8.0f-28, cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(3*mMaxDistance/4), cx + mHeight*3.0f/8.0f-28, cy + 12, mGridDistancePaint);
		canvas.drawText(String.valueOf(mMaxDistance) , cx + mHeight*4.0f/8.0f-28, cy + 12, mGridDistancePaint);

		for (int i=0; d1x[i]!=-1; i++) {
			if(d1x[i]!= 300 || d1y[i]!=300)
				canvas.drawPoint(d1x[i], d1y[i], mMeasurementPaint);
		}

		for (int i=0; d2x[i]!=-1; i++) {
			if(d2x[i]!= 300 || d2y[i]!=300)
				canvas.drawPoint(d2x[i], d2y[i], mMeasurementPaint);
		}

		canvas.drawLine(cx+(mHeight/2.0f*(float)Math.cos(theta + Math.PI )), cy-(mHeight/2.0f*(float)Math.sin(theta + Math.PI )), cx+(mHeight/2.0f*(float)Math.cos(theta)),cy-mHeight/2.0f*(float)Math.sin(theta), mLinePaint);
	}

	public void drawMeasurement(int angle, int distance1, int distance2) {

		if(angle != -1) { //-1 means end of measurement
			theta = angle * Math.PI / 180.0;

			d1x[counterMeasurements] = cx + mCmToPix * ((float) distance1 * (float) Math.cos(theta));
			d1y[counterMeasurements] = cy - mCmToPix * ((float) distance1 * (float) Math.sin(theta));

			d2x[counterMeasurements] = cx + mCmToPix * ((float) distance2 * (float) Math.cos(theta + Math.PI));
			d2y[counterMeasurements] = cy - mCmToPix * ((float) distance2 * (float) Math.sin(theta + Math.PI));

			counterMeasurements++;
		}else if(angle == -1 && counterMeasurements == 1) {
			counterMeasurements = 0;
		}else {
			Arrays.fill(d1x, -1);
			Arrays.fill(d1y, -1);
			Arrays.fill(d2x, -1);
			Arrays.fill(d2y, -1);
			counterMeasurements = 0;
			theta = 0;
		}

		this.postInvalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = (float) w;
		mHeight = (float) h;

		cx = mWidth / 2.0f;
		cy = mHeight / 2.0f;
		mCmToPix = (mHeight/2.0f)/mMaxDistance;
	}
}
