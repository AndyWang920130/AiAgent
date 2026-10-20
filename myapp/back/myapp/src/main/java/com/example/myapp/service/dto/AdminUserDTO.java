package com.example.myapp.service.dto;

import com.example.myapp.contants.enumeration.Gender;
import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.contants.enumeration.UserType;
import com.example.myapp.domain.User;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link User} entity as seen by an administrator in User Management.
 *
 * <p>Deliberately has no {@code password} field. The stored value is a bcrypt hash and
 * nothing outside the authentication flow has any reason to read it, so it is never
 * mapped into a response — an administrator resets a password rather than reading it.
 */
public class AdminUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String login;

    private String realName;

    private String nickName;

    private Gender gender;

    private UserType userType;

    private Role role;

    private String phoneNumber;

    private String email;

    private String avatar;

    private String description;

    /** True when the account is soft-deleted (disabled). Null is treated as active. */
    private Boolean deleted;

    private String deleteReason;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdminUserDTO)) {
            return false;
        }
        return id != null && Objects.equals(id, ((AdminUserDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
