package com.example.myapp.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Full ECG record for the chart view: metadata plus the amplitude samples (mV).
 */
public class EcgRecordDTO implements Serializable {

    private Long id;
    private String name;
    private String leadName;
    private Integer sampleRate;
    private Integer heartRate;
    private long durationMs;
    private double[] samples;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeadName() {
        return leadName;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public double[] getSamples() {
        return samples;
    }

    public void setSamples(double[] samples) {
        this.samples = samples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EcgRecordDTO)) return false;
        return id != null && Objects.equals(id, ((EcgRecordDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
