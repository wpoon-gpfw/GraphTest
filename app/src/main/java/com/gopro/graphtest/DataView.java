package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataView extends View {

    private static final String TAG = "~DataView";

    private static final int MAX_HORZ_POINTS = 301;
    private static final int MAX_DATA_LINES = 3;
    private static final float DATA_LINE_WIDTH = 4F;

    private final Paint[] dataPaint = new Paint[MAX_DATA_LINES];
    private final float[][] yVals = new float[MAX_DATA_LINES][];
    private final float[] yRange = new float[MAX_DATA_LINES];
    private final float[][][] points = new float[MAX_DATA_LINES][2][MAX_HORZ_POINTS * 4 + 4];
    private final float[] yMin = new float[MAX_DATA_LINES];
    private final float[] yMax = new float[MAX_DATA_LINES];
    private final float[][] bufPrev = new float[MAX_DATA_LINES][];
    private final float[][] bufNext = new float[MAX_DATA_LINES][];
    private final int[] bufSel = new int[MAX_DATA_LINES];

    private int width, height;
    private final float[] xPts = new float[MAX_HORZ_POINTS];
    private int xSize, xRange, xOffs;
    private int xSizeDraw, xRangeDraw, xOffsDraw;
    private int lineEnable;
    private boolean changed;
    private int prevXRange, prevXOffs;
    private final ExecutorService lineRenderSvc;

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

        lineRenderSvc = Executors.newSingleThreadExecutor();
    }

    private float touchDnX, touchDnY, lastDistX;
    private int touchDnXOffs;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float distX, distY;

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchDnX = event.getX();
                //touchDnY = event.getY();
                touchDnXOffs = xOffs;
                return true;

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                distX = event.getX() - touchDnX;
                //distY = event.getY() - touchDnY ;
                if ((distX - lastDistX > 10) || (lastDistX - distX > 10)) {
                    lastDistX = distX;
                    xOffs = touchDnXOffs - (int) ((2F * distX * xRange) / width);
                    xOffs = (xOffs < 0) ? 0 : xOffs;
                    changed = true;
                    update(true);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                xOffs = touchDnXOffs;
                changed = true;
                update(true);
                return true;
        }
        return false;
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

        int numLines = ((xSizeDraw - xOffsDraw) > xRangeDraw) ?
                (xRangeDraw - 1) :
                (xSizeDraw - xOffsDraw - 1);
        if (numLines <= 0) return;

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) return;
            canvas.drawLines(bufNext[i], 0, numLines << 2, dataPaint[i]);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow: DataView");
        lineRenderSvc.shutdown();
    }

    public void destroy() {
        lineRenderSvc.shutdown();
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
        changed = true;
    }

    public void setYMax(int lineNum, float yMax) {
        this.yMax[lineNum] = yMax;
        yRange[lineNum] = this.yMax[lineNum] - this.yMin[lineNum];
        changed = true;
    }

    public void setYMinMax(int lineNum, float yMin, float yMax) {
        this.yMin[lineNum] = yMin;
        this.yMax[lineNum] = yMax;
        yRange[lineNum] = this.yMax[lineNum] - this.yMin[lineNum];
        changed = true;
    }

    public void setXRange(int xRange) {
        this.xRange = xRange;
        calcXPts();
        changed = true;
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

    private class LineRenderer implements Runnable {
        private final int xOffs, xRange, xSize;
        private final boolean changed;

        public LineRenderer(int xOffs, int xRange, int xSize, boolean changed) {
            this.xOffs = xOffs;
            this.xRange = xRange;
            this.xSize = xSize;
            this.changed = changed;
        }

        @Override
        public void run() {
            int j, xStop;
            float x, y;

            for (int lineNum = 0; lineNum < MAX_DATA_LINES; lineNum++) {
                if (((1 << lineNum) & lineEnable) == 0) break;

                bufPrev[lineNum] = points[lineNum][bufSel[lineNum]];
                bufSel[lineNum] = bufSel[lineNum] ^ 0x1;
                bufNext[lineNum] = points[lineNum][bufSel[lineNum]];

                if (!changed && xOffs == prevXOffs + 1 && xRange == prevXRange) {
                    /* shift left all prev lines */
                    int xRangeMT = xRange - 2;
                    j = 0;
                    for (int i = 0; i < xRangeMT; i++) {
                        j = i << 2;
                        bufNext[lineNum][j + 1] = bufPrev[lineNum][j + 5];
                        bufNext[lineNum][j + 3] = bufPrev[lineNum][j + 7];
                    }

                    /* add new, last line */
                    j += 4;
                    bufNext[lineNum][j + 0] = bufPrev[lineNum][j + 0];
                    bufNext[lineNum][j + 1] = bufPrev[lineNum][j + 3];
                    bufNext[lineNum][j + 2] = bufPrev[lineNum][j + 2];
                    bufNext[lineNum][j + 3] = calcY(lineNum, xOffs + xRangeMT + 1);
                } else {
                    xStop = xOffs + xRange;
                    bufNext[lineNum][0] = 0;
                    bufNext[lineNum][1] = calcY(lineNum, xOffs);
                    j = 2;
                    for (int i = xOffs + 1; i <= xStop; i++) {
                        if (i >= xSize) break;
                        x = xPts[i - xOffs];
                        y = calcY(lineNum, i);
                        bufNext[lineNum][j++] = x;
                        bufNext[lineNum][j++] = y;
                        bufNext[lineNum][j++] = x;
                        bufNext[lineNum][j++] = y;
                    }
                }
            }

            xOffsDraw = xOffs;
            xRangeDraw = xRange;
            xSizeDraw = xSize;

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
    }

    public synchronized void update(boolean isUI) {
        lineRenderSvc.execute(new LineRenderer(xOffs, xRange, xSize, changed));
        changed = false;

        if (isUI)
            invalidate();
        else
            postInvalidate();
    }

    public void incUpdate(boolean isUI) {
        xSize++;
        xOffs = xSize - xRange;
        xOffs = (xOffs < 0) ? 0 : xOffs;

        update(isUI);
    }

    private void calcXPts() {
        float xRangeMO = xRange - 1;
        for (int i = 0; i < xRange; i++) {
            xPts[i] = (i * width) / xRangeMO;
            //Log.i(TAG, "xPts = " + xPts[i]);
        }
    }

}
