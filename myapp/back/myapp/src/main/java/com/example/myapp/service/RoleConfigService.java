package com.example.myapp.service;

import com.example.myapp.domain.RoleConfig;
import com.example.myapp.repository.RoleConfigRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.RoleConfigDTO;
import com.example.myapp.service.mapper.RoleConfigMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Role catalog service, backing the Role Config page. Writes are administrator-only
 * (see the {@code /api/v1/role-configs} rules in SecurityConfig).
 *
 * <p>Three rules are enforced here rather than left to the UI, since the API is reachable
 * without it: the seeded {@code USER} and {@code ADMIN} rows cannot be deleted, disabled or
 * have their code changed — the security rules name {@code ADMIN} directly and accounts need
 * a role to fall back on; a role still held by an account cannot be deleted or disabled,
 * which would leave those accounts pointing at a role that no longer resolves; and codes are
 * uppercased so they line up with the {@code ROLE_<CODE>} authority the security layer grants.
 */
@Service
@Transactional
public class RoleConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(RoleConfigService.class);

    private static final String ENTITY_NAME = "roleConfig";

    private final RoleConfigRepository roleConfigRepository;

    private final UserRepository userRepository;

    private final RoleConfigMapper roleConfigMapper;

    public RoleConfigService(
        RoleConfigRepository roleConfigRepository,
        UserRepository userRepository,
        RoleConfigMapper roleConfigMapper
    ) {
        this.roleConfigRepository = roleConfigRepository;
        this.userRepository = userRepository;
        this.roleConfigMapper = roleConfigMapper;
    }

    /**
     * Create a role. The code is uppercased and must not already be taken.
     *
     * @param roleConfigDTO the role to create.
     * @return the created role.
     */
    public RoleConfigDTO save(RoleConfigDTO roleConfigDTO) {
        LOG.debug("Request to save RoleConfig : {}", roleConfigDTO);
        normalize(roleConfigDTO);
        if (roleConfigRepository.existsByCodeIgnoreCase(roleConfigDTO.getCode())) {
            throw new BadRequestAlertException("Role code already exists", ENTITY_NAME, "codeexists");
        }
        RoleConfig roleConfig = roleConfigMapper.toEntity(roleConfigDTO);
        // Only the seeded rows are built in; anything created through the API is not.
        roleConfig.setBuiltIn(false);
        roleConfig.setEnabled(roleConfigDTO.getEnabled() == null || roleConfigDTO.getEnabled());
        String username = SecurityUtil.getCurrentUsername();
        roleConfig.setCreatedBy(username);
        roleConfig.setLastModifiedBy(username);
        return withUserCount(roleConfigMapper.toDto(roleConfigRepository.save(roleConfig)));
    }

    /**
     * Update a role's display fields, and its code when the role is not built in.
     *
     * @param roleConfigDTO the new values.
     * @return the updated role.
     */
    public RoleConfigDTO update(RoleConfigDTO roleConfigDTO) {
        LOG.debug("Request to update RoleConfig : {}", roleConfigDTO);
        normalize(roleConfigDTO);
        RoleConfig existing = roleConfigRepository.findById(roleConfigDTO.getId())
            .orElseThrow(() -> new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnotfound"));
        boolean builtIn = Boolean.TRUE.equals(existing.getBuiltIn());

        if (!existing.getCode().equalsIgnoreCase(roleConfigDTO.getCode())) {
            if (builtIn) {
                throw new BadRequestAlertException("A built-in role's code cannot be changed", ENTITY_NAME, "builtincode");
            }
            if (roleConfigRepository.existsByCodeIgnoreCase(roleConfigDTO.getCode())) {
                throw new BadRequestAlertException("Role code already exists", ENTITY_NAME, "codeexists");
            }
            // Accounts store the code by value, so renaming one that is in use would leave
            // them pointing at a role that no longer resolves.
            guardNotInUse(existing, "A role held by an account cannot be renamed");
            existing.setCode(roleConfigDTO.getCode());
        }

        boolean enabled = roleConfigDTO.getEnabled() == null || roleConfigDTO.getEnabled();
        if (!enabled && Boolean.TRUE.equals(existing.getEnabled())) {
            if (builtIn) {
                throw new BadRequestAlertException("A built-in role cannot be disabled", ENTITY_NAME, "builtindisable");
            }
            guardNotInUse(existing, "A role held by an account cannot be disabled");
        }

        existing.setName(roleConfigDTO.getName());
        existing.setDescription(roleConfigDTO.getDescription());
        existing.setSortOrder(roleConfigDTO.getSortOrder());
        existing.setEnabled(enabled);
        existing.setLastModifiedBy(SecurityUtil.getCurrentUsername());
        return withUserCount(roleConfigMapper.toDto(roleConfigRepository.save(existing)));
    }

    /**
     * List roles, each carrying how many accounts hold it.
     *
     * @param enabledOnly keep only assignable roles, for the User Management dropdown.
     * @return the roles, in configured order.
     */
    @Transactional(readOnly = true)
    public List<RoleConfigDTO> findAll(boolean enabledOnly) {
        LOG.debug("Request to get the RoleConfig list, enabledOnly: {}", enabledOnly);
        List<RoleConfig> roles = enabledOnly
            ? roleConfigRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()
            : roleConfigRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<String, Long> counts = userCountsByRole();
        return roles.stream()
            .map(roleConfigMapper::toDto)
            .peek(dto -> dto.setUserCount(counts.getOrDefault(key(dto.getCode()), 0L)))
            .toList();
    }

    /**
     * Get one role.
     *
     * @param id the id of the role.
     * @return the role, or empty when no such id exists.
     */
    @Transactional(readOnly = true)
    public Optional<RoleConfigDTO> findOne(Long id) {
        LOG.debug("Request to get RoleConfig : {}", id);
        return roleConfigRepository.findById(id).map(roleConfigMapper::toDto).map(this::withUserCount);
    }

    /**
     * Delete a role. Refused for the built-in rows and for any role still held by an account.
     *
     * @param id the id of the role.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete RoleConfig : {}", id);
        RoleConfig existing = roleConfigRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnotfound"));
        if (Boolean.TRUE.equals(existing.getBuiltIn())) {
            throw new BadRequestAlertException("A built-in role cannot be deleted", ENTITY_NAME, "builtindelete");
        }
        guardNotInUse(existing, "A role held by an account cannot be deleted");
        roleConfigRepository.deleteById(id);
    }

    /**
     * Whether a role can be assigned to an account: it exists in the catalog and is enabled.
     * Used by {@link AdminUserService} to validate what a request asks for.
     *
     * @param code the role code to check.
     * @return true when the code names an enabled role.
     */
    @Transactional(readOnly = true)
    public boolean isAssignable(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return roleConfigRepository.findOneByCodeIgnoreCase(code.trim())
            .filter(role -> Boolean.TRUE.equals(role.getEnabled()))
            .isPresent();
    }

    private RoleConfigDTO withUserCount(RoleConfigDTO dto) {
        dto.setUserCount(userCountsByRole().getOrDefault(key(dto.getCode()), 0L));
        return dto;
    }

    /**
     * Accounts per role, keyed by upper-cased code. Counts disabled accounts too: a disabled
     * account still holds its role, and re-enabling it must not resurrect a deleted one.
     */
    private Map<String, Long> userCountsByRole() {
        Map<String, Long> counts = new HashMap<>();
        for (UserRepository.RoleCount row : userRepository.countAllGroupedByRole()) {
            if (row.getRole() != null) {
                counts.merge(key(row.getRole()), row.getCount(), Long::sum);
            }
        }
        return counts;
    }

    private void guardNotInUse(RoleConfig role, String message) {
        long inUse = userCountsByRole().getOrDefault(key(role.getCode()), 0L);
        if (inUse > 0) {
            throw new BadRequestAlertException(message, ENTITY_NAME, "roleinuse");
        }
    }

    private void normalize(RoleConfigDTO dto) {
        dto.setCode(dto.getCode() == null ? null : key(dto.getCode()));
        dto.setName(trim(dto.getName()));
        dto.setDescription(trim(dto.getDescription()));
    }

    private String key(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
