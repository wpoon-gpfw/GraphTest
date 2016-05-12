package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataView extends View implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "~DataView";

    private static final int MAX_HORZ_POINTS = 400;
    private static final int MIN_XRANGE = 200;
    private static final int MAX_DATA_LINES = 3;
    private static final float DATA_LINE_WIDTH = 4F;

    private static final int CHANGED_XOFF = (1 << 0);
    private static final int CHANGED_XRANGE = (1 << 1);
    private static final int CHANGED_YRANGE = (1 << 2);


    private final Paint[] dataPaint = new Paint[MAX_DATA_LINES];
    private final float[][] yVals = new float[MAX_DATA_LINES][];
    private final float[] yRange = new float[MAX_DATA_LINES];
    private final float[][] points = new float[MAX_DATA_LINES][MAX_HORZ_POINTS * 4 + 8];
    private final float[] yMin = new float[MAX_DATA_LINES];
    private final float[] yMax = new float[MAX_DATA_LINES];
    private final float[] yAbsMin = new float[MAX_DATA_LINES];
    private final float[] yAbsMax = new float[MAX_DATA_LINES];

    private int width, height;
    private final float[] xPts = new float[MAX_HORZ_POINTS + 1];
    private float xSampFact;
    private int xSize, xRange, xOffs, xMaxSize;
    private int prevXOffs;
    private int numLinesDraw;
    private int lineEnable;
    private int changed;
    private boolean pinching;
    private final ExecutorService lineRenderSvc;
    private ScaleGestureDetector scaleDetector;

    public DataView(Context context, AttributeSet attrs) {
        super(context, attrs);

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            dataPaint[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            dataPaint[i].setStyle(Paint.Style.STROKE);
            dataPaint[i].setStrokeJoin(Paint.Join.ROUND);
            dataPaint[i].setStrokeCap(Paint.Cap.ROUND);
            dataPaint[i].setStrokeWidth(DATA_LINE_WIDTH);
        }

        lineRenderSvc = Executors.newSingleThreadExecutor();
        scaleDetector = new ScaleGestureDetector(context, this);
    }

    private float touchDnX, lastDistX;
    private int touchDnXOffs;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float distX;
        boolean retVal;

        retVal = scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!pinching) {
                    //Log.i(TAG, "ACTION_DOWN");
                    touchDnX = event.getX();
                    touchDnXOffs = xOffs;
                    retVal = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!pinching) {
                    //Log.i(TAG, "ACTION_MOVE");
                    distX = event.getX() - touchDnX;
                    if ((distX - lastDistX > 8) || (lastDistX - distX > 8)) {
                        lastDistX = distX;
                        xOffs = touchDnXOffs - (int) ((1.5F * distX * xRange) / width);
                        xOffs = (xOffs < 0) ? 0 : xOffs;
                        changed |= CHANGED_XOFF;
                        update();
                        //Log.i(TAG, String.format("xOffs=%d xRange=%d", xOffs, xRange));
                    }
                    retVal = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                //Log.i(TAG, "ACTION_UP");
                pinching = false;
                retVal = true;
                break;

            case MotionEvent.ACTION_CANCEL:
                //Log.i(TAG, "ACTION_CANCEL");
                xOffs = touchDnXOffs;
                changed |= CHANGED_XOFF;
                update();
                retVal = true;
                break;
        }

        return retVal || super.onTouchEvent(event);
    }

    private int pinchBeginXRange, pinchBeginX;
    private float pinchBeginRatio;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float prevSpanX, currSpanX, xScale;
        //Log.i(TAG, "onScale: ");

        prevSpanX = detector.getPreviousSpanX();
        currSpanX = detector.getCurrentSpanX();
        xScale = 1F + ((prevSpanX / currSpanX) - 1) * 0.5F;

        xRange = (int) (pinchBeginXRange * xScale);
        xRange = (xRange < MIN_XRANGE) ? MIN_XRANGE : xRange;
        xRange = (xRange > xSize + MIN_XRANGE) ? (xSize + MIN_XRANGE) : xRange;
        xSampFact = (xRange < MAX_HORZ_POINTS) ? 1F : (xRange / (float) MAX_HORZ_POINTS);
        xOffs = pinchBeginX - (int) (pinchBeginRatio * xRange);
        xOffs = (xOffs < 0) ? 0 : xOffs;
        changed |= (CHANGED_XRANGE | CHANGED_XOFF);
        update();
        //Log.i(TAG, String.format("xOffs=%d xRange=%d xSampFact=%f", xOffs, xRange, xSampFact));
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        //Log.i(TAG, "onScaleBegin: ");
        pinchBeginXRange = xRange;
        pinchBeginRatio = detector.getFocusX() / width;
        pinchBeginX = xOffs + (int) (pinchBeginRatio * xRange);
        pinching = true;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        //Log.i(TAG, "onScaleEnd: ");
        //pinching = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        changed |= (CHANGED_XRANGE | CHANGED_YRANGE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.scale(1, -1);
        canvas.translate(0, -height);

        if (numLinesDraw <= 0) return;
        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) return;
            //check(points[i], numLinesDraw);
            canvas.drawLines(points[i], 0, numLinesDraw << 2, dataPaint[i]);
        }
    }

    private void check(float[] lines, int numLines) {
        int j;
        for (int i = 4; i < numLines; i++) {
            j = i << 2;
            if (lines[j] == 0 || lines[j + 2] == 0) {
                Log.i(TAG, "!!!" + i);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //Log.i(TAG, "onDetachedFromWindow: DataView");
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

    public void setYMinMax(int lineNum, float yMin, float yMax) {
        this.yMin[lineNum] = yMin;
        this.yMax[lineNum] = yMax;
        yRange[lineNum] = this.yMax[lineNum] - this.yMin[lineNum];
        changed |= CHANGED_YRANGE;
    }

    public void setYAbsMinMax(int lineNum, float yAbsMin, float yAbsMax) {
        this.yAbsMin[lineNum] = yAbsMin;
        this.yAbsMax[lineNum] = yAbsMax;
    }

    public void setXRange(int xRange) {
        xRange = (xRange < MIN_XRANGE) ? MIN_XRANGE : xRange;
        this.xRange = xRange;
        changed |= CHANGED_XRANGE;
        xSampFact = (xRange < MAX_HORZ_POINTS) ? 1F : (xRange / (float) MAX_HORZ_POINTS);
    }

    public void setXOffs(int xOffs) {
        this.xOffs = (xOffs < 0) ? 0 : xOffs;
    }

    public void setXSize(int xSize) {
        this.xSize = xSize;
    }

    public void setXMaxSize(int xMaxSize) {
        this.xMaxSize = xMaxSize;
    }

    public void setYVals(int lineNum, float[] yVals) {
        this.yVals[lineNum] = yVals;
    }

    private class LineRenderer implements Runnable {
        private final int xOffs, xRange, xSize, changed;

        public LineRenderer(int xOffs, int xRange, int xSize, int changed) {
            this.xOffs = xOffs;
            this.xRange = xRange;
            this.xSize = xSize;
            this.changed = changed;
        }

        @Override
        public void run() {
            int j, numPts;
            float x, y;

            numPts = (int) ((xSize - xOffs) / xSampFact) - 1;
            numPts = (numPts > xRange) ? xRange : numPts;
            numPts = (numPts >= MAX_HORZ_POINTS) ? MAX_HORZ_POINTS - 1 : numPts;

            for (int lineNum = 0; lineNum < MAX_DATA_LINES; lineNum++) {
                if (((1 << lineNum) & lineEnable) == 0) break;

                if ((changed & CHANGED_XRANGE) != 0) calcXPts();

                if (((changed & ~CHANGED_XOFF) == 0) && (xOffs == prevXOffs + 1)) {
                    /* shift left all prev lines */
                    int xRangeMT = ((xRange < MAX_HORZ_POINTS) ? xRange : MAX_HORZ_POINTS) - 2;
                    j = 0;
                    for (int i = 0; i < xRangeMT; i++) {
                        j = i << 2;
                        points[lineNum][j + 1] = points[lineNum][j + 5];
                        points[lineNum][j + 3] = points[lineNum][j + 7];
                    }

                    /* add new, last line */
                    j += 4;
                    points[lineNum][j] = points[lineNum][j];
                    points[lineNum][j + 1] = points[lineNum][j + 3];
                    points[lineNum][j + 2] = points[lineNum][j + 2];
                    points[lineNum][j + 3] = calcY(lineNum, xOffs + xRangeMT + 1);
                } else {
                    points[lineNum][0] = 0;
                    points[lineNum][1] = calcY(lineNum, xOffs);
                    j = 2;
                    for (int i = 1; i <= numPts; i++) {
                        x = xPts[i];
                        y = calcY(lineNum, xOffs + (int) (i * xSampFact));
                        points[lineNum][j++] = x;
                        points[lineNum][j++] = y;
                        points[lineNum][j++] = x;
                        points[lineNum][j++] = y;
                    }
                }
            }

            numLinesDraw = (numPts < 1) ? 0 : numPts - 1;
            Log.i(TAG, "numLinesDraw=" + numLinesDraw);
            prevXOffs = xOffs;
            postInvalidate();
        }

        private float calcY(int lineNum, int idx) {
            if (idx >= xMaxSize) idx = xMaxSize - 1;
            float val = yVals[lineNum][idx];
            if (val > yMax[lineNum])
                return height;
            else if (val < yMin[lineNum])
                return 0;
            else
                return (((val - yMin[lineNum]) / yRange[lineNum]) * height);
        }
    }

    public synchronized void update() {
        lineRenderSvc.execute(new LineRenderer(xOffs, xRange, xSize, changed));
        changed = 0;
    }

    public void incUpdate() {
        xSize++;
        xOffs = xSize - xRange;
        xOffs = (xOffs < 0) ? 0 : xOffs;

        update();
    }

    private void calcXPts() {
        int range = (xRange < MAX_HORZ_POINTS) ? xRange : MAX_HORZ_POINTS;
        float xRangeMO = range - 1;
        for (int i = 0; i < range; i++) {
            xPts[i] = (i * width) / xRangeMO;
        }
    }

}
