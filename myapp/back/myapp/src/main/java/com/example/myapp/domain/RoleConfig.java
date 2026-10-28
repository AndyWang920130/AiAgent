package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A role an account can hold. The catalog managed on the Role Config page is the source of
 * truth for which roles are assignable: {@code User.role} stores the {@link #code} of one
 * of these rows.
 *
 * <p>{@code USER} and {@code ADMIN} are seeded with {@link #builtIn} set, which locks their
 * code and stops them being disabled or deleted — the app's security rules name {@code ADMIN}
 * directly, and every account needs a role to fall back on.
 */
@Entity
@Table(
    name = "twsny_role_config",
    uniqueConstraints = @UniqueConstraint(name = "uk_twsny_role_config_code", columnNames = {"code"})
)
public class RoleConfig extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "built_in")
    private Boolean builtIn;

    public Long getId() {
        return id;
    }

    public RoleConfig id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public RoleConfig code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public RoleConfig name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public RoleConfig description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public RoleConfig sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public RoleConfig enabled(Boolean enabled) {
        this.setEnabled(enabled);
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public RoleConfig builtIn(Boolean builtIn) {
        this.setBuiltIn(builtIn);
        return this;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleConfig)) return false;
        return getId() != null && getId().equals(((RoleConfig) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoleConfig{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", enabled='" + getEnabled() + "'" +
            ", builtIn='" + getBuiltIn() + "'" +
            "}";
    }
}
