package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DataView extends View {

    private static final String TAG = "~DataView";

    private static final int MAX_HORZ_POINTS = 301;
    private static final int MAX_DATA_LINES = 3;
    private static final float DATA_LINE_WIDTH = 4F;

    private final Paint[] dataPaint = new Paint[MAX_DATA_LINES];
    private final float[][] yVals = new float[MAX_DATA_LINES][];
    private final float[] yRange = new float[MAX_DATA_LINES];
    private final float[][][] points = new float[MAX_DATA_LINES][2][MAX_HORZ_POINTS * 4 + 4];
    private final float[][] bufNext = new float[MAX_DATA_LINES][];
    private final float[] yMin = new float[MAX_DATA_LINES];
    private final float[] yMax = new float[MAX_DATA_LINES];
    private final float[][] bufPrev = new float[MAX_DATA_LINES][];
    private final int[] bufSel = new int[MAX_DATA_LINES];

    private int width, height;
    private final float[] xPts = new float[MAX_HORZ_POINTS];
    private int xSize;
    private int xRange, xOffs, prevXRange, prevXOffs;
    private int lineEnable;

    public DataView(Context context, AttributeSet attrs) {
        super(context, attrs);

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            bufPrev[i] = points[i][1];
            bufNext[i] = points[i][0];

            dataPaint[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            dataPaint[i].setStyle(Paint.Style.STROKE);
            dataPaint[i].setStrokeJoin(Paint.Join.ROUND);
            dataPaint[i].setStrokeCap(Paint.Cap.ROUND);
            dataPaint[i].setStrokeWidth(DATA_LINE_WIDTH);
        }
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

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) return;
            canvas.drawLines(bufNext[i], 0, numLines << 2, dataPaint[i]);
        }
    }

    public void enableLine(int lineNum, boolean enable) {
        if (enable)
            lineEnable |= (1 << lineNum);
        else
            lineEnable &= ~(1 << lineNum);
    }
    public void setLineColor(int lineNum, int color) {
        dataPaint[lineNum].setColor(color);
    }

    public void setYMin(int lineNum, float yMin) {
        this.yMin[lineNum] = yMin;
        yRange[lineNum] = this.yMax[lineNum] - this.yMin[lineNum];
    }

    public void setYMax(int lineNum, float yMax) {
        this.yMax[lineNum] = yMax;
        yRange[lineNum] = this.yMax[lineNum] - this.yMin[lineNum];
    }

    public void setYMinMax(int lineNum, float yMin, float yMax) {
        this.yMin[lineNum] = yMin;
        this.yMax[lineNum] = yMax;
        yRange[lineNum] = this.yMax[lineNum] - this.yMin[lineNum];
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

    public void setYVals(int lineNum, float[] yVals) {
        this.yVals[lineNum] = yVals;
    }

    public void update() {
        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) return;
            renderLine(i);
        }
        postInvalidate();
    }

    public void incUpdate() {
        xSize++;
        update();
    }

    private void renderLine(int lineNum) {
        int j;
        float x, y;

        bufPrev[lineNum] = points[lineNum][bufSel[lineNum]];
        bufSel[lineNum] = bufSel[lineNum] ^ 0x1;
        bufNext[lineNum] = points[lineNum][bufSel[lineNum]];

        if (xOffs == prevXOffs + 1 && xRange == prevXRange) {
            /* shift left all prev lines */
            int xRangeMT = xRange - 2;
            j = 0;
            for (int i = 0; i < xRangeMT; i++) {
                j = i << 2;
                bufNext[lineNum][j+1] = bufPrev[lineNum][j+5];
                bufNext[lineNum][j+3] = bufPrev[lineNum][j+7];
            }

            /* add new, last line */
            j += 4;
            bufNext[lineNum][j+1] = bufPrev[lineNum][j+3];
            bufNext[lineNum][j+3] = calcY(lineNum, xOffs + xRangeMT + 1);
        } else {
            bufNext[lineNum][0] = 0;
            bufNext[lineNum][1] = calcY(lineNum, xOffs);
            j = 2;
            int xStop = xOffs + xRange;
            for (int i = xOffs + 1; i <= xStop; i++) {
                if (i > xSize) break;
                x = xPts[i - xOffs];
                y = calcY(lineNum, i);
                bufNext[lineNum][j++] = x;
                bufNext[lineNum][j++] = y;
                bufNext[lineNum][j++] = x;
                bufNext[lineNum][j++] = y;
            }
        }
        prevXOffs = xOffs;
        prevXRange = xRange;
    }

    private float calcY(int lineNum, int idx) {
        float val = yVals[lineNum][idx];
        if (val > yMax[lineNum])
            return height;
        else if (val < yMin[lineNum])
            return 0;
        else
            return (((val - yMin[lineNum]) / yRange[lineNum]) * height);
    }

    private void calcXPts() {
        float xRangeMO = xRange - 1;
        for (int i = 0; i < xRange; i++) {
            xPts[i] = (i * width) / xRangeMO;
            //Log.i(TAG, "xPts = " + xPts[i]);
        }
    }

}
