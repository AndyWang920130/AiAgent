package com.example.myapp.web.rest;

import com.example.myapp.service.EcgRecordService;
import com.example.myapp.service.dto.EcgRecordDTO;
import com.example.myapp.service.dto.EcgRecordSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for reading ECG records. Access is restricted to administrators
 * via SecurityConfig ({@code /api/v1/ecg-records/**} requires ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/v1")
public class EcgRecordResource {

    private static final Logger LOG = LoggerFactory.getLogger(EcgRecordResource.class);

    private final EcgRecordService ecgRecordService;

    public EcgRecordResource(EcgRecordService ecgRecordService) {
        this.ecgRecordService = ecgRecordService;
    }

    /**
     * GET /ecg-records : List ECG record summaries (metadata only).
     */
    @GetMapping("/ecg-records")
    public ResponseEntity<List<EcgRecordSummaryDTO>> getAllEcgRecords() {
        LOG.debug("REST request to list ECG records");
        return ResponseEntity.ok(ecgRecordService.findAllSummaries());
    }

    /**
     * GET /ecg-records/{id} : Get one ECG record with its samples.
     */
    @GetMapping("/ecg-records/{id}")
    public ResponseEntity<EcgRecordDTO> getEcgRecord(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ECG record : {}", id);
        return ecgRecordService.findOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
