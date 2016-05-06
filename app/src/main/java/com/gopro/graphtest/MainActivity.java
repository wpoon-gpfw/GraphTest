package com.gopro.graphtest;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final String TAG = "~MainActivity";

    private static final int MAX_SAMPLES = 2000;
    private static final int DISPLAY_WINDOW = 400;
    private static final int SAMPLE_INTERVAL = 40;

    private DataView dataView;
    private ChartView chartView;
    private float[] yVals = new float[MAX_SAMPLES];
    private int sampleCount = 0;
    private RandomData randomData;
    private Thread feedThread;
    private volatile boolean feedThreadPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        randomData = new RandomData(-1.00F, 1.00F, 0.02F, 1.5F, 0.5F, 20);

        //initYVals(1F);

        chartView = (ChartView) findViewById(R.id.chartView);
        chartView.setXRange(DISPLAY_WINDOW);
        chartView.setXOffs(0);
        chartView.postDelayed(new Runnable() {
            @Override
            public void run() {
                chartView.invalidate();
            }
        }, 100);

        dataView = (DataView) findViewById(R.id.dataView);
        dataView.setYVals(yVals);
        dataView.setXRange(DISPLAY_WINDOW);
        dataView.setXOffs(0);
        dataView.setYMin(-1F);
        dataView.setYMax(1F);

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

                    addNewSample(randomData.getNext());
                }
            }
        });
        feedThread.start();

    }

    private void initYVals(float yMax) {
        for (int i = 0; i < MAX_SAMPLES; i++) {
            yVals[i] = (i * yMax) / (MAX_SAMPLES - 1);
        }
    }

    private void addNewSample(float sample) {
        //Log.i(TAG, "addNewSample: " + sample);
        if (sampleCount >= MAX_SAMPLES) return;
        yVals[sampleCount++] = sample;
        dataView.setXOffs(sampleCount - DISPLAY_WINDOW);
        chartView.setXOffs(sampleCount - DISPLAY_WINDOW);
        chartView.update();
    }

}
