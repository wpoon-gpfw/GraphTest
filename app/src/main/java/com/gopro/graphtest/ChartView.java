package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.text.DecimalFormat;

public class ChartView extends View {

    public static final int MAX_DATA_LINES = 3;

    private static final String TAG = "~CanvasView";

    private static final int MAX_VERT_DIVISIONS = 8;
    private static final int MAX_HORZ_DIVISIONS = 8;
    private static final int MAX_DIV_LINES = MAX_VERT_DIVISIONS + MAX_HORZ_DIVISIONS + 2;

    private static int NUM_VERT_DIVISIONS = 5;
    private static int NUM_HORZ_DIVISIONS = 5;
    private static int NUM_DIV_LINES = NUM_HORZ_DIVISIONS + NUM_VERT_DIVISIONS + 2;

    private static final int CHARTNAME_TEXT_SIZE = 14;
    private static final int DIV_TEXT_SIZE = 12;
    private static final int MARGIN_YAXIS_DP = 45;
    private static final int MARGIN_BOTTOM_DP = 20;
    private static final int MARGIN_DP = 5;
    private static final int MARGIN_CHARTNAME_DP = 10;
    private static final int LEGEND_MARKER_RADIUS_DP = 4;
    private static final int LEGEND_MARGIN_LEFT_DP = 18;
    private static final int LEGEND_MARGIN_RIGHT_DP = 8;
    private static final int YVAL_SPACING_DP = 55;
    private static final int YVAL_VERT_MARGIN_DP = 15;
    private static final int YVAL_STACK_VERT_DP = 280;

    private static final int CHANGED_GRID = (1 << 0);
    private static final int CHANGED_YLABELS = (1 << 1);
    private static final int CHANGED_XLABELS = (1 << 2);
    private static final int CHANGED_LEGEND = (1 << 3);
    private static final int CHANGED_YVALS = (1 << 4);

    //private static final int REFRESH_INTERVAL = 100;

    private final Paint chartNamePaintFG, chartNamePaintBG;
    private final Paint divisionPaint;
    private final Paint divTextPaintL, divTextPaintR;
    private final Paint markerPaint;
    private final Paint yValTextPaint;
    private final float[][] yVals = new float[MAX_DATA_LINES][];
    private final float[] yMin = new float[MAX_DATA_LINES];
    private final float[] yMax = new float[MAX_DATA_LINES];
    private final float[] yRange = new float[MAX_DATA_LINES];
    private final String[] lineLabels = new String[MAX_DATA_LINES];
    private final float[] lineLabelsWidth = new float[MAX_DATA_LINES];
    private final int[] lineColors = new int[MAX_DATA_LINES];
    private final float[] divisionPts = new float[MAX_DIV_LINES * 4];
    private final DisplayMetrics displayMetrics;
    private final int marginChartName;
    private final int marginOffs;
    private final int markerRadius, marginLegendL, marginLegendR;
    private final int yValSpacing, yValVertMargin;
    private final int dvYOffs;
    private String chartName;
    private int width, height;
    private int lineEnable;
    private int xSize, xRange, xOffs, xRightMost;
    private float xDispScale;
    private int dvXOffsL, dvXOffsR;
    private int dvWidth, dvHeight;
    private float chartNameHeight;
    private final Rect chartNameRect = new Rect();
    private final Rect rect = new Rect();
    private final RectF chartNameRectF = new RectF();
    private final DecimalFormat yValFormat = new DecimalFormat("0.0E0");
    private final DecimalFormat xValFormat = new DecimalFormat("0.00E0");
    private final DataView dataView;
    private FrameLayout.LayoutParams dataViewLayParams;
    private int leftLineNum, rightLineNum;
    private int changed;
    private boolean yValStackVert;
    //private RefreshThread refreshThread;

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        displayMetrics = context.getResources().getDisplayMetrics();
        dvYOffs = dpToPx(MARGIN_BOTTOM_DP);
        marginOffs = dpToPx(MARGIN_DP);

        marginChartName = dpToPx(MARGIN_CHARTNAME_DP);

        markerRadius = dpToPx(LEGEND_MARKER_RADIUS_DP);
        marginLegendL = dpToPx(LEGEND_MARGIN_LEFT_DP);
        marginLegendR = dpToPx(LEGEND_MARGIN_RIGHT_DP);

        yValSpacing = dpToPx(YVAL_SPACING_DP);
        yValVertMargin = dpToPx(YVAL_VERT_MARGIN_DP);

        dataView = new DataView(context);
        dataView.setChartView(this);

        chartNamePaintFG = new Paint(Paint.ANTI_ALIAS_FLAG);
        chartNamePaintFG.setColor(0xFF000000);
        chartNamePaintFG.setTextAlign(Paint.Align.RIGHT);
        chartNamePaintFG.setTextSize(dpToPx(CHARTNAME_TEXT_SIZE));

        chartNamePaintBG = new Paint();
        chartNamePaintBG.setColor(0xFFE0E0E0);
        chartNamePaintBG.setStyle(Paint.Style.FILL_AND_STROKE);

        divisionPaint = new Paint();
        divisionPaint.setAntiAlias(false);
        divisionPaint.setColor(0xFFB0B0B0);
        divisionPaint.setStyle(Paint.Style.STROKE);
        divisionPaint.setStrokeWidth(0F);

        divTextPaintL = new Paint(Paint.ANTI_ALIAS_FLAG);
        divTextPaintL.setColor(0xFF707070);
        divTextPaintL.setTextSize(dpToPx(DIV_TEXT_SIZE));

        divTextPaintR = new Paint(Paint.ANTI_ALIAS_FLAG);
        divTextPaintR.setColor(0xFF707070);
        divTextPaintR.setTextAlign(Paint.Align.RIGHT);
        divTextPaintR.setTextSize(dpToPx(DIV_TEXT_SIZE));

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        yValTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yValTextPaint.setTextSize(dpToPx(DIV_TEXT_SIZE));
    }

    @Override
    protected void onAttachedToWindow() {
        Log.i(TAG, "onAttachedToWindow: RAN!");
        super.onAttachedToWindow();

        //refreshThread = new RefreshThread(REFRESH_INTERVAL);
        //refreshThread.start();

        dataViewLayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        dataViewLayParams.leftMargin = dvXOffsL;
        dataViewLayParams.rightMargin = dvXOffsR;
        dataViewLayParams.bottomMargin = dvYOffs;

        FrameLayout parent = (FrameLayout) getParent();
        parent.addView(dataView, dataViewLayParams);
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.i(TAG, "onDetachedFromWindow: RAN!");
        super.onDetachedFromWindow();

        //refreshThread.interrupt();
        //refreshThread = null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "W = " + pxToDp(w) + " H = " + pxToDp(h));

        NUM_HORZ_DIVISIONS = pxToDp(w) / 80;
        if (NUM_HORZ_DIVISIONS > MAX_HORZ_DIVISIONS) NUM_HORZ_DIVISIONS = MAX_HORZ_DIVISIONS;
        NUM_VERT_DIVISIONS = pxToDp(h) / 60;
        if (NUM_VERT_DIVISIONS > MAX_VERT_DIVISIONS) NUM_VERT_DIVISIONS = MAX_VERT_DIVISIONS;
        Log.i(TAG, "HD = " + NUM_HORZ_DIVISIONS + " VD = " + NUM_VERT_DIVISIONS);

        NUM_DIV_LINES = NUM_HORZ_DIVISIONS + NUM_VERT_DIVISIONS + 2;

        width = w;
        height = h;
        dvWidth = width - dvXOffsL - dvXOffsR;
        dvHeight = height - dvYOffs;

        Log.i(TAG, "dvW = " + pxToDp(dvWidth));
        yValStackVert = (pxToDp(dvWidth) < YVAL_STACK_VERT_DP);

        calcChartRect();
        calcDivisions();
        changed |= CHANGED_GRID;
    }

    private void calcDivisions() {
        float pos;
        int j = 0;
        int k;

        for (int i = 0; i <= NUM_HORZ_DIVISIONS; i++) {
            pos = (i * dvWidth) / NUM_HORZ_DIVISIONS;
            j = i << 2;
            divisionPts[j] = pos + dvXOffsL;
            divisionPts[j + 1] = 0;
            divisionPts[j + 2] = pos + dvXOffsL;
            divisionPts[j + 3] = dvHeight;
        }
        k = j + 4;
        for (int i = 0; i <= NUM_VERT_DIVISIONS; i++) {
            pos = (i * dvHeight) / NUM_VERT_DIVISIONS;
            j = (i << 2) + k;
            divisionPts[j] = dvXOffsL;
            divisionPts[j + 1] = pos;
            divisionPts[j + 2] = width - dvXOffsR;
            divisionPts[j + 3] = pos;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawRect(0, 0, width, height, divisionPaint);

        /* Draw Graph Grid and Chart Name */
        if ((changed & CHANGED_GRID) != 0) {
            canvas.drawLines(divisionPts, 0, NUM_DIV_LINES << 2, divisionPaint);
            drawChartName(canvas);
        }

        if ((changed & CHANGED_XLABELS) != 0)
            drawHorzDivText(canvas);

        if ((changed & CHANGED_YLABELS) != 0) {
            if ((dvXOffsL > 0)) drawVertDivTextL(canvas);
            if ((dvXOffsR > 0)) drawVertDivTextR(canvas);
        }

        if ((changed & CHANGED_LEGEND) != 0)
            drawGraphLegend(canvas);

        if ((changed & CHANGED_YVALS) != 0) {
            if (yValStackVert)
                drawLastYValVert(canvas);
            else
                drawLastYValHorz(canvas);

        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return dataView.onTouchEvent(event);
    }

    public void setChartName(String chartName) {
        this.chartName = chartName;
        chartNamePaintFG.getTextBounds(chartName, 0, chartName.length(), chartNameRect);
        chartNameHeight = chartNameRect.height();
        calcChartRect();
    }

    public void enableLine(int lineNum, boolean enable) {
        if (enable)
            this.lineEnable |= (1 << lineNum);
        else
            this.lineEnable &= ~(1 << lineNum);
        dataView.enableLine(lineEnable);
    }

    public void setLineLabel(int lineNum, String label) {
        lineLabels[lineNum] = label;
        divTextPaintL.getTextBounds(label, 0, label.length(), rect);
        lineLabelsWidth[lineNum] = rect.width();
        changed |= CHANGED_LEGEND;
    }

    public void setLineColor(int lineNum, int color) {
        dataView.setLineColor(lineNum, color);
        lineColors[lineNum] = color;
        changed |= CHANGED_LEGEND;
    }

    public void setYMinMax(int lineNum, float yMin, float yMax) {
        dataView.setYMinMax(lineNum, yMin, yMax);
        this.yMin[lineNum] = yMin;
        this.yMax[lineNum] = yMax;
        yRange[lineNum] = yMax - yMin;
    }

    public void setYAbsMinMax(int lineNum, float yAbsMin, float yAbsMax) {
        dataView.setYAbsMinMax(lineNum, yAbsMin, yAbsMax);
    }

    public void setXRange(int xRange) {
        dataView.setXRange(xRange);
        this.xRange = xRange;
    }

    public void setXOffs(int xOffs) {
        dataView.setXOffs(xOffs);
        this.xOffs = (xOffs < 0) ? 0 : xOffs;
    }

    public void setXSize(int xSize) {
        dataView.setXSize(xSize);
        this.xSize = xSize;
    }

    public void setXMaxSize(int xMaxSize) {
        dataView.setXMaxSize(xMaxSize);
    }

    public void setYVals(int lineNum, float[] yVals) {
        dataView.setYVals(lineNum, yVals);
        this.yVals[lineNum] = yVals;
    }

    public void setXDispScale(float xDispScale) {
        this.xDispScale = xDispScale;
    }

    public void update() {
        dataView.update();
        changed = 0xFF;
        chartInvalidate();
    }

    public void incUpdate() {
        dataView.incUpdate();
        xSize++;
    }

    public void setLeftRight(int leftLineNum, int rightLineNum) {
        this.leftLineNum = leftLineNum;
        this.rightLineNum = rightLineNum;
        dvXOffsL = (leftLineNum < 0) ? 0 : dpToPx(MARGIN_YAXIS_DP);
        dvXOffsR = (rightLineNum < 0) ? 0 : dpToPx(MARGIN_YAXIS_DP);
        if (dataViewLayParams != null) {
            dataViewLayParams.leftMargin = dvXOffsL;
            dataViewLayParams.rightMargin = dvXOffsR;
        }

        calcChartRect();
        calcDivisions();
        changed |= CHANGED_GRID | CHANGED_YVALS | CHANGED_YLABELS;
    }

    public void updateXRangeXOffs(int xRange, int xOffs) {
        this.xRange = xRange;
        this.xOffs = xOffs;
        xRightMost = xOffs + xRange;
        changed |= CHANGED_XLABELS | CHANGED_YVALS;
        chartInvalidate();
    }

    public void updateXOffs(int xOffs) {
        this.xOffs = xOffs;
        xRightMost = xOffs + xRange;
        changed |= CHANGED_XLABELS | CHANGED_YVALS;
        chartInvalidate();
    }

    public void updateYMinMaxRange(float[] yMin, float[] yMax, float[] yRange) {
        System.arraycopy(yMin, 0, this.yMin, 0, MAX_DATA_LINES);
        System.arraycopy(yMax, 0, this.yMax, 0, MAX_DATA_LINES);
        System.arraycopy(yRange, 0, this.yRange, 0, MAX_DATA_LINES);
        changed |= CHANGED_YLABELS;
        chartInvalidate();
    }

    private void drawChartName(Canvas canvas) {
        canvas.drawRoundRect(chartNameRectF,
                marginOffs,
                marginOffs,
                chartNamePaintBG);

        canvas.drawText(chartName,
                width - dvXOffsR - marginChartName,
                chartNameHeight + marginChartName,
                chartNamePaintFG);
    }

    private void calcChartRect() {
        chartNameRectF.left = width - dvXOffsR - chartNameRect.right - marginChartName - marginOffs;
        chartNameRectF.top = -chartNameRect.bottom + marginChartName - marginOffs;
        chartNameRectF.right = width - dvXOffsR - chartNameRect.left - marginChartName + marginOffs;
        chartNameRectF.bottom = -chartNameRect.top + marginChartName + marginOffs;
    }

    private void drawHorzDivText(Canvas canvas) {
        float pos;
        String text;

        /* Leftmost label */
        text = getHorzDivString(xOffs);
        divTextPaintL.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                dvXOffsL > 0 ? dvXOffsL - rect.width() / 2 : marginOffs,
                dvHeight + rect.height() + marginOffs,
                divTextPaintL);

        for (int i = 1; i < NUM_HORZ_DIVISIONS; i++) {
            text = getHorzDivString(xOffs + (i * xRange) / NUM_HORZ_DIVISIONS);
            pos = (i * dvWidth) / NUM_HORZ_DIVISIONS;
            divTextPaintL.getTextBounds(text, 0, text.length(), rect);
            canvas.drawText(text,
                    dvXOffsL + pos - rect.width() / 2,
                    dvHeight + rect.height() + marginOffs,
                    divTextPaintL);
        }

        /* Rightmost label */
        text = getHorzDivString(xOffs + xRange);
        divTextPaintL.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                dvXOffsR > 0 ? width - dvXOffsR - rect.width() / 2 : width - rect.width() - marginOffs,
                dvHeight + rect.height() + marginOffs,
                divTextPaintL);
    }

    private String getHorzDivString(float value) {
        return xValFormat.format(value * xDispScale);
    }

    private void drawVertDivTextL(Canvas canvas) {
        float pos;
        String text;

        /* Bottommost label */
        text = yValFormat.format(yMin[leftLineNum]);
        canvas.drawText(text,
                marginOffs,
                height - dvYOffs - marginOffs,
                divTextPaintL);

        for (int i = 1; i < NUM_VERT_DIVISIONS; i++) {
            text = yValFormat.format(yMin[leftLineNum] + (i * yRange[leftLineNum]) / NUM_VERT_DIVISIONS);
            pos = (i * dvHeight) / NUM_VERT_DIVISIONS;
            divTextPaintL.getTextBounds(text, 0, text.length(), rect);
            canvas.drawText(text,
                    marginOffs,
                    height - dvYOffs - pos + rect.height() / 2,
                    divTextPaintL);
        }

        /* Topmost label */
        text = yValFormat.format(yMax[leftLineNum]);
        divTextPaintL.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                marginOffs,
                rect.height() + marginOffs,
                divTextPaintL);
    }

    private void drawVertDivTextR(Canvas canvas) {
        float pos;
        String text;

        /* Bottommost label */
        text = yValFormat.format(yMin[rightLineNum]);
        canvas.drawText(text,
                width - marginOffs,
                height - dvYOffs - marginOffs,
                divTextPaintR);

        for (int i = 1; i < NUM_VERT_DIVISIONS; i++) {
            text = yValFormat.format(yMin[rightLineNum] + (i * yRange[rightLineNum]) / NUM_VERT_DIVISIONS);
            pos = (i * dvHeight) / NUM_VERT_DIVISIONS;
            divTextPaintL.getTextBounds(text, 0, text.length(), rect);
            canvas.drawText(text,
                    width - marginOffs,
                    height - dvYOffs - pos + rect.height() / 2,
                    divTextPaintR);
        }

        /* Topmost label */
        text = yValFormat.format(yMax[rightLineNum]);
        divTextPaintL.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                width - marginOffs,
                rect.height() + marginOffs,
                divTextPaintR);
    }

    private void drawGraphLegend(Canvas canvas) {
        float posX = dvXOffsL;
        String text;

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) continue;

            markerPaint.setColor(lineColors[i]);
            canvas.drawCircle(
                    posX + marginOffs + markerRadius,
                    dvHeight - marginOffs - markerRadius,
                    markerRadius,
                    markerPaint);

            posX += marginLegendL;

            text = lineLabels[i];
            canvas.drawText(text,
                    posX,
                    dvHeight - marginOffs,
                    divTextPaintL);

            posX += lineLabelsWidth[i] + marginLegendR;
        }
    }

    private void drawLastYValHorz(Canvas canvas) {
        String text;

        float posX = dvXOffsL + marginLegendR;
        int rightMost = (xRightMost > xSize) ? xSize : xRightMost;

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) continue;

            yValTextPaint.setColor(lineColors[i]);

            if (rightMost < 1)
                text = "[ - ]";
            else
                text = "[ " + yValFormat.format(yVals[i][rightMost - 1]) + " ]";

            canvas.drawText(text,
                    posX,
                    yValVertMargin,
                    yValTextPaint);
            //noinspection SuspiciousNameCombination
            posX += yValSpacing;
        }
    }

    private void drawLastYValVert(Canvas canvas) {
        String text;

        float posY = yValVertMargin;
        int rightMost = (xRightMost > xSize) ? xSize : xRightMost;

        for (int i = 0; i < MAX_DATA_LINES; i++) {
            if (((1 << i) & lineEnable) == 0) continue;

            yValTextPaint.setColor(lineColors[i]);

            if (rightMost < 1)
                text = "[ - ]";
            else
                text = "[ " + yValFormat.format(yVals[i][rightMost - 1]) + " ]";

            canvas.drawText(text,
                    dvXOffsL + marginLegendR,
                    posY,
                    yValTextPaint);
            //noinspection SuspiciousNameCombination
            posY += dvYOffs;
        }
    }

    private void chartInvalidate() {
        //if (refreshThread != null) refreshThread.refresh();
        postInvalidate();
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    private int pxToDp(int px) {
        return ((160 * px) / displayMetrics.densityDpi);
    }

    private class RefreshThread extends Thread {
        private final int interval;

        public RefreshThread(int refreshInterval) {
            interval = refreshInterval;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.i(TAG, "feedThread: Bye!");
                        return;
                    }

                }
                postInvalidate();
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    Log.i(TAG, "feedThread: Adios!");
                    return;
                }
            }
        }

        public synchronized void refresh() {
            notify();
        }
    }

}
