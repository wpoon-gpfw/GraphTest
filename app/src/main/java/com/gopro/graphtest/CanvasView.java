package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CanvasView extends View {

    private static final String TAG = "~CanvasView";

    private static final int MAX_HORZ_POINTS = 300;
    private static final int NUM_VERT_DIVISIONS = 6;
    private static final int NUM_HORZ_DIVISIONS = 8;

    private int width, height;
    private Path waveform;
    private Paint waveformPaint, borderPaint, divisionPaint;
    private float[] yVals;
    private float yMin, yMax, yRange;
    private float[] xPts = new float[MAX_HORZ_POINTS];
    private int xRange, xOffs;

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

        waveform = new Path();

        waveformPaint = new Paint();
        waveformPaint.setAntiAlias(true);
        waveformPaint.setColor(0xFF000000);
        waveformPaint.setStyle(Paint.Style.STROKE);
        waveformPaint.setStrokeJoin(Paint.Join.ROUND);
        waveformPaint.setStrokeCap(Paint.Cap.ROUND);
        waveformPaint.setStrokeWidth(4f);

        borderPaint = new Paint();
        borderPaint.setAntiAlias(false);
        borderPaint.setColor(0xFFA0A0A0);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeJoin(Paint.Join.MITER);
        borderPaint.setStrokeCap(Paint.Cap.SQUARE);
        borderPaint.setStrokeWidth(2F);

        divisionPaint = new Paint();
        divisionPaint.setAntiAlias(false);
        divisionPaint.setColor(0xFFB0B0B0);
        divisionPaint.setStyle(Paint.Style.STROKE);
        divisionPaint.setStrokeJoin(Paint.Join.MITER);
        divisionPaint.setStrokeCap(Paint.Cap.BUTT);
        divisionPaint.setStrokeWidth(0F);
    }

    public void setYMin(float yMin) {
        this.yMin = yMin;
        yRange = yMax - yMin;
    }

    public void setYMax(float yMax) {
        this.yMax = yMax;
        yRange = yMax - yMin;
    }

    public void setXRange(int xRange) {
        this.xRange = xRange;
        calcXPts();
    }

    public void setXOffs(int xOffs) {
        this.xOffs = (xOffs < 0) ? 0 : xOffs;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        calcXPts();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(2, 2, width - 2, height - 2, borderPaint);
        drawDivisions(canvas);
        canvas.drawPath(waveform, waveformPaint);
    }

    private void drawDivisions(Canvas canvas) {
        float pos;

        for (int i = 1; i < NUM_HORZ_DIVISIONS; i++) {
            pos = (i * width)/NUM_HORZ_DIVISIONS;
            canvas.drawLine(pos, 0, pos, height, divisionPaint);
        }
        for (int i = 1; i < NUM_VERT_DIVISIONS; i++) {
            pos = (i * height)/NUM_VERT_DIVISIONS;
            canvas.drawLine(0, pos, width, pos, divisionPaint);
        }
    }

    public void update() {
        waveform.reset();
        waveform.moveTo(0, height - calcY(yVals[xOffs]));
        int xStop = xOffs + xRange;
        for (int i = xOffs + 1; i < xStop; i++) {
            float yVal = calcY(yVals[i]);
            //Log.i(TAG, "yVals = " + yVal);
            waveform.lineTo(xPts[i - xOffs], height - yVal);
        }

        this.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    public void setYVals(float[] yVals) {
        this.yVals = yVals;
    }

    private float calcY(float val) {
        float yOff = val - yMin;
        if (yOff < 0) yOff = 0;
        return ((yOff/yRange) * height);
    }

    private void calcXPts() {
        float xRangeMO = xRange - 1;
        for (int i = 0; i < xRange ; i++) {
            xPts[i] = (i * width) / xRangeMO;
            //Log.i(TAG, "xPts = " + xPts[i]);
        }
    }

}
