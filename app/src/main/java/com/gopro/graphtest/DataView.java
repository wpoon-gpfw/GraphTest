package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DataView extends View {

    private static final String TAG = "~DataView";

    private static final int MAX_HORZ_POINTS = 301;

    private int width, height;
    private Paint dataPaint;
    private float[] yVals;
    private int xSize;
    private float yMin, yMax, yRange;
    private float[] xPts = new float[MAX_HORZ_POINTS];
    private int xRange, xOffs, prevXRange, prevXOffs;
    private float[][] points = new float[2][MAX_HORZ_POINTS * 4 + 4];
    private float[] bufPrev = points[0];
    private float[] bufNext = points[1];
    private int bufSel = 1;

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

    public void setXOffs(int xOffs) {
        this.xOffs = (xOffs < 0) ? 0 : xOffs;
    }

    public void setXSize(int xSize) {
        this.xSize = xSize;
    }

    private void renderLine() {
        int j;
        float x, y;

        bufPrev = points[bufSel];
        bufSel = bufSel ^ 0x1;
        bufNext = points[bufSel];

        if (xOffs == prevXOffs + 1 && xRange == prevXRange) {
            /* shift left all prev lines */
            int xRangeMT = xRange - 2;
            j = 0;
            for (int i = 0; i < xRangeMT; i++) {
                j = i << 2;
                bufNext[j+1] = bufPrev[j+5];
                bufNext[j+3] = bufPrev[j+7];
            }

            /* add new, last line */
            j += 4;
            bufNext[j+1] = bufPrev[j+3];
            bufNext[j+3] = calcY(yVals[xOffs + xRangeMT + 1]);
        } else {
            bufNext[0] = 0;
            bufNext[1] = calcY(yVals[xOffs]);
            j = 2;
            int xStop = xOffs + xRange;
            for (int i = xOffs + 1; i <= xStop; i++) {
                if (i > xSize) break;
                x = xPts[i - xOffs];
                y = calcY(yVals[i]);
                bufNext[j++] = x;
                bufNext[j++] = y;
                bufNext[j++] = x;
                bufNext[j++] = y;
            }
        }
        prevXOffs = xOffs;
        prevXRange = xRange;
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
        canvas.scale(1, -1);
        canvas.translate(0, -height);
        int numLines = (xSize > xRange) ? (xRange - 1) : (xSize - 1);
        canvas.drawLines(bufNext, 0, numLines << 2, dataPaint);
    }

    public void update() {
        renderLine();
        postInvalidate();
    }

    public void incUpdate() {
        xSize++;
        update();
    }

    public void setYVals(float[] yVals) {
        this.yVals = yVals;
    }

    private float calcY(float val) {
        //return val;
        if (val > yMax)
            return height;
        else if (val < yMin)
            return 0;
        else
            return (((val - yMin) / yRange) * height);
    }

    private void calcXPts() {
        float xRangeMO = xRange - 1;
        for (int i = 0; i < xRange; i++) {
            xPts[i] = (i * width) / xRangeMO;
            //Log.i(TAG, "xPts = " + xPts[i]);
        }
    }

}
