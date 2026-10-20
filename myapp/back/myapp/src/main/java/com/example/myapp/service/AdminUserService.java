package com.example.myapp.service;

import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.AdminUserDTO;
import com.example.myapp.service.mapper.AdminUserMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import com.example.myapp.web.rest.vm.AdminCreateUserRequest;
import com.example.myapp.web.rest.vm.AdminResetPasswordRequest;
import com.example.myapp.web.rest.vm.AdminSetUserStatusRequest;
import com.example.myapp.web.rest.vm.AdminUpdateUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

/**
 * User Management service, for administrators only (see the {@code /api/v1/admin/**}
 * rule in SecurityConfig).
 *
 * <p>Three rules are enforced here rather than left to the UI, since the API is reachable
 * without it: an administrator cannot lock themselves out (no self-demotion, no
 * self-disable), the last active administrator cannot lose that role or be disabled, and
 * passwords are always encoded on the way in. Accounts are disabled rather than deleted —
 * logins are referenced by value across blogs, comments and stock watches, so removing a
 * row would orphan that content.
 */
@Service
@Transactional
public class AdminUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserService.class);

    private static final String ENTITY_NAME = "user";

    private final UserRepository userRepository;

    private final AdminUserMapper adminUserMapper;

    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
        UserRepository userRepository,
        AdminUserMapper adminUserMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Page through accounts, narrowed by any combination of the list page's filters.
     *
     * @param query    free-text match on login, real name, nickname or email; blank for all.
     * @param role     restrict to one role, or null for any.
     * @param disabled true for disabled accounts, false for active, null for both.
     * @param pageable the pagination information.
     * @return a page of accounts.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> findAll(String query, Role role, Boolean disabled, Pageable pageable) {
        LOG.debug("Request to get a page of Users : q={}, role={}, disabled={}", query, role, disabled);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository
            .findForAdmin("%" + term + "%", role, disabled, pageable)
            .map(adminUserMapper::toDto);
    }

    /**
     * Get one account.
     *
     * @param id the id of the user.
     * @return the account, or empty when no such id exists.
     */
    @Transactional(readOnly = true)
    public Optional<AdminUserDTO> findOne(Long id) {
        LOG.debug("Request to get User : {}", id);
        return userRepository.findById(id).map(adminUserMapper::toDto);
    }

    /**
     * Create an account directly, skipping the email verification self-registration needs.
     *
     * @param request the account to create, with a plain-text password.
     * @return the created account.
     */
    public AdminUserDTO create(AdminCreateUserRequest request) {
        LOG.debug("Request to create User : {}", request.login());
        String login = request.login().trim();
        if (userRepository.existsByLogin(login)) {
            throw new BadRequestAlertException("Login already in use", ENTITY_NAME, "loginexists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestAlertException("Email already in use", ENTITY_NAME, "emailexists");
        }
        User user = new User()
            .login(login)
            .realName(request.realName())
            .nickName(request.nickName())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(request.role())
            .gender(request.gender())
            .userType(request.userType())
            .phoneNumber(request.phoneNumber())
            .description(request.description())
            .deleted(false);
        return adminUserMapper.toDto(userRepository.save(user));
    }

    /**
     * Update an account's profile fields and role.
     *
     * @param id the id of the user.
     * @param request the new values.
     * @return the updated account.
     */
    public AdminUserDTO update(Long id, AdminUpdateUserRequest request) {
        LOG.debug("Request to update User : {}", id);
        User user = require(id);
        if (emailTakenByOther(request.email(), user)) {
            throw new BadRequestAlertException("Email already in use", ENTITY_NAME, "emailexists");
        }
        if (request.role() != user.getRole()) {
            guardRoleChange(user, request.role());
            user.setRole(request.role());
        }
        user.setRealName(request.realName());
        user.setNickName(request.nickName());
        user.setEmail(request.email());
        user.setGender(request.gender());
        user.setUserType(request.userType());
        user.setPhoneNumber(request.phoneNumber());
        user.setDescription(request.description());
        return adminUserMapper.toDto(userRepository.save(user));
    }

    /**
     * Disable or re-enable an account. Disabling is a soft delete: the row stays so
     * content that references the login keeps resolving, but the account is excluded
     * from search and is refused at login.
     *
     * @param id the id of the user.
     * @param request whether to disable, and why.
     * @return the updated account.
     */
    public AdminUserDTO setStatus(Long id, AdminSetUserStatusRequest request) {
        LOG.debug("Request to set status of User : {} to deleted={}", id, request.deleted());
        User user = require(id);
        boolean disable = Boolean.TRUE.equals(request.deleted());
        if (disable) {
            guardSelf(user, "Administrators cannot disable their own account");
            guardLastAdmin(user, "The last administrator cannot be disabled");
            user.setDeleted(true);
            user.setDeleteReason(request.reason());
        } else {
            user.setDeleted(false);
            user.setDeleteReason(null);
        }
        return adminUserMapper.toDto(userRepository.save(user));
    }

    /**
     * Set a new password for an account, encoded before it is stored.
     *
     * @param id the id of the user.
     * @param request the new password in plain text.
     */
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        LOG.debug("Request to reset the password of User : {}", id);
        User user = require(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User require(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean emailTakenByOther(String email, User user) {
        return userRepository
            .findOneByEmailIgnoreCase(email)
            .filter(other -> !other.getId().equals(user.getId()))
            .isPresent();
    }

    /**
     * Refuse a role change that would lock administrators out of the app: demoting
     * yourself, or demoting the only administrator left.
     */
    private void guardRoleChange(User user, Role newRole) {
        if (newRole == Role.ADMIN) {
            return;
        }
        guardSelf(user, "Administrators cannot change their own role");
        guardLastAdmin(user, "The last administrator cannot be demoted");
    }

    private void guardSelf(User user, String message) {
        if (user.getLogin().equals(SecurityUtil.getCurrentUsername())) {
            throw new BadRequestAlertException(message, ENTITY_NAME, "selfaction");
        }
    }

    private void guardLastAdmin(User user, String message) {
        boolean activeAdmin = user.getRole() == Role.ADMIN && !Boolean.TRUE.equals(user.getDeleted());
        if (activeAdmin && userRepository.countActiveByRole(Role.ADMIN) <= 1) {
            throw new BadRequestAlertException(message, ENTITY_NAME, "lastadmin");
        }
    }
}
