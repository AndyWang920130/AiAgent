package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A stored outbound API integration configuration. The flexible parts (headers,
 * query params, body config, response-data config) are persisted as JSON strings;
 * this feature only stores the definitions, it does not execute the calls.
 */
@Entity
@Table(name = "twsny_data_integration")
public class DataIntegration extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Size(max = 1000)
    @Column(name = "base_url", nullable = false, length = 1000)
    private String baseUrl;

    @Size(max = 1000)
    @Column(name = "path", length = 1000)
    private String path;

    @Size(max = 10)
    @Column(name = "method", length = 10)
    private String method;

    @Lob
    @Column(name = "headers", columnDefinition = "longtext")
    private String headers;

    @Lob
    @Column(name = "query_params", columnDefinition = "longtext")
    private String queryParams;

    @Lob
    @Column(name = "body_config", columnDefinition = "longtext")
    private String bodyConfig;

    /** How the request body is built: "KV" = the bodyConfig key/value rows, "RAW" = the bodyRaw JSON sent as-is. */
    @Size(max = 10)
    @Column(name = "body_type", length = 10)
    private String bodyType;

    /** Raw request body sent verbatim when bodyType is "RAW" (supports nested objects, arrays, non-string types). */
    @Lob
    @Column(name = "body_raw", columnDefinition = "longtext")
    private String bodyRaw;

    @Lob
    @Column(name = "response_config", columnDefinition = "longtext")
    private String responseConfig;

    /** Optional id of another integration to run first as an auth step (null = none). */
    @Column(name = "auth_source_id")
    private Long authSourceId;

    /** Dotted JSON path into the auth response body to extract the token (e.g. data.token). */
    @Size(max = 500)
    @Column(name = "auth_token_path", length = 500)
    private String authTokenPath;

    /** Header name to inject the token under (default Authorization when blank). */
    @Size(max = 100)
    @Column(name = "auth_header_name", length = 100)
    private String authHeaderName;

    /** Value template for the injected header ({{token}} is replaced; default "Bearer {{token}}"). */
    @Size(max = 500)
    @Column(name = "auth_header_template", length = 500)
    private String authHeaderTemplate;

    /**
     * Optional dotted path of a request-body property to inject the extracted value into
     * (e.g. {@code data.token} creates/overwrites a nested "token" under "data"). Blank = none.
     * Applies only to methods that send a body.
     */
    @Size(max = 500)
    @Column(name = "auth_body_property", length = 500)
    private String authBodyProperty;

    public Long getId() {
        return id;
    }

    public DataIntegration id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public DataIntegration name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public DataIntegration description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public DataIntegration baseUrl(String baseUrl) {
        this.setBaseUrl(baseUrl);
        return this;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPath() {
        return path;
    }

    public DataIntegration path(String path) {
        this.setPath(path);
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public DataIntegration method(String method) {
        this.setMethod(method);
        return this;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHeaders() {
        return headers;
    }

    public DataIntegration headers(String headers) {
        this.setHeaders(headers);
        return this;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public DataIntegration queryParams(String queryParams) {
        this.setQueryParams(queryParams);
        return this;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getBodyConfig() {
        return bodyConfig;
    }

    public DataIntegration bodyConfig(String bodyConfig) {
        this.setBodyConfig(bodyConfig);
        return this;
    }

    public void setBodyConfig(String bodyConfig) {
        this.bodyConfig = bodyConfig;
    }

    public String getBodyType() {
        return bodyType;
    }

    public DataIntegration bodyType(String bodyType) {
        this.setBodyType(bodyType);
        return this;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getBodyRaw() {
        return bodyRaw;
    }

    public DataIntegration bodyRaw(String bodyRaw) {
        this.setBodyRaw(bodyRaw);
        return this;
    }

    public void setBodyRaw(String bodyRaw) {
        this.bodyRaw = bodyRaw;
    }

    public String getResponseConfig() {
        return responseConfig;
    }

    public DataIntegration responseConfig(String responseConfig) {
        this.setResponseConfig(responseConfig);
        return this;
    }

    public void setResponseConfig(String responseConfig) {
        this.responseConfig = responseConfig;
    }

    public Long getAuthSourceId() {
        return authSourceId;
    }

    public DataIntegration authSourceId(Long authSourceId) {
        this.setAuthSourceId(authSourceId);
        return this;
    }

    public void setAuthSourceId(Long authSourceId) {
        this.authSourceId = authSourceId;
    }

    public String getAuthTokenPath() {
        return authTokenPath;
    }

    public DataIntegration authTokenPath(String authTokenPath) {
        this.setAuthTokenPath(authTokenPath);
        return this;
    }

    public void setAuthTokenPath(String authTokenPath) {
        this.authTokenPath = authTokenPath;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public DataIntegration authHeaderName(String authHeaderName) {
        this.setAuthHeaderName(authHeaderName);
        return this;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthHeaderTemplate() {
        return authHeaderTemplate;
    }

    public DataIntegration authHeaderTemplate(String authHeaderTemplate) {
        this.setAuthHeaderTemplate(authHeaderTemplate);
        return this;
    }

    public void setAuthHeaderTemplate(String authHeaderTemplate) {
        this.authHeaderTemplate = authHeaderTemplate;
    }

    public String getAuthBodyProperty() {
        return authBodyProperty;
    }

    public DataIntegration authBodyProperty(String authBodyProperty) {
        this.setAuthBodyProperty(authBodyProperty);
        return this;
    }

    public void setAuthBodyProperty(String authBodyProperty) {
        this.authBodyProperty = authBodyProperty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataIntegration)) return false;
        return getId() != null && getId().equals(((DataIntegration) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
