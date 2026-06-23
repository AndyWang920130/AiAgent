package com.example.myapp.service;

import com.example.myapp.domain.MyAppUser;
import com.example.myapp.repository.MyAppUserRepository;
import com.example.myapp.service.dto.MyAppUserDTO;
import com.example.myapp.service.mapper.MyAppUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link MyAppUser}.
 */
@Service
@Transactional
public class MyAppUserService {

    private static final Logger LOG = LoggerFactory.getLogger(MyAppUserService.class);

    private final MyAppUserRepository myAppUserRepository;

    private final MyAppUserMapper myAppUserMapper;

    public MyAppUserService(MyAppUserRepository myAppUserRepository, MyAppUserMapper myAppUserMapper) {
        this.myAppUserRepository = myAppUserRepository;
        this.myAppUserMapper = myAppUserMapper;
    }

    /**
     * Save a myAppUser.
     *
     * @param myAppUserDTO the entity to save.
     * @return the persisted entity.
     */
    public MyAppUserDTO save(MyAppUserDTO myAppUserDTO) {
        LOG.debug("Request to save MyAppUser : {}", myAppUserDTO);
        MyAppUser myAppUser = myAppUserMapper.toEntity(myAppUserDTO);
        myAppUser = myAppUserRepository.save(myAppUser);
        return myAppUserMapper.toDto(myAppUser);
    }

    /**
     * Update a myAppUser.
     *
     * @param myAppUserDTO the entity to save.
     * @return the persisted entity.
     */
    public MyAppUserDTO update(MyAppUserDTO myAppUserDTO) {
        LOG.debug("Request to update MyAppUser : {}", myAppUserDTO);
        MyAppUser myAppUser = myAppUserMapper.toEntity(myAppUserDTO);
        myAppUser = myAppUserRepository.save(myAppUser);
        return myAppUserMapper.toDto(myAppUser);
    }

    /**
     * Partially update a myAppUser.
     *
     * @param myAppUserDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MyAppUserDTO> partialUpdate(MyAppUserDTO myAppUserDTO) {
        LOG.debug("Request to partially update MyAppUser : {}", myAppUserDTO);

        return myAppUserRepository
            .findById(myAppUserDTO.getId())
            .map(existingMyAppUser -> {
                myAppUserMapper.partialUpdate(existingMyAppUser, myAppUserDTO);

                return existingMyAppUser;
            })
            .map(myAppUserRepository::save)
            .map(myAppUserMapper::toDto);
    }

    /**
     * Get all the myAppUsers.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<MyAppUserDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all MyAppUsers");
        return myAppUserRepository.findAll(pageable).map(myAppUserMapper::toDto);
    }

    /**
     * Get one myAppUser by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MyAppUserDTO> findOne(Long id) {
        LOG.debug("Request to get MyAppUser : {}", id);
        return myAppUserRepository.findById(id).map(myAppUserMapper::toDto);
    }

    /**
     * Delete the myAppUser by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete MyAppUser : {}", id);
        myAppUserRepository.deleteById(id);
    }
}
