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
        existing.setBodyType(dto.getBodyType());
        existing.setBodyRaw(dto.getBodyRaw());
        existing.setResponseConfig(dto.getResponseConfig());
        existing.setAuthSourceId(dto.getAuthSourceId());
        existing.setAuthTokenPath(dto.getAuthTokenPath());
        existing.setAuthHeaderName(dto.getAuthHeaderName());
        existing.setAuthHeaderTemplate(dto.getAuthHeaderTemplate());
        existing.setAuthBodyProperty(dto.getAuthBodyProperty());
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
        Map<String, String> extraBodyProps = new LinkedHashMap<>();

        // Optional pre-step: run the linked source integration, extract a value from its
        // response, and inject that value into a header and/or a request-body property. The
        // source is NOT necessarily an authentication interface — the extracted value may be
        // a token, an id, or any scalar. Single-level only — the source's own source link is
        // ignored (prevents cycles).
        Long sourceId = entity.getAuthSourceId();
        if (sourceId != null && !sourceId.equals(entity.getId())) {
            Optional<DataIntegration> sourceOpt = dataIntegrationRepository.findById(sourceId);
            if (sourceOpt.isEmpty()) {
                return new ExecuteResult(0, 0, Map.of(), "", false, "pre-step: source integration not found");
            }
            ExecuteResult sourceResult = performCall(sourceOpt.get(), Map.of(), Map.of());
            if (!sourceResult.success()) {
                String detail = sourceResult.error() != null ? sourceResult.error() : ("HTTP " + sourceResult.status());
                return new ExecuteResult(sourceResult.status(), sourceResult.durationMs(), sourceResult.headers(),
                    sourceResult.body(), false, "pre-step failed: " + detail);
            }
            String extracted = extractByPath(sourceResult.body(), entity.getAuthTokenPath());
            String value = extracted == null ? "" : extracted;

            boolean headerConfigured = (entity.getAuthHeaderName() != null && !entity.getAuthHeaderName().isBlank())
                || (entity.getAuthHeaderTemplate() != null && !entity.getAuthHeaderTemplate().isBlank());
            String bodyProperty = entity.getAuthBodyProperty();
            boolean bodyConfigured = bodyProperty != null && !bodyProperty.isBlank();

            // Inject a header when one is explicitly configured, or by default when no body
            // target was given (preserves the original auth-token behavior for existing
            // integrations). When only a body property is set, skip the header so a plain
            // value-forwarding chain sends no spurious Authorization header.
            if (headerConfigured || !bodyConfigured) {
                String headerName = (entity.getAuthHeaderName() == null || entity.getAuthHeaderName().isBlank())
                    ? "Authorization" : entity.getAuthHeaderName().trim();
                String template = (entity.getAuthHeaderTemplate() == null || entity.getAuthHeaderTemplate().isBlank())
                    ? "Bearer {{token}}" : entity.getAuthHeaderTemplate();
                extraHeaders.put(headerName, template.replace("{{token}}", value));
            }

            // Inject the extracted value into a request-body property (dotted path).
            if (bodyConfigured) {
                extraBodyProps.put(bodyProperty.trim(), value);
            }
        }

        return performCall(entity, extraHeaders, extraBodyProps);
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
     * Inject each {@code dotted.path → value} into the JSON body, creating intermediate
     * objects as needed and overwriting any existing leaf. Starts from an empty object when
     * {@code bodyJson} is null/blank or not a JSON object. On any failure the original body
     * is returned unchanged, so a bad injection never breaks the call.
     */
    private String injectBodyProperties(String bodyJson, Map<String, String> props) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = (bodyJson == null || bodyJson.isBlank())
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(bodyJson);
            if (!(root instanceof com.fasterxml.jackson.databind.node.ObjectNode)) {
                // Only object bodies can carry named properties; leave arrays/scalars untouched.
                return bodyJson;
            }
            com.fasterxml.jackson.databind.node.ObjectNode rootObj =
                (com.fasterxml.jackson.databind.node.ObjectNode) root;
            for (Map.Entry<String, String> prop : props.entrySet()) {
                String[] segments = prop.getKey().trim().split("\\.");
                com.fasterxml.jackson.databind.node.ObjectNode cursor = rootObj;
                for (int i = 0; i < segments.length - 1; i++) {
                    com.fasterxml.jackson.databind.JsonNode child = cursor.get(segments[i]);
                    if (child instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                        cursor = (com.fasterxml.jackson.databind.node.ObjectNode) child;
                    } else {
                        cursor = cursor.putObject(segments[i]);
                    }
                }
                cursor.put(segments[segments.length - 1], prop.getValue());
            }
            return objectMapper.writeValueAsString(rootObj);
        } catch (Exception e) {
            return bodyJson;
        }
    }

    /**
     * Build and send the HTTP request for a single integration, applying {@code extraHeaders}
     * (e.g. an injected auth token) after the entity's own headers, and {@code extraBodyProps}
     * (dotted-path → value) into the JSON request body. Captures the response for all statuses;
     * wraps connection/timeout failures into an unsuccessful result.
     */
    private ExecuteResult performCall(DataIntegration entity, Map<String, String> extraHeaders,
                                      Map<String, String> extraBodyProps) {
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

        // Assemble the request body for methods that carry one. Two modes:
        //   RAW → send bodyRaw verbatim (supports nested objects, arrays, non-string types);
        //   KV  → build a flat JSON object from the key/value rows.
        String bodyJson = null;
        boolean sendBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
        if (sendBody) {
            if ("RAW".equalsIgnoreCase(entity.getBodyType())) {
                String raw = entity.getBodyRaw();
                if (raw != null && !raw.isBlank()) {
                    bodyJson = raw;
                }
            } else {
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
            // Inject any auth-derived body properties (dotted path → value). Starts from an
            // empty object when no body was configured, so injection alone still sends a body.
            if (!extraBodyProps.isEmpty()) {
                bodyJson = injectBodyProperties(bodyJson, extraBodyProps);
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
        // Default the body mode so a missing value always means the flat key/value path.
        String bodyType = trim(dto.getBodyType());
        dto.setBodyType("RAW".equalsIgnoreCase(bodyType) ? "RAW" : "KV");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
