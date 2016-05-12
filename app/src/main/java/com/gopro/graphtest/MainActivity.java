package com.gopro.graphtest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "~MainActivity";

    private static final int MAX_SAMPLES = 5000;
    private static final int DISPLAY_WINDOW = 200;
    private static final int SAMPLE_INTERVAL = 10;

    private DataView dataView;
    private ChartView chartView;
    private final float[][] yVals = new float[3][MAX_SAMPLES];
    private int sampleCount = 0;
    private final RandomData[] randomData = new RandomData[3];
    private DataFeed dataFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        randomData[0] = new RandomData(-1.00F, 1.00F, 0.02F, 1.3F, 0.4F, 15);
        randomData[1] = new RandomData(-2.00F, 2.00F, 0.02F, 1.3F, 0.4F, 15);
        randomData[2] = new RandomData(-4.00F, 4.00F, 0.02F, 1.3F, 0.4F, 15);

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
        dataView.setYAbsMinMax(0, -1F, 1F);
        dataView.setYAbsMinMax(1, -2F, 2F);
        dataView.setYAbsMinMax(2, -4F, 4F);
        dataView.setYMinMax(0, -1F, 1F);
        dataView.setYMinMax(1, -2F, 2F);
        dataView.setYMinMax(2, -4F, 4F);
        dataView.setXRange(DISPLAY_WINDOW);
        dataView.setXOffs(0);
        dataView.setXSize(0);
        dataView.setXMaxSize(MAX_SAMPLES);

        dataFeed = new DataFeed();
        dataFeed.pause(true);
        dataFeed.start();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart: MainActivity");
        super.onStart();
        dataFeed.pause(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        dataFeed.pause(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataFeed.interrupt();
        dataView.destroy();
    }

    private class DataFeed extends Thread {
        private volatile boolean paused;

        @Override
        public void run() {
            while (true) {
                if (paused) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            Log.i(TAG, "feedThread: Bye!");
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                try {
                    Thread.sleep(SAMPLE_INTERVAL);
                } catch (InterruptedException e) {
                    Log.i(TAG, "feedThread: Adios!");
                    return;
                }

                addNewSample(
                        randomData[0].getNext(),
                        randomData[1].getNext(),
                        randomData[2].getNext());

            }
        }

        public synchronized void pause(boolean isPause) {
            paused = isPause;
            if (!isPause) this.notify();
        }
    }

    private void addNewSample(float sample0, float sample1, float sample2) {
        //Log.i(TAG, "addNewSample: " + sample);
        if (sampleCount >= 800) return;
        yVals[0][sampleCount] = sample0;
        yVals[1][sampleCount] = sample1;
        yVals[2][sampleCount] = sample2;
        sampleCount++;
        dataView.incUpdate();

        chartView.setXOffs(sampleCount - DISPLAY_WINDOW);
        chartView.update();
    }

}
