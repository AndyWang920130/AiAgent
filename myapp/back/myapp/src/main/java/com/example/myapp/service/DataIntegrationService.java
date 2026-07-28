package com.example.myapp.service;

import com.example.myapp.domain.DataIntegration;
import com.example.myapp.repository.DataIntegrationRepository;
import com.example.myapp.service.dto.DataIntegrationDTO;
import com.example.myapp.service.mapper.DataIntegrationMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import com.example.myapp.web.rest.vm.ExecuteResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service Implementation for managing {@link DataIntegration}.
 */
@Service
@Transactional
public class DataIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(DataIntegrationService.class);

    private static final String ENTITY_NAME = "dataIntegration";

    /** Cap the captured response body so a huge upstream payload can't bloat memory/response. */
    private static final int MAX_BODY_BYTES = 100 * 1024;

    private final DataIntegrationRepository dataIntegrationRepository;
    private final DataIntegrationMapper dataIntegrationMapper;
    private final RestClient dataIntegrationRestClient;
    private final ObjectMapper objectMapper;

    public DataIntegrationService(
        DataIntegrationRepository dataIntegrationRepository,
        DataIntegrationMapper dataIntegrationMapper,
        RestClient dataIntegrationRestClient,
        ObjectMapper objectMapper
    ) {
        this.dataIntegrationRepository = dataIntegrationRepository;
        this.dataIntegrationMapper = dataIntegrationMapper;
        this.dataIntegrationRestClient = dataIntegrationRestClient;
        this.objectMapper = objectMapper;
    }

    public DataIntegrationDTO save(DataIntegrationDTO dto) {
        LOG.debug("Request to save DataIntegration : {}", dto);
        normalize(dto);
        if (dataIntegrationRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new BadRequestAlertException("Name already exists", ENTITY_NAME, "nameexists");
        }
        DataIntegration entity = dataIntegrationMapper.toEntity(dto);
        String username = SecurityUtil.getCurrentUsername();
        entity.setCreatedBy(username);
        entity.setLastModifiedBy(username);
        return dataIntegrationMapper.toDto(dataIntegrationRepository.save(entity));
    }

    public DataIntegrationDTO update(DataIntegrationDTO dto) {
        LOG.debug("Request to update DataIntegration : {}", dto);
        normalize(dto);
        DataIntegration existing = dataIntegrationRepository.findById(dto.getId())
            .orElseThrow(() -> new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnotfound"));
        boolean duplicate = dataIntegrationRepository.findAllByOrderByIdAsc().stream()
            .anyMatch(item -> !Objects.equals(item.getId(), dto.getId())
                && item.getName().equalsIgnoreCase(dto.getName()));
        if (duplicate) {
            throw new BadRequestAlertException("Name already exists", ENTITY_NAME, "nameexists");
        }
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setBaseUrl(dto.getBaseUrl());
        existing.setPath(dto.getPath());
        existing.setMethod(dto.getMethod());
        existing.setHeaders(dto.getHeaders());
        existing.setQueryParams(dto.getQueryParams());
        existing.setBodyConfig(dto.getBodyConfig());
        existing.setResponseConfig(dto.getResponseConfig());
        existing.setAuthSourceId(dto.getAuthSourceId());
        existing.setAuthTokenPath(dto.getAuthTokenPath());
        existing.setAuthHeaderName(dto.getAuthHeaderName());
        existing.setAuthHeaderTemplate(dto.getAuthHeaderTemplate());
        existing.setLastModifiedBy(SecurityUtil.getCurrentUsername());
        return dataIntegrationMapper.toDto(dataIntegrationRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public List<DataIntegrationDTO> findAll() {
        LOG.debug("Request to get all DataIntegrations");
        return dataIntegrationMapper.toDto(dataIntegrationRepository.findAllByOrderByIdAsc());
    }

    @Transactional(readOnly = true)
    public Optional<DataIntegrationDTO> findOne(Long id) {
        LOG.debug("Request to get DataIntegration : {}", id);
        return dataIntegrationRepository.findById(id).map(dataIntegrationMapper::toDto);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete DataIntegration : {}", id);
        dataIntegrationRepository.deleteById(id);
    }

    /**
     * Execute a saved integration server-side and capture the upstream response so an
     * admin can validate it. Connection/timeout failures are returned as an
     * {@link ExecuteResult} with {@code success=false} rather than propagated, so a bad
     * upstream never turns into a 500 from our own API. Read-only; no persistence.
     */
    @Transactional(readOnly = true)
    public ExecuteResult execute(Long id) {
        LOG.debug("Request to execute DataIntegration : {}", id);
        DataIntegration entity = dataIntegrationRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnotfound"));

        Map<String, String> extraHeaders = new LinkedHashMap<>();

        // Optional auth pre-step: run the linked source, extract a token, inject a header.
        // Single-level only — the source's own auth source is ignored (prevents cycles).
        Long authSourceId = entity.getAuthSourceId();
        if (authSourceId != null && !authSourceId.equals(entity.getId())) {
            Optional<DataIntegration> authOpt = dataIntegrationRepository.findById(authSourceId);
            if (authOpt.isEmpty()) {
                return new ExecuteResult(0, 0, Map.of(), "", false, "auth step: auth source not found");
            }
            ExecuteResult authResult = performCall(authOpt.get(), Map.of());
            if (!authResult.success()) {
                String detail = authResult.error() != null ? authResult.error() : ("HTTP " + authResult.status());
                return new ExecuteResult(authResult.status(), authResult.durationMs(), authResult.headers(),
                    authResult.body(), false, "auth step failed: " + detail);
            }
            String token = extractByPath(authResult.body(), entity.getAuthTokenPath());
            String headerName = (entity.getAuthHeaderName() == null || entity.getAuthHeaderName().isBlank())
                ? "Authorization" : entity.getAuthHeaderName().trim();
            String template = (entity.getAuthHeaderTemplate() == null || entity.getAuthHeaderTemplate().isBlank())
                ? "Bearer {{token}}" : entity.getAuthHeaderTemplate();
            extraHeaders.put(headerName, template.replace("{{token}}", token == null ? "" : token));
        }

        return performCall(entity, extraHeaders);
    }

    /**
     * Extract a value from a JSON body by a dot-separated path (e.g. {@code data.token}).
     * Returns null on invalid JSON, blank path, or a missing/non-scalar target.
     */
    private String extractByPath(String bodyJson, String path) {
        if (bodyJson == null || bodyJson.isBlank() || path == null || path.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(bodyJson);
            for (String segment : path.trim().split("\\.")) {
                if (node == null) {
                    return null;
                }
                node = node.path(segment);
            }
            return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build and send the HTTP request for a single integration, applying {@code extraHeaders}
     * (e.g. an injected auth token) after the entity's own headers. Captures the response for
     * all statuses; wraps connection/timeout failures into an unsuccessful result.
     */
    private ExecuteResult performCall(DataIntegration entity, Map<String, String> extraHeaders) {
        HttpMethod method = HttpMethod.valueOf(
            (entity.getMethod() == null || entity.getMethod().isBlank() ? "GET" : entity.getMethod().trim().toUpperCase())
        );

        // Build URL: baseUrl + path + query params.
        String rawUrl = (entity.getBaseUrl() == null ? "" : entity.getBaseUrl().trim())
            + (entity.getPath() == null ? "" : entity.getPath().trim());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(rawUrl);
        for (Map<String, String> row : parseKeyValues(entity.getQueryParams())) {
            uriBuilder.queryParam(row.getOrDefault("key", ""), row.getOrDefault("value", ""));
        }

        List<Map<String, String>> headerRows = parseKeyValues(entity.getHeaders());
        boolean hasContentType = headerRows.stream()
            .anyMatch(h -> "content-type".equalsIgnoreCase(h.getOrDefault("key", "")));

        // Assemble a JSON body from the key/value rows for methods that carry one.
        String bodyJson = null;
        boolean sendBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
        if (sendBody) {
            List<Map<String, String>> bodyRows = parseKeyValues(entity.getBodyConfig());
            if (!bodyRows.isEmpty()) {
                Map<String, String> bodyMap = new LinkedHashMap<>();
                for (Map<String, String> row : bodyRows) {
                    bodyMap.put(row.getOrDefault("key", ""), row.getOrDefault("value", ""));
                }
                try {
                    bodyJson = objectMapper.writeValueAsString(bodyMap);
                } catch (Exception e) {
                    bodyJson = null;
                }
            }
        }

        long start = System.nanoTime();
        try {
            RestClient.RequestBodySpec spec = dataIntegrationRestClient
                .method(method)
                .uri(uriBuilder.build(true).toUri());
            for (Map<String, String> row : headerRows) {
                String key = row.getOrDefault("key", "");
                if (!key.isBlank()) {
                    spec.header(key, row.getOrDefault("value", ""));
                }
            }
            // Injected headers (e.g. auth token) go last so they take precedence.
            extraHeaders.forEach((k, v) -> {
                if (k != null && !k.isBlank()) {
                    spec.header(k, v);
                }
            });
            if (bodyJson != null) {
                if (!hasContentType) {
                    spec.header("Content-Type", "application/json");
                }
                spec.body(bodyJson);
            }

            ResponseEntity<byte[]> response = spec
                .retrieve()
                .onStatus(status -> true, (req, res) -> { /* never throw — capture all statuses */ })
                .toEntity(byte[].class);

            long durationMs = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatusCode().value();
            Map<String, String> headers = new LinkedHashMap<>();
            response.getHeaders().forEach((k, v) -> headers.put(k, String.join(", ", v)));
            String body = truncateBody(response.getBody());
            return new ExecuteResult(status, durationMs, headers, body, response.getStatusCode().is2xxSuccessful(), null);
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ExecuteResult(0, durationMs, Map.of(), "", false, msg);
        }
    }

    /** Parse a JSON string of {@code [{key,value}]} rows; tolerant of null/blank/invalid → empty list. */
    private List<Map<String, String>> parseKeyValues(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, String>> rows = objectMapper.readValue(json, new TypeReference<>() {});
            return rows == null ? new ArrayList<>() : rows;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String truncateBody(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        int len = Math.min(body.length, MAX_BODY_BYTES);
        String text = new String(body, 0, len, StandardCharsets.UTF_8);
        if (body.length > MAX_BODY_BYTES) {
            text += "\n… [truncated, " + body.length + " bytes total]";
        }
        return text;
    }

    private void normalize(DataIntegrationDTO dto) {
        dto.setName(trim(dto.getName()));
        dto.setDescription(trim(dto.getDescription()));
        dto.setBaseUrl(trim(dto.getBaseUrl()));
        dto.setPath(trim(dto.getPath()));
        dto.setMethod(trim(dto.getMethod()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
