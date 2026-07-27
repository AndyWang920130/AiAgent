package com.example.myapp.service;

import com.example.myapp.domain.EcgRecord;
import com.example.myapp.repository.EcgRecordRepository;
import com.example.myapp.service.dto.EcgRecordDTO;
import com.example.myapp.service.dto.EcgRecordSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read-only service exposing recorded ECG signals. ECG samples are persisted as
 * a comma-separated string on the entity and converted to a numeric array here.
 */
@Service
@Transactional(readOnly = true)
public class EcgRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(EcgRecordService.class);

    private final EcgRecordRepository ecgRecordRepository;

    public EcgRecordService(EcgRecordRepository ecgRecordRepository) {
        this.ecgRecordRepository = ecgRecordRepository;
    }

    /**
     * List all ECG records as lightweight summaries (no sample data).
     */
    public List<EcgRecordSummaryDTO> findAllSummaries() {
        LOG.debug("Request to list ECG record summaries");
        return ecgRecordRepository.findAllByOrderByIdAsc().stream().map(this::toSummary).toList();
    }

    /**
     * Get one ECG record with its full sample array.
     */
    public Optional<EcgRecordDTO> findOne(Long id) {
        LOG.debug("Request to get ECG record : {}", id);
        return ecgRecordRepository.findById(id).map(this::toDto);
    }

    private EcgRecordSummaryDTO toSummary(EcgRecord entity) {
        EcgRecordSummaryDTO dto = new EcgRecordSummaryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLeadName(entity.getLeadName());
        dto.setSampleRate(entity.getSampleRate());
        dto.setHeartRate(entity.getHeartRate());
        int count = countSamples(entity.getSamples());
        dto.setSampleCount(count);
        dto.setDurationMs(durationMs(count, entity.getSampleRate()));
        return dto;
    }

    private EcgRecordDTO toDto(EcgRecord entity) {
        EcgRecordDTO dto = new EcgRecordDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLeadName(entity.getLeadName());
        dto.setSampleRate(entity.getSampleRate());
        dto.setHeartRate(entity.getHeartRate());
        double[] samples = parseSamples(entity.getSamples());
        dto.setSamples(samples);
        dto.setDurationMs(durationMs(samples.length, entity.getSampleRate()));
        return dto;
    }

    private static long durationMs(int sampleCount, Integer sampleRate) {
        if (sampleRate == null || sampleRate <= 0) {
            return 0L;
        }
        return sampleCount * 1000L / sampleRate;
    }

    private static int countSamples(String csv) {
        if (csv == null || csv.isBlank()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < csv.length(); i++) {
            if (csv.charAt(i) == ',') {
                count++;
            }
        }
        return count;
    }

    private static double[] parseSamples(String csv) {
        if (csv == null || csv.isBlank()) {
            return new double[0];
        }
        String[] parts = csv.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return values;
    }
}
