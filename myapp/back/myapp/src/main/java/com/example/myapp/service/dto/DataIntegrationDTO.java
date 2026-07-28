package com.example.myapp.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.example.myapp.domain.DataIntegration} entity. The four
 * config fields (headers, queryParams, bodyConfig, responseConfig) are JSON strings
 * produced/consumed by the frontend; the backend treats them as opaque text.
 */
public class DataIntegrationDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @Size(max = 1000)
    private String baseUrl;

    @Size(max = 1000)
    private String path;

    @Size(max = 10)
    private String method;

    private String headers;
    private String queryParams;
    private String bodyConfig;
    private String responseConfig;

    private Long authSourceId;
    private String authTokenPath;
    private String authHeaderName;
    private String authHeaderTemplate;

    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getBodyConfig() {
        return bodyConfig;
    }

    public void setBodyConfig(String bodyConfig) {
        this.bodyConfig = bodyConfig;
    }

    public String getResponseConfig() {
        return responseConfig;
    }

    public void setResponseConfig(String responseConfig) {
        this.responseConfig = responseConfig;
    }

    public Long getAuthSourceId() {
        return authSourceId;
    }

    public void setAuthSourceId(Long authSourceId) {
        this.authSourceId = authSourceId;
    }

    public String getAuthTokenPath() {
        return authTokenPath;
    }

    public void setAuthTokenPath(String authTokenPath) {
        this.authTokenPath = authTokenPath;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthHeaderTemplate() {
        return authHeaderTemplate;
    }

    public void setAuthHeaderTemplate(String authHeaderTemplate) {
        this.authHeaderTemplate = authHeaderTemplate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataIntegrationDTO)) return false;
        return id != null && Objects.equals(id, ((DataIntegrationDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
