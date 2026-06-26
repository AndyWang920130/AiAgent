package com.example.myapp.web.rest;

import com.example.myapp.domain.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.UserService;
import com.example.myapp.service.dto.UserDTO;
import com.example.myapp.utils.PageUtils;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing {@link User}.
 */
@RestController
@RequestMapping("/api/my-app-users")
public class UserResource {

    private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);

    private static final String ENTITY_NAME = "user";

    private final UserService userService;

    private final UserRepository userRepository;

    public UserResource(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * {@code POST  /my-app-users} : Create a new myAppUser.
     *
     * @param myAppUserDTO the myAppUserDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new myAppUserDTO, or with status {@code 400 (Bad Request)} if the myAppUser has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UserDTO> createMyAppUser(@Valid @RequestBody UserDTO myAppUserDTO) throws URISyntaxException {
        LOG.debug("REST request to save MyAppUser : {}", myAppUserDTO);
        if (myAppUserDTO.getId() != null) {
            throw new BadRequestAlertException("A new myAppUser cannot already have an ID", ENTITY_NAME, "idexists");
        }
        myAppUserDTO = userService.save(myAppUserDTO);
        return ResponseEntity.ok(myAppUserDTO);
    }

    /**
     * {@code PUT  /my-app-users/:id} : Updates an existing myAppUser.
     *
     * @param id the id of the myAppUserDTO to save.
     * @param userDTO the myAppUserDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated myAppUserDTO,
     * or with status {@code 400 (Bad Request)} if the myAppUserDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the myAppUserDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateMyAppUser(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody UserDTO userDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MyAppUser : {}, {}", id, userDTO);
        if (userDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        userDTO = userService.update(userDTO);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * {@code PATCH  /my-app-users/:id} : Partial updates given fields of an existing myAppUser, field will ignore if it is null
     *
     * @param id the id of the myAppUserDTO to save.
     * @param userDTO the myAppUserDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated myAppUserDTO,
     * or with status {@code 400 (Bad Request)} if the myAppUserDTO is not valid,
     * or with status {@code 404 (Not Found)} if the myAppUserDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the myAppUserDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UserDTO> partialUpdateMyAppUser(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody UserDTO userDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MyAppUser partially : {}, {}", id, userDTO);
        if (userDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserDTO> result = userService.partialUpdate(userDTO);

        return ResponseEntity.ok(userDTO);
    }

    /**
     * {@code GET  /my-app-users} : get all the myAppUsers.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of myAppUsers in body.
     */
    @GetMapping("")
    public ResponseEntity<List<UserDTO>> getAllMyAppUsers(Pageable pageable) {
        LOG.debug("REST request to get a page of MyAppUsers");
        Page<UserDTO> page = userService.findAll(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /my-app-users/:id} : get the "id" myAppUser.
     *
     * @param id the id of the myAppUserDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the myAppUserDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getMyAppUser(@PathVariable("id") Long id) {
        LOG.debug("REST request to get MyAppUser : {}", id);
        Optional<UserDTO> userDTO = userService.findOne(id);
        return ResponseEntity.ok(userDTO.get());
    }

    /**
     * {@code DELETE  /my-app-users/:id} : delete the "id" myAppUser.
     *
     * @param id the id of the myAppUserDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyAppUser(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete MyAppUser : {}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();

    }
}
