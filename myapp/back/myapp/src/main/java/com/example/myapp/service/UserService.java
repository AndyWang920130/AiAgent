package com.example.myapp.service;

import com.example.myapp.domain.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.UserDTO;
import com.example.myapp.service.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link User}.
 */
@Service
@Transactional
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Save a myAppUser.
     *
     * @param userDTO the entity to save.
     * @return the persisted entity.
     */
    public UserDTO save(UserDTO userDTO) {
        LOG.debug("Request to save MyAppUser : {}", userDTO);
        User user = userMapper.toEntity(userDTO);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    /**
     * Update a myAppUser.
     *
     * @param userDTO the entity to save.
     * @return the persisted entity.
     */
    public UserDTO update(UserDTO userDTO) {
        LOG.debug("Request to update MyAppUser : {}", userDTO);
        User user = userMapper.toEntity(userDTO);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    /**
     * Partially update a myAppUser.
     *
     * @param userDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserDTO> partialUpdate(UserDTO userDTO) {
        LOG.debug("Request to partially update MyAppUser : {}", userDTO);

        return userRepository
            .findById(userDTO.getId())
            .map(existingMyAppUser -> {
                userMapper.partialUpdate(existingMyAppUser, userDTO);

                return existingMyAppUser;
            })
            .map(userRepository::save)
            .map(userMapper::toDto);
    }

    /**
     * Get all the myAppUsers.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all MyAppUsers");
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    /**
     * Get one myAppUser by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> findOne(Long id) {
        LOG.debug("Request to get MyAppUser : {}", id);
        return userRepository.findById(id).map(userMapper::toDto);
    }

    /**
     * Delete the myAppUser by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete MyAppUser : {}", id);
        userRepository.deleteById(id);
    }
}
