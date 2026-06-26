package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.Gender;
import com.example.myapp.contants.enumeration.UserType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * The user entity.
 */
@Entity
@Table(name = "twsny_user")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 512)
    @Column(name = "login", length = 512, nullable = false)
    private String login;

    @Size(max = 512)
    @Column(name = "real_name", length = 512)
    private String realName;

    @Size(max = 512)
    @Column(name = "nick_name", length = 512)
    private String nickName;

    @Size(max = 512)
    @Column(name = "password", length = 512)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Size(max = 32)
    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Size(max = 512)
    @Column(name = "email", length = 512)
    private String email;

    @Size(max = 512)
    @Column(name = "avatar", length = 512)
    private String avatar;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "deleted")
    private Boolean deleted;

    @Size(max = 100)
    @Column(name = "delete_reason", length = 100)
    private String deleteReason;

    @Size(max = 50)
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate;

    @Size(max = 50)
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public User id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return this.login;
    }

    public User login(String login) {
        this.setLogin(login);
        return this;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getRealName() {
        return this.realName;
    }

    public User realName(String realName) {
        this.setRealName(realName);
        return this;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public User nickName(String nickName) {
        this.setNickName(nickName);
        return this;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPassword() {
        return this.password;
    }

    public User password(String password) {
        this.setPassword(password);
        return this;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Gender getGender() {
        return this.gender;
    }

    public User gender(Gender gender) {
        this.setGender(gender);
        return this;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public UserType getUserType() {
        return this.userType;
    }

    public User userType(UserType userType) {
        this.setUserType(userType);
        return this;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public User phoneNumber(String phoneNumber) {
        this.setPhoneNumber(phoneNumber);
        return this;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return this.email;
    }

    public User email(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public User avatar(String avatar) {
        this.setAvatar(avatar);
        return this;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getDescription() {
        return this.description;
    }

    public User description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public User deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getDeleteReason() {
        return this.deleteReason;
    }

    public User deleteReason(String deleteReason) {
        this.setDeleteReason(deleteReason);
        return this;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public User createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public User createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return this.lastModifiedBy;
    }

    public User lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public User lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        return getId() != null && getId().equals(((User) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MyAppUser{" +
            "id=" + getId() +
            ", login='" + getLogin() + "'" +
            ", realName='" + getRealName() + "'" +
            ", nickName='" + getNickName() + "'" +
            ", password='" + getPassword() + "'" +
            ", gender='" + getGender() + "'" +
            ", userType='" + getUserType() + "'" +
            ", phoneNumber='" + getPhoneNumber() + "'" +
            ", email='" + getEmail() + "'" +
            ", avatar='" + getAvatar() + "'" +
            ", description='" + getDescription() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", deleteReason='" + getDeleteReason() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
