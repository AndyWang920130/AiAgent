package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.GameConfigType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "twsny_game_config",
    uniqueConstraints = @UniqueConstraint(name = "uk_twsny_game_config_type_name", columnNames = {"type", "name"})
)
public class GameConfig extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private GameConfigType type;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 50)
    @Column(name = "config_value", length = 50)
    private String value;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public GameConfig id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameConfigType getType() {
        return type;
    }

    public GameConfig type(GameConfigType type) {
        this.setType(type);
        return this;
    }

    public void setType(GameConfigType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public GameConfig name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public GameConfig value(String value) {
        this.setValue(value);
        return this;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public GameConfig description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public GameConfig sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameConfig)) return false;
        return getId() != null && getId().equals(((GameConfig) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
