package com.gopro.graphtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.text.DecimalFormat;

public class ChartView extends View {

    private static final String TAG = "~CanvasView";

    private static final int NUM_VERT_DIVISIONS = 6;
    private static final int NUM_HORZ_DIVISIONS = 8;
    private static final int DIV_TEXT_SIZE = 16;


    private int width, height;
    private Paint divisionPaint, divTextPaint;
    private float yMin, yMax, yRange;
    private int xRange, xOffs;
    private float xDispScale;
    private DisplayMetrics displayMetrics;
    private int dvXOffs, dvYOffs;
    private int dvWidth, dvHeight;
    private int marginOffs;
    private Rect rect = new Rect();
    private final DecimalFormat yValFormat = new DecimalFormat("0.0E0");
    private final DecimalFormat xValFormat = new DecimalFormat("0.00E0");

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        displayMetrics = context.getResources().getDisplayMetrics();
        dvXOffs = dpToPx(60);
        dvYOffs = dpToPx(40);
        marginOffs = dpToPx(5);

        divisionPaint = new Paint();
        divisionPaint.setAntiAlias(false);
        divisionPaint.setColor(0xFFB0B0B0);
        divisionPaint.setStyle(Paint.Style.STROKE);
        divisionPaint.setStrokeJoin(Paint.Join.MITER);
        divisionPaint.setStrokeCap(Paint.Cap.BUTT);
        divisionPaint.setStrokeWidth(0F);

        divTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        divTextPaint.setColor(0xFF808080);
        divTextPaint.setTextSize(dpToPx(DIV_TEXT_SIZE));

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
    }

    public void setXOffs(int xOffs) {
        this.xOffs = (xOffs < 0) ? 0 : xOffs;
    }

    public void setXDispScale(float xDispScale) {
        this.xDispScale = xDispScale;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        dvWidth = width - dvXOffs;
        dvHeight = height - dvYOffs;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, width, height, divisionPaint);

        drawDivisions(canvas);
        drawHorzDivText(canvas);
        drawVertDivText(canvas);
    }

    private void drawDivisions(Canvas canvas) {
        float pos;

        canvas.drawRect(dvXOffs, 0, width, height - dvYOffs, divisionPaint);
        for (int i = 1; i < NUM_HORZ_DIVISIONS; i++) {
            pos = (i * dvWidth) / NUM_HORZ_DIVISIONS;
            canvas.drawLine(pos + dvXOffs, 0, pos + dvXOffs, dvHeight, divisionPaint);
        }
        for (int i = 1; i < NUM_VERT_DIVISIONS; i++) {
            pos = (i * dvHeight) / NUM_VERT_DIVISIONS;
            canvas.drawLine(dvXOffs, pos, width, pos, divisionPaint);
        }
    }

    private void drawHorzDivText(Canvas canvas) {
        float pos;
        String text;

        /* Leftmost label */
        text = getHorzDivString(xOffs);
        divTextPaint.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                dvXOffs + marginOffs,
                dvHeight + rect.height() + marginOffs,
                divTextPaint);

        for (int i = 1; i < NUM_HORZ_DIVISIONS; i++) {
            text = getHorzDivString(xOffs + (i * xRange) / NUM_HORZ_DIVISIONS);
            pos = (i * dvWidth) / NUM_HORZ_DIVISIONS;
            divTextPaint.getTextBounds(text, 0, text.length(), rect);
            canvas.drawText(text,
                    dvXOffs + pos - rect.width() / 2,
                    dvHeight + rect.height() + marginOffs,
                    divTextPaint);
        }

        /* Rightmost label */
        text = getHorzDivString(xOffs + xRange);
        divTextPaint.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                width - marginOffs - rect.width(),
                dvHeight + rect.height() + marginOffs,
                divTextPaint);
    }

    private String getHorzDivString(float value) {
        return xValFormat.format(value * xDispScale);
    }

    private void drawVertDivText(Canvas canvas) {
        float pos;
        String text;

        /* Bottommost label */
        text = yValFormat.format(yMin);
        canvas.drawText(text,
                marginOffs,
                height - dvYOffs - marginOffs,
                divTextPaint);

        for (int i = 1; i < NUM_VERT_DIVISIONS; i++) {
            text = yValFormat.format(yMin + (i * yRange) / NUM_VERT_DIVISIONS);
            pos = (i * dvHeight) / NUM_VERT_DIVISIONS;
            divTextPaint.getTextBounds(text, 0, text.length(), rect);
            canvas.drawText(text,
                    marginOffs,
                    height - dvYOffs - pos + rect.height() / 2,
                    divTextPaint);
        }

        /* Topmost label */
        text = yValFormat.format(yMax);
        divTextPaint.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text,
                marginOffs,
                rect.height() + marginOffs,
                divTextPaint);
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    public void update() {
        postInvalidate();
    }
}
