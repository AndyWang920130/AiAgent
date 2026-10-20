package com.example.myapp.web.rest;

import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.service.AdminUserService;
import com.example.myapp.service.dto.AdminUserDTO;
import com.example.myapp.utils.PageUtils;
import com.example.myapp.web.rest.vm.AdminCreateUserRequest;
import com.example.myapp.web.rest.vm.AdminResetPasswordRequest;
import com.example.myapp.web.rest.vm.AdminSetUserStatusRequest;
import com.example.myapp.web.rest.vm.AdminUpdateUserRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for User Management. Restricted to administrators by the
 * {@code /api/v1/admin/**} rule in SecurityConfig.
 *
 * <p>Replaces the generated CRUD controller that used to sit at {@code /api/my-app-users},
 * which was reachable by any authenticated user, returned password hashes, and let a
 * caller grant themselves {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserResource.class);

    private final AdminUserService adminUserService;

    public AdminUserResource(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * {@code GET /api/v1/admin/users} : one page of accounts.
     *
     * @param query    free text matched against login, real name, nick name and email.
     * @param role     keep only this role when set.
     * @param disabled keep only disabled ({@code true}) or active ({@code false}) accounts when set.
     */
    @GetMapping("")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "role", required = false) Role role,
        @RequestParam(name = "disabled", required = false) Boolean disabled,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LOG.debug("REST request to list users, query: {}, role: {}, disabled: {}", query, role, disabled);
        Page<AdminUserDTO> page = adminUserService.findAll(query, role, disabled, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /** {@code GET /api/v1/admin/users/:id} : one account, or 404. */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUser(@PathVariable("id") Long id) {
        LOG.debug("REST request to get user: {}", id);
        return adminUserService.findOne(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** {@code POST /api/v1/admin/users} : create an account. The password is encoded before it is stored. */
    @PostMapping("")
    public ResponseEntity<AdminUserDTO> createUser(@Valid @RequestBody AdminCreateUserRequest request) throws Exception {
        LOG.debug("REST request to create user: {}", request.login());
        AdminUserDTO result = adminUserService.create(request);
        return ResponseEntity.created(new URI("/api/v1/admin/users/" + result.getId())).body(result);
    }

    /**
     * {@code PUT /api/v1/admin/users/:id} : update the profile fields of an account.
     *
     * <p>The login cannot be changed here: it identifies the account by value in blogs,
     * comments and watch lists. Passwords go through {@code /reset-password}.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDTO> updateUser(
        @PathVariable("id") Long id,
        @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        LOG.debug("REST request to update user: {}", id);
        return ResponseEntity.ok(adminUserService.update(id, request));
    }

    /**
     * {@code PUT /api/v1/admin/users/:id/status} : disable or re-enable an account.
     *
     * <p>Accounts are disabled rather than removed. Disabling yourself, or the last
     * active administrator, is refused.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<AdminUserDTO> setUserStatus(
        @PathVariable("id") Long id,
        @Valid @RequestBody AdminSetUserStatusRequest request
    ) {
        LOG.debug("REST request to set status of user {} to disabled: {}", id, request.deleted());
        return ResponseEntity.ok(adminUserService.setStatus(id, request));
    }

    /** {@code POST /api/v1/admin/users/:id/reset-password} : set a new password for an account. */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(
        @PathVariable("id") Long id,
        @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        LOG.debug("REST request to reset the password of user: {}", id);
        adminUserService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
