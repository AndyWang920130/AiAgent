package com.example.myapp.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Lightweight ECG record projection for list views: metadata only, without the
 * (potentially large) sample array.
 */
public class EcgRecordSummaryDTO implements Serializable {

    private Long id;
    private String name;
    private String leadName;
    private Integer sampleRate;
    private Integer heartRate;
    private int sampleCount;
    private long durationMs;

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

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EcgRecordSummaryDTO)) return false;
        return id != null && Objects.equals(id, ((EcgRecordSummaryDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
