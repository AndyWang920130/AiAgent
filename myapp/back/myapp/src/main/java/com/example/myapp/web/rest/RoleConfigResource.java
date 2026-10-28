package com.example.myapp.web.rest;

import com.example.myapp.service.RoleConfigService;
import com.example.myapp.service.dto.RoleConfigDTO;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

/**
 * REST controller for the role catalog behind the Role Config page.
 *
 * <p>Writes are administrator-only via the {@code /api/v1/role-configs/**} rules in
 * SecurityConfig. Reads are left to any authenticated user because User Management fills
 * its role dropdown from this list.
 */
@RestController
@RequestMapping("/api/v1")
public class RoleConfigResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoleConfigResource.class);

    private static final String ENTITY_NAME = "roleConfig";

    private final RoleConfigService roleConfigService;

    public RoleConfigResource(RoleConfigService roleConfigService) {
        this.roleConfigService = roleConfigService;
    }

    @PostMapping("/role-configs")
    public ResponseEntity<RoleConfigDTO> createRoleConfig(@Valid @RequestBody RoleConfigDTO roleConfigDTO) throws URISyntaxException {
        LOG.debug("REST request to save RoleConfig : {}", roleConfigDTO);
        if (roleConfigDTO.getId() != null) {
            throw new BadRequestAlertException("A new role config cannot already have an ID", ENTITY_NAME, "idexists");
        }
        RoleConfigDTO result = roleConfigService.save(roleConfigDTO);
        return ResponseEntity.created(new URI("/api/v1/role-configs/" + result.getId())).body(result);
    }

    @PutMapping("/role-configs/{id}")
    public ResponseEntity<RoleConfigDTO> updateRoleConfig(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RoleConfigDTO roleConfigDTO
    ) {
        LOG.debug("REST request to update RoleConfig : {}, {}", id, roleConfigDTO);
        if (roleConfigDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roleConfigDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        return ResponseEntity.ok(roleConfigService.update(roleConfigDTO));
    }

    /**
     * {@code GET /api/v1/role-configs} : the role catalog, in configured order.
     *
     * @param enabledOnly keep only assignable roles, as the User Management dropdown needs.
     */
    @GetMapping("/role-configs")
    public ResponseEntity<List<RoleConfigDTO>> getAllRoleConfigs(
        @RequestParam(name = "enabledOnly", required = false, defaultValue = "false") boolean enabledOnly
    ) {
        LOG.debug("REST request to get the RoleConfig list, enabledOnly: {}", enabledOnly);
        return ResponseEntity.ok(roleConfigService.findAll(enabledOnly));
    }

    @GetMapping("/role-configs/{id}")
    public ResponseEntity<RoleConfigDTO> getRoleConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RoleConfig : {}", id);
        return roleConfigService.findOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/role-configs/{id}")
    public ResponseEntity<Void> deleteRoleConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete RoleConfig : {}", id);
        roleConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
