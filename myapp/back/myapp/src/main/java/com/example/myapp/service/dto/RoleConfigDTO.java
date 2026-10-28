package com.example.myapp.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

public class RoleConfigDTO implements Serializable {

    private Long id;

    /**
     * The value stored in {@code User.role}. Uppercase so it maps straight onto the
     * {@code ROLE_<CODE>} authority the security layer grants.
     */
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "Code may only contain letters, digits and underscore")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Integer sortOrder;

    private Boolean enabled;

    /** True for the seeded USER and ADMIN rows, which cannot be renamed away or removed. */
    private Boolean builtIn;

    /** Active accounts currently holding this role. Read-only; computed on read. */
    private Long userCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleConfigDTO)) return false;
        RoleConfigDTO that = (RoleConfigDTO) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
