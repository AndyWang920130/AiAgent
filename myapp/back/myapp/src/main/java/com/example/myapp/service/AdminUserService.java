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
 * <p>Four rules are enforced here rather than left to the UI, since the API is reachable
 * without it: an administrator cannot lock themselves out (no self-demotion, no
 * self-disable), the last active administrator cannot lose that role or be disabled, an
 * assigned role must name an enabled row in the Role Config catalog, and passwords are
 * always encoded on the way in. Accounts are disabled rather than deleted — logins are
 * referenced by value across blogs, comments and stock watches, so removing a row would
 * orphan that content.
 */
@Service
@Transactional
public class AdminUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserService.class);

    private static final String ENTITY_NAME = "user";

    private final UserRepository userRepository;

    private final AdminUserMapper adminUserMapper;

    private final PasswordEncoder passwordEncoder;

    private final RoleConfigService roleConfigService;

    public AdminUserService(
        UserRepository userRepository,
        AdminUserMapper adminUserMapper,
        PasswordEncoder passwordEncoder,
        RoleConfigService roleConfigService
    ) {
        this.userRepository = userRepository;
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleConfigService = roleConfigService;
    }

    /**
     * Page through accounts, narrowed by any combination of the list page's filters.
     *
     * @param query    free-text match on login, real name, nickname or email; blank for all.
     * @param role     restrict to one role code, or null for any.
     * @param disabled true for disabled accounts, false for active, null for both.
     * @param pageable the pagination information.
     * @return a page of accounts.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> findAll(String query, String role, Boolean disabled, Pageable pageable) {
        LOG.debug("Request to get a page of Users : q={}, role={}, disabled={}", query, role, disabled);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String roleCode = role == null || role.isBlank() ? null : normalizeRole(role);
        return userRepository
            .findForAdmin("%" + term + "%", roleCode, disabled, pageable)
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
            .role(requireAssignableRole(request.role()))
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
        String newRole = requireAssignableRole(request.role());
        if (!newRole.equals(user.getRole())) {
            guardRoleChange(user, newRole);
            user.setRole(newRole);
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
     * Normalise a submitted role code and check it against the Role Config catalog. A code
     * that does not exist, or names a disabled role, is refused — otherwise an account could
     * be parked on a role that grants it nothing and cannot be reasoned about.
     */
    private String requireAssignableRole(String role) {
        String code = normalizeRole(role);
        if (!roleConfigService.isAssignable(code)) {
            throw new BadRequestAlertException("Unknown or disabled role: " + code, ENTITY_NAME, "roleinvalid");
        }
        return code;
    }

    private String normalizeRole(String role) {
        return role.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Refuse a role change that would lock administrators out of the app: demoting
     * yourself, or demoting the only administrator left. Any role other than
     * {@code ADMIN} counts as a demotion, since that is the only code the security
     * rules grant privileged access to.
     */
    private void guardRoleChange(User user, String newRole) {
        if (Role.ADMIN.name().equals(newRole)) {
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
        boolean activeAdmin = Role.ADMIN.name().equals(user.getRole()) && !Boolean.TRUE.equals(user.getDeleted());
        if (activeAdmin && userRepository.countActiveByRole(Role.ADMIN.name()) <= 1) {
            throw new BadRequestAlertException(message, ENTITY_NAME, "lastadmin");
        }
    }
}
