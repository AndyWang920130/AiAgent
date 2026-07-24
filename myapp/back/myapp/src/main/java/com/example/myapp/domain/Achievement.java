package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.AchievementType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * A single achievement points bucket for a user, keyed by login and type.
 */
@Entity
@Table(
    name = "twsny_achievement",
    uniqueConstraints = @UniqueConstraint(name = "uk_twsny_achievement_login_type", columnNames = {"login", "type"})
)
public class Achievement extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 512)
    @Column(name = "login", length = 512, nullable = false)
    private String login;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AchievementType type;

    @Column(name = "points")
    private Long points;

    public Long getId() {
        return this.id;
    }

    public Achievement id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return this.login;
    }

    public Achievement login(String login) {
        this.setLogin(login);
        return this;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public AchievementType getType() {
        return this.type;
    }

    public Achievement type(AchievementType type) {
        this.setType(type);
        return this;
    }

    public void setType(AchievementType type) {
        this.type = type;
    }

    public Long getPoints() {
        return this.points;
    }

    public Achievement points(Long points) {
        this.setPoints(points);
        return this;
    }

    public void setPoints(Long points) {
        this.points = points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Achievement)) {
            return false;
        }
        return getId() != null && getId().equals(((Achievement) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Achievement{" +
            "id=" + getId() +
            ", login='" + getLogin() + "'" +
            ", type='" + getType() + "'" +
            ", points=" + getPoints() +
            "}";
    }
}
