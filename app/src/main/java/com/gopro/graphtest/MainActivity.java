package com.gopro.graphtest;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final String TAG = "~MainActivity";

    private static final int MAX_SAMPLES = 2000;
    private static final int DISPLAY_WINDOW = 200;
    private static final int SAMPLE_INTERVAL = 40;

    private DataView dataView;
    private ChartView chartView;
    private final float[][] yVals = new float[3][MAX_SAMPLES];
    private int sampleCount = 0;
    private RandomData[] randomData = new RandomData[3];
    private Thread feedThread;
    private final boolean feedThreadPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        randomData[0] = new RandomData(-1.00F, 1.00F, 0.02F, 1.8F, 0.4F, 15);
        randomData[1] = new RandomData(-2.00F, 2.00F, 0.02F, 1.8F, 0.4F, 15);
        randomData[2] = new RandomData(-4.00F, 4.00F, 0.02F, 1.8F, 0.4F, 15);

        //initYVals(1F);

        chartView = (ChartView) findViewById(R.id.chartView);
        chartView.setXRange(DISPLAY_WINDOW);
        chartView.setXOffs(0);
        chartView.setXDispScale(1F / SAMPLE_INTERVAL);
        chartView.setYMin(-1F);
        chartView.setYMax(1F);
        chartView.postDelayed(new Runnable() {
            @Override
            public void run() {
                chartView.invalidate();
            }
        }, 100);

        dataView = (DataView) findViewById(R.id.dataView);
        dataView.setLineColor(0, 0xFFFF0000);
        dataView.setLineColor(1, 0xFF00FF00);
        dataView.setLineColor(2, 0xFF0000FF);
        dataView.enableLine(0, true);
        dataView.enableLine(1, true);
        dataView.enableLine(2, true);
        dataView.setYVals(0, yVals[0]);
        dataView.setYVals(1, yVals[1]);
        dataView.setYVals(2, yVals[2]);
        dataView.setYMinMax(0, -1F, 1F);
        dataView.setYMinMax(1, -2F, 2F);
        dataView.setYMinMax(2, -4F, 4F);
        dataView.setXRange(DISPLAY_WINDOW);
        dataView.setXOffs(0);
        dataView.setXSize(0);

        feedThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    do {
                        try {
                            Thread.sleep(SAMPLE_INTERVAL);
                        } catch (InterruptedException e) {
                            return;
                        }
                    } while (feedThreadPaused);

                    addNewSample(
                            randomData[0].getNext(),
                            randomData[1].getNext(),
                            randomData[2].getNext());

                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        feedThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        feedThread.interrupt();
    }

    private void addNewSample(float sample0, float sample1, float sample2) {
        //Log.i(TAG, "addNewSample: " + sample);
        if (sampleCount >= MAX_SAMPLES) return;
        yVals[0][sampleCount] = sample0;
        yVals[1][sampleCount] = sample1;
        yVals[2][sampleCount] = sample2;
        sampleCount++;

        dataView.setXOffs(sampleCount - DISPLAY_WINDOW);
        dataView.incUpdate();

        chartView.setXOffs(sampleCount - DISPLAY_WINDOW);
        chartView.update();
    }

}
