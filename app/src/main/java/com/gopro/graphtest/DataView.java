package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataView extends View implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "~DataView";

    private static final int MAX_HORZ_POINTS = 400;
    private static final int MIN_XRANGE = 200;
    private static final float MIN_YRANGE = 0.1F;
    private static final int MAX_DATA_LINES = ChartView.MAX_DATA_LINES;
    private static final float DATA_LINE_WIDTH = 4F;

    private static final int CHANGED_XOFF = (1 << 0);
    private static final int CHANGED_XRANGE = (1 << 1);
    private static final int CHANGED_YRANGE = (1 << 2);

    private final Paint[] dataPaint = new Paint[MAX_DATA_LINES];
    private final float[][] yVals = new float[MAX_DATA_LINES][];
    private final float[][] points = new float[MAX_DATA_LINES][MAX_HORZ_POINTS * 4 + 8];
    private final float[] yRange = new float[MAX_DATA_LINES];
    private final float[] yMin = new float[MAX_DATA_LINES];
    private final float[] yMax = new float[MAX_DATA_LINES];
    private final float[] yAbsMin = new float[MAX_DATA_LINES];
    private final float[] yAbsMax = new float[MAX_DATA_LINES];
    private final float[] xPts = new float[MAX_HORZ_POINTS + 1];

    private int width, height;
    private float xSampFact;
    private int xSize, xRange, xOffs, xMaxSize;
    private int prevXOffs;
    private int numLinesDraw;
    private int lineEnable;
    private int changed;
    private boolean pinching;
    private ExecutorService lineRenderSvc;
    private ScaleGestureDetector scaleDetector;
    private ChartView chartView;

    public DataView(Context context) {
        super(context);
        init(context);
    }

    public DataView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
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

    private float touchDnX, touchDnY;
    private final float[] touchDnYMin = new float[MAX_DATA_LINES];
    private int touchDnXOffs;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float distX, distY;
        boolean retVal;

        retVal = scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!pinching) {
                    //Log.i(TAG, "ACTION_DOWN");
                    touchDnX = event.getX();
                    touchDnXOffs = xOffs;
                    touchDnY = event.getY();
                    System.arraycopy(yMin, 0, touchDnYMin, 0, MAX_DATA_LINES);
                    retVal = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!pinching) {
                    //Log.i(TAG, "ACTION_MOVE");

                    distX = event.getX() - touchDnX;
                    xOffs = touchDnXOffs - (int) ((1.5F * distX * xRange) / width);
                    xOffs = (xOffs < 0) ? 0 : xOffs;
                    changed |= CHANGED_XOFF;
                    //Log.i(TAG, String.format("xOffs=%d xRange=%d", xOffs, xRange));

                    distY = event.getY() - touchDnY;
                    for (int i = 0; i < MAX_DATA_LINES; i++) {
                        yMin[i] = touchDnYMin[i] + ((distY * yRange[i]) / height);
                        yMax[i] = yMin[i] + yRange[i];
                        if (yMin[i] < yAbsMin[i]) {
                            yMin[i] = yAbsMin[i];
                            yMax[i] = yMin[i] + yRange[i];
                        } else if (yMax[i] > yAbsMax[i]) {
                            yMax[i] = yAbsMax[i];
                            yMin[i] = yMax[i] - yRange[i];
                        }
                    }
                    changed |= CHANGED_YRANGE;

                    update();
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
                retVal = true;
                break;
        }

        return retVal || super.onTouchEvent(event);
    }

    private int pinchBeginXRange, pinchBeginX;
    private float pinchBeginXRatio;
    private final float[] pinchBeginYRange = new float[MAX_DATA_LINES];
    private final float[] pinchBeginY = new float[MAX_DATA_LINES];
    private boolean pinchingX;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float prevSpanX, currSpanX, xScale;
        float prevSpanY, currSpanY, yScale;
        //Log.i(TAG, "onScale: ");

        prevSpanX = detector.getPreviousSpanX();
        currSpanX = detector.getCurrentSpanX();
        xScale = 1F + ((prevSpanX / currSpanX) - 1) * 0.5F;

        prevSpanY = detector.getPreviousSpanY();
        currSpanY = detector.getCurrentSpanY();
        yScale = 1F + ((prevSpanY / currSpanY) - 1) * 0.5F;

        if (pinchingX) {
            /* Pinch-X */
            xRange = (int) (pinchBeginXRange * xScale);
            xRange = (xRange < MIN_XRANGE) ? MIN_XRANGE : xRange;
            xRange = (xRange > xSize + MIN_XRANGE) ? (xSize + MIN_XRANGE) : xRange;
            xSampFact = (xRange < MAX_HORZ_POINTS) ? 1F : (xRange / (float) MAX_HORZ_POINTS);
            xOffs = pinchBeginX - (int) (pinchBeginXRatio * xRange);
            xOffs = (xOffs < 0) ? 0 : xOffs;
            changed |= (CHANGED_XRANGE | CHANGED_XOFF);
            //Log.i(TAG, String.format("xOffs=%d xRange=%d xSampFact=%f", xOffs, xRange, xSampFact));
        } else {
            /* Pinch-Y */
            for (int i = 0; i < MAX_DATA_LINES; i++) {
                yRange[i] = (pinchBeginYRange[i] * yScale);
                if ((yRange[i] / (yAbsMax[i] - yAbsMin[i])) < MIN_YRANGE)
                    yRange[i] = MIN_YRANGE * (yAbsMax[i] - yAbsMin[i]);
                if (yRange[i] > (yAbsMax[i] - yAbsMin[i]))
                    yRange[i] = yAbsMax[i] - yAbsMin[i];

                yMin[i] = pinchBeginY[i] - yRange[i] * 0.5F;
                yMax[i] = pinchBeginY[i] + yRange[i] * 0.5F;
                changed |= CHANGED_YRANGE;
            }
        }

        update();
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        //Log.i(TAG, "onScaleBegin: ");
        pinchBeginXRange = xRange;
        pinchBeginXRatio = detector.getFocusX() / width;
        pinchBeginX = xOffs + (int) (pinchBeginXRatio * xRange);

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            pinchBeginYRange[i] = yRange[i];
            pinchBeginY[i] = (yAbsMax[i] + yAbsMin[i]) * 0.5F;
        }

        pinchingX = (detector.getCurrentSpanX() > detector.getCurrentSpanY());
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
            canvas.drawLines(points[i], 0, numLinesDraw << 2, dataPaint[i]);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //Log.i(TAG, "onDetachedFromWindow: DataView");
        lineRenderSvc.shutdown();
    }

    public void setChartView(ChartView chartView) {
        this.chartView = chartView;
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
        changed |= CHANGED_XOFF;
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
            //Log.i(TAG, "numLinesDraw=" + numLinesDraw);
            prevXOffs = xOffs;
            postInvalidate();
        }

        private float calcY(int lineNum, int idx) {
            if (idx >= xMaxSize) idx = xMaxSize - 1;
            float val = yVals[lineNum][idx];
            if (val > yMax[lineNum])
                return height - 1;
            else if (val < yMin[lineNum])
                return 1;
            else
                return (((val - yMin[lineNum]) / yRange[lineNum]) * height);
        }
    }

    public synchronized void update() {
        lineRenderSvc.execute(new LineRenderer(xOffs, xRange, xSize, changed));
        if ((changed & CHANGED_XOFF) != 0) {
            chartView.updateXOffs(xOffs);
        }
        if ((changed & CHANGED_XRANGE) != 0) {
            chartView.updateXRange(xRange);
        }
        if ((changed & CHANGED_YRANGE) != 0) {
            chartView.updateYMinMaxRange(yMin, yMax, yRange);
        }
        changed = 0;
    }

    public void incUpdate() {
        xSize++;
        xOffs = xSize - xRange;
        xOffs = (xOffs < 0) ? 0 : xOffs;
        chartView.updateXOffs(xOffs);

        lineRenderSvc.execute(new LineRenderer(xOffs, xRange, xSize, changed));
        changed = 0;
    }

    private void calcXPts() {
        int range = (xRange < MAX_HORZ_POINTS) ? xRange : MAX_HORZ_POINTS;
        float xRangeMO = range - 1;
        for (int i = 0; i < range; i++) {
            xPts[i] = (i * width) / xRangeMO;
        }
    }

}
