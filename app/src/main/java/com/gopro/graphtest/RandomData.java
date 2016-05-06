package com.gopro.graphtest;

import junit.framework.Assert;

import java.util.Random;

class RandomData {
    private static final String LOG_TAG = "~RandomData";

    private final Random random;
    private final float min;
    private final float max;
    private final float noise;
    private final float volatility;
    private final float jerkiness;
    private final float timespan;

    private final float range;
    private float currValue;

    private int walkDuration = 0;
    private float walkTrend = 0;
    private float walkBias = 0;
    private float mean = 0;

    public RandomData(float min, float max, float noise, float volatility, float jerkiness, float timespan) {
        Assert.assertTrue(min < max);
        Assert.assertTrue(jerkiness <= 1.0F);

        this.range = max - min;
        this.mean = (max + min) * 0.5F;
        this.min = min;
        this.max = max;
        this.volatility = volatility;
        this.jerkiness = jerkiness;
        this.noise = noise;
        this.timespan = timespan;

        random = new Random();
        currValue = this.min + (range * random.nextFloat());
    }

    public float getNext() {
        float tmp;

        if (--walkDuration <= 0) {
            walkDuration = 2 + (int) (random.nextFloat() * timespan);
            tmp = range * (volatility / timespan) * ((random.nextFloat() * 2.0F) - 1.0F + walkBias);
            walkTrend = (jerkiness * tmp) + ((1 - jerkiness) * walkTrend);
        }

        tmp = (random.nextFloat() - 0.25F) * walkTrend;
        currValue += tmp;

        walkBias = ((mean - currValue) / range) * 0.6F;

        tmp = currValue + (range * noise * (random.nextFloat() - 0.5F));
        return (tmp > max) ? max : (tmp < min) ? min : tmp;
    }

}
