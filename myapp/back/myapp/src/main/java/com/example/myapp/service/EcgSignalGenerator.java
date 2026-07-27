package com.example.myapp.service;

import java.util.Random;

/**
 * Generates synthetic single-lead ECG waveforms. Each cardiac cycle is modelled
 * as a sum of Gaussian deflections (P, Q, R, S, T) positioned within the R-R
 * interval, with a small amount of baseline wander/noise so the trace looks
 * realistic rather than perfectly periodic. Amplitudes are in millivolts (mV).
 */
public final class EcgSignalGenerator {

    private EcgSignalGenerator() {}

    /** A single Gaussian deflection: center as a fraction [0,1) of the beat, width as a fraction, amplitude in mV. */
    private record Wave(double center, double width, double amplitude) {}

    // Classic PQRST morphology, positions expressed as a fraction of one R-R interval.
    private static final Wave[] WAVES = {
        new Wave(0.16, 0.030, 0.15),   // P
        new Wave(0.23, 0.008, -0.10),  // Q
        new Wave(0.25, 0.012, 1.20),   // R
        new Wave(0.27, 0.012, -0.25),  // S
        new Wave(0.42, 0.045, 0.30),   // T
    };

    /**
     * Generate {@code durationSec} seconds of ECG at the given heart rate and sample rate.
     *
     * @return amplitude samples in mV.
     */
    public static double[] generate(int heartRateBpm, int sampleRate, int durationSec) {
        int total = sampleRate * durationSec;
        double beatPeriodSec = 60.0 / heartRateBpm;
        double[] samples = new double[total];
        Random random = new Random(heartRateBpm); // deterministic per waveform

        for (int i = 0; i < total; i++) {
            double t = (double) i / sampleRate;
            double phase = (t % beatPeriodSec) / beatPeriodSec; // [0,1) within the beat

            double value = 0.0;
            for (Wave w : WAVES) {
                double d = phase - w.center();
                value += w.amplitude() * Math.exp(-(d * d) / (2.0 * w.width() * w.width()));
            }
            // slow baseline wander + small high-frequency noise
            value += 0.05 * Math.sin(2.0 * Math.PI * 0.3 * t);
            value += (random.nextDouble() - 0.5) * 0.02;

            samples[i] = value;
        }
        return samples;
    }

    /**
     * Serialize samples to a compact comma-separated string (3 decimal places),
     * as stored on the {@code twsny_ecg_record.samples} column.
     */
    public static String toCsv(double[] samples) {
        StringBuilder sb = new StringBuilder(samples.length * 6);
        for (int i = 0; i < samples.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Math.round(samples[i] * 1000.0) / 1000.0);
        }
        return sb.toString();
    }
}
