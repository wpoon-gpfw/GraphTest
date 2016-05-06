package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DataView extends View {

    private static final String TAG = "~DataView";

    private static final int MAX_HORZ_POINTS = 400;

    private int width, height;
    private Paint dataPaint;
    private float[] yVals;
    private float yMin, yMax, yRange;
    private float[] xPts = new float[MAX_HORZ_POINTS];
    private int xRange, xOffs;
    private float[][] points = new float[2][MAX_HORZ_POINTS * 4 + 4];
    private float[] bufPrev, bufNext;

    public DataView(Context context, AttributeSet attrs) {
        super(context, attrs);

        dataPaint = new Paint();
        dataPaint.setAntiAlias(true);
        dataPaint.setColor(0xFF000000);
        dataPaint.setStyle(Paint.Style.STROKE);
        dataPaint.setStrokeJoin(Paint.Join.ROUND);
        dataPaint.setStrokeCap(Paint.Cap.ROUND);
        dataPaint.setStrokeWidth(4f);
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

    private int prevXOffs;

    public void setXOffs(int newXOffs) {
        if (xOffs == prevXOffs + 1) {
            xOffs = newXOffs;
            xOffsIncrByOne();
            return;
        }

        if (newXOffs < 0) {
            prevXOffs = 0;
            xOffs = 0;
        } else {
            xOffs = newXOffs;
        }
        update();
    }

    public void xOffsIncrByOne() {
        int ii, jj;

        bufPrev = points[bufSel];
        bufSel = bufSel ^ 0x1;
        bufNext = points[bufSel];

        /* shift left all prev lines */
        int xRangeMO = xRange - 1;
        ii = 0;
        jj = 4;
        for (int i = 0; i < xRangeMO; i++) {
            bufNext[ii++] = bufPrev[jj++];
            bufNext[ii++] = bufPrev[jj++];
            bufNext[ii++] = bufPrev[jj++];
            bufNext[ii++] = bufPrev[jj++];
        }

        /* add new, last line */
        jj = ii - 2;
        bufNext[ii++] = bufPrev[jj++];
        bufNext[ii++] = bufPrev[jj];
        bufNext[ii++] = xPts[xRangeMO];
        bufNext[ii] = calcY(yVals[xOffs + xRangeMO]);

        postInvalidate();
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
        //drawDivisions(canvas);
        canvas.scale(1, -1);
        canvas.translate(0, -height);
        canvas.drawLines(bufNext, 0, (xRange - 1) << 2, dataPaint);
        canvas.setMatrix(null);
    }

    private int bufSel;

    public void update() {
        float x, y;
        float yVal;
        int j;

        bufPrev = points[bufSel];
        bufSel = bufSel ^ 0x1;
        bufNext = points[bufSel];

        bufNext[0] = 0;
        bufNext[1] = calcY(yVals[xOffs]);
        j = 2;
        int xStop = xOffs + xRange;
        for (int i = xOffs + 1; i < xStop; i++) {
            yVal = calcY(yVals[i]);
            //Log.i(TAG, "yVals = " + yVal);
            x = xPts[i - xOffs];
            y = yVal;
            bufNext[j++] = x;
            bufNext[j++] = y;
            bufNext[j++] = x;
            bufNext[j++] = y;
        }

        postInvalidate();
    }

    public void setYVals(float[] yVals) {
        this.yVals = yVals;
    }

    private float calcY(float val) {
        float yOff = val - yMin;
        if (yOff < 0) yOff = 0;
        return ((yOff / yRange) * height);
    }

    private void calcXPts() {
        float xRangeMO = xRange - 1;
        for (int i = 0; i < xRange; i++) {
            xPts[i] = (i * width) / xRangeMO;
            //Log.i(TAG, "xPts = " + xPts[i]);
        }
    }

}
