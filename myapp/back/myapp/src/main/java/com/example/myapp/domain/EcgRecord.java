package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A recorded ECG (electrocardiogram) signal: a lead of amplitude samples plus
 * the metadata needed to render and scale it. Samples are stored as a
 * comma-separated list of millivolt values in {@link #samples}.
 */
@Entity
@Table(name = "twsny_ecg_record")
public class EcgRecord extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 20)
    @Column(name = "lead_name", length = 20)
    private String leadName;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Lob
    @Column(name = "samples", columnDefinition = "longtext")
    private String samples;

    public Long getId() {
        return id;
    }

    public EcgRecord id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public EcgRecord name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeadName() {
        return leadName;
    }

    public EcgRecord leadName(String leadName) {
        this.setLeadName(leadName);
        return this;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public EcgRecord sampleRate(Integer sampleRate) {
        this.setSampleRate(sampleRate);
        return this;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public EcgRecord heartRate(Integer heartRate) {
        this.setHeartRate(heartRate);
        return this;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public String getSamples() {
        return samples;
    }

    public EcgRecord samples(String samples) {
        this.setSamples(samples);
        return this;
    }

    public void setSamples(String samples) {
        this.samples = samples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EcgRecord)) return false;
        return getId() != null && getId().equals(((EcgRecord) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
