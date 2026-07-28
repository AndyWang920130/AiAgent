package com.example.myapp.web.rest;

import com.example.myapp.service.DataIntegrationService;
import com.example.myapp.service.dto.DataIntegrationDTO;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import com.example.myapp.web.rest.vm.ExecuteResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

/**
 * REST controller for managing DataIntegration. Access is restricted to
 * administrators via SecurityConfig ({@code /api/v1/data-integrations/**}).
 */
@RestController
@RequestMapping("/api/v1")
public class DataIntegrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(DataIntegrationResource.class);

    private static final String ENTITY_NAME = "dataIntegration";

    private final DataIntegrationService dataIntegrationService;

    public DataIntegrationResource(DataIntegrationService dataIntegrationService) {
        this.dataIntegrationService = dataIntegrationService;
    }

    @PostMapping("/data-integrations")
    public ResponseEntity<DataIntegrationDTO> createDataIntegration(@Valid @RequestBody DataIntegrationDTO dto) throws URISyntaxException {
        LOG.debug("REST request to save DataIntegration : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new data integration cannot already have an ID", ENTITY_NAME, "idexists");
        }
        DataIntegrationDTO result = dataIntegrationService.save(dto);
        return ResponseEntity.created(new URI("/api/v1/data-integrations/" + result.getId())).body(result);
    }

    @PutMapping("/data-integrations/{id}")
    public ResponseEntity<DataIntegrationDTO> updateDataIntegration(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DataIntegrationDTO dto
    ) {
        LOG.debug("REST request to update DataIntegration : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        return ResponseEntity.ok(dataIntegrationService.update(dto));
    }

    @GetMapping("/data-integrations")
    public ResponseEntity<List<DataIntegrationDTO>> getAllDataIntegrations() {
        LOG.debug("REST request to get all DataIntegrations");
        return ResponseEntity.ok(dataIntegrationService.findAll());
    }

    @GetMapping("/data-integrations/{id}")
    public ResponseEntity<DataIntegrationDTO> getDataIntegration(@PathVariable("id") Long id) {
        LOG.debug("REST request to get DataIntegration : {}", id);
        return dataIntegrationService.findOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/data-integrations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteDataIntegration(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete DataIntegration : {}", id);
        dataIntegrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/data-integrations/{id}/execute")
    public ResponseEntity<ExecuteResult> executeDataIntegration(@PathVariable("id") Long id) {
        LOG.debug("REST request to execute DataIntegration : {}", id);
        return ResponseEntity.ok(dataIntegrationService.execute(id));
    }
}
