package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;

public class ChartView extends View {

    public static final int MAX_DATA_LINES = 3;

    private static final String TAG = "~CanvasView";

    private static final int NUM_VERT_DIVISIONS = 6;
    private static final int NUM_HORZ_DIVISIONS = 8;
    private static final int DIV_TEXT_SIZE = 16;

    private int width, height;
    private final Paint divisionPaint;
    private final Paint divTextPaintL, divTextPaintR;
    private final float[] yMin = new float[MAX_DATA_LINES];
    private final float[] yMax = new float[MAX_DATA_LINES];
    private final float[] yRange = new float[MAX_DATA_LINES];
    private int xRange, xOffs;
    private float xDispScale;
    private final DisplayMetrics displayMetrics;
    private final int dvXOffsL, dvXOffsR;
    private final int dvYOffs;
    private int dvWidth, dvHeight;
    private final int marginOffs;
    private final Rect rect = new Rect();
    private final DecimalFormat yValFormat = new DecimalFormat("0.0E0");
    private final DecimalFormat xValFormat = new DecimalFormat("0.00E0");
    private DataView dataView;

    private int leftLineNum, rightLineNum;

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        displayMetrics = context.getResources().getDisplayMetrics();
        dvXOffsL = dpToPx(60);
        dvXOffsR = dpToPx(60);
        dvYOffs = dpToPx(40);
        marginOffs = dpToPx(6);

        divisionPaint = new Paint();
        divisionPaint.setAntiAlias(false);
        divisionPaint.setColor(0xFFB0B0B0);
        divisionPaint.setStyle(Paint.Style.STROKE);
        divisionPaint.setStrokeJoin(Paint.Join.MITER);
        divisionPaint.setStrokeCap(Paint.Cap.BUTT);
        divisionPaint.setStrokeWidth(0F);

        divTextPaintL = new Paint(Paint.ANTI_ALIAS_FLAG);
        divTextPaintL.setColor(0xFF808080);
        divTextPaintL.setTextSize(dpToPx(DIV_TEXT_SIZE));

        divTextPaintR = new Paint(Paint.ANTI_ALIAS_FLAG);
        divTextPaintR.setTextAlign(Paint.Align.RIGHT);
        divTextPaintR.setColor(0xFF808080);
        divTextPaintR.setTextSize(dpToPx(DIV_TEXT_SIZE));

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        dvWidth = width - dvXOffsL - dvXOffsR;
        dvHeight = height - dvYOffs;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, width, height, divisionPaint);

        drawDivisions(canvas);
        drawHorzDivText(canvas);
        drawVertDivTextL(canvas);
        drawVertDivTextR(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return dataView.onTouchEvent(event);
    }

    public void enableLine(int lineNum, boolean enable) {
        dataView.enableLine(lineNum, enable);
    }

    public void setLineColor(int lineNum, int color) {
        dataView.setLineColor(lineNum, color);
    }

    public void setDataView(DataView dataView) {
        dataView.setChartView(this);
        this.dataView = dataView;
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
    }

    public void setXMaxSize(int xMaxSize) {
        dataView.setXMaxSize(xMaxSize);
    }

    public void setYVals(int lineNum, float[] yVals) {
        dataView.setYVals(lineNum, yVals);
    }

    public void setXDispScale(float xDispScale) {
        this.xDispScale = xDispScale;
    }

    public void update() {
        dataView.update();
    }

    public void incUpdate() {
        dataView.incUpdate();
    }

    public void setLeftRight(int leftLineNum, int rightLineNum) {
        this.leftLineNum = leftLineNum;
        this.rightLineNum = rightLineNum;
    }

    private void drawDivisions(Canvas canvas) {
        float pos;

        canvas.drawRect(dvXOffsL, 0, width - dvXOffsR, height - dvYOffs, divisionPaint);
        for (int i = 1; i < NUM_HORZ_DIVISIONS; i++) {
            pos = (i * dvWidth) / NUM_HORZ_DIVISIONS;
            canvas.drawLine(pos + dvXOffsL, 0, pos + dvXOffsL, dvHeight, divisionPaint);
        }
        for (int i = 1; i < NUM_VERT_DIVISIONS; i++) {
            pos = (i * dvHeight) / NUM_VERT_DIVISIONS;
            canvas.drawLine(dvXOffsL, pos, width - dvXOffsR, pos, divisionPaint);
        }
    }

    public void updateXRange(int xRange) {
        this.xRange = xRange;
        postInvalidate();
    }

    public void updateXOffs(int xOffs) {
        this.xOffs = (xOffs < 0) ? 0 : xOffs;
        postInvalidate();
    }

    public void updateYMinMaxRange(float[] yMin, float[] yMax, float[] yRange) {
        System.arraycopy(yMin, 0, this.yMin, 0, MAX_DATA_LINES);
        System.arraycopy(yMax, 0, this.yMax, 0, MAX_DATA_LINES);
        System.arraycopy(yRange, 0, this.yRange, 0, MAX_DATA_LINES);
        postInvalidate();
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

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

}
