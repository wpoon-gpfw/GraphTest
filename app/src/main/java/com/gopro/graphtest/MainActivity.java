package com.gopro.graphtest;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final String TAG = "~MainActivity";

    private static final int MAX_SAMPLES = 2000;
    private static final int DISPLAY_WINDOW = 300;
    private static final int SAMPLE_INTERVAL = 40;

    private CanvasView canvasView;
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

        canvasView = (CanvasView) findViewById(R.id.cvw);
        canvasView.setXRange(DISPLAY_WINDOW);
        canvasView.setXOffs(0);
        canvasView.setYVals(yVals);
        canvasView.setYMin(-1F);
        canvasView.setYMax(1F);

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
        canvasView.setXOffs(sampleCount - DISPLAY_WINDOW);
        canvasView.update();
    }

}
