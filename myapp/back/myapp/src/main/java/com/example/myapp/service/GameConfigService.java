package com.example.myapp.service;

import com.example.myapp.contants.enumeration.GameConfigType;
import com.example.myapp.domain.GameConfig;
import com.example.myapp.repository.GameConfigRepository;
import com.example.myapp.service.dto.GameConfigDTO;
import com.example.myapp.service.mapper.GameConfigMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class GameConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(GameConfigService.class);

    private static final String ENTITY_NAME = "gameConfig";

    private final GameConfigRepository gameConfigRepository;

    private final GameConfigMapper gameConfigMapper;

    public GameConfigService(GameConfigRepository gameConfigRepository, GameConfigMapper gameConfigMapper) {
        this.gameConfigRepository = gameConfigRepository;
        this.gameConfigMapper = gameConfigMapper;
    }

    public GameConfigDTO save(GameConfigDTO gameConfigDTO) {
        LOG.debug("Request to save GameConfig : {}", gameConfigDTO);
        normalize(gameConfigDTO);
        if (gameConfigRepository.existsByTypeAndNameIgnoreCase(gameConfigDTO.getType(), gameConfigDTO.getName())) {
            throw new BadRequestAlertException("Name already exists", ENTITY_NAME, "nameexists");
        }
        GameConfig gameConfig = gameConfigMapper.toEntity(gameConfigDTO);
        String username = SecurityUtil.getCurrentUsername();
        gameConfig.setCreatedBy(username);
        gameConfig.setLastModifiedBy(username);
        gameConfig = gameConfigRepository.save(gameConfig);
        return gameConfigMapper.toDto(gameConfig);
    }

    public GameConfigDTO update(GameConfigDTO gameConfigDTO) {
        LOG.debug("Request to update GameConfig : {}", gameConfigDTO);
        normalize(gameConfigDTO);
        GameConfig existing = gameConfigRepository.findById(gameConfigDTO.getId())
            .orElseThrow(() -> new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnotfound"));
        boolean duplicate = gameConfigRepository.findByTypeOrderBySortOrderAscIdAsc(gameConfigDTO.getType())
            .stream()
            .anyMatch(item -> !Objects.equals(item.getId(), gameConfigDTO.getId())
                && item.getName().equalsIgnoreCase(gameConfigDTO.getName()));
        if (duplicate) {
            throw new BadRequestAlertException("Name already exists", ENTITY_NAME, "nameexists");
        }
        existing.setType(gameConfigDTO.getType());
        existing.setName(gameConfigDTO.getName());
        existing.setValue(gameConfigDTO.getValue());
        existing.setDescription(gameConfigDTO.getDescription());
        existing.setSortOrder(gameConfigDTO.getSortOrder());
        existing.setLastModifiedBy(SecurityUtil.getCurrentUsername());
        return gameConfigMapper.toDto(gameConfigRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public List<GameConfigDTO> findAll(Optional<GameConfigType> type) {
        LOG.debug("Request to get GameConfig list by type : {}", type);
        if (type.isPresent()) {
            return gameConfigMapper.toDto(gameConfigRepository.findByTypeOrderBySortOrderAscIdAsc(type.get()));
        }
        return gameConfigMapper.toDto(gameConfigRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<GameConfigDTO> findOne(Long id) {
        LOG.debug("Request to get GameConfig : {}", id);
        return gameConfigRepository.findById(id).map(gameConfigMapper::toDto);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete GameConfig : {}", id);
        gameConfigRepository.deleteById(id);
    }

    private void normalize(GameConfigDTO dto) {
        dto.setName(trim(dto.getName()));
        dto.setValue(trim(dto.getValue()));
        dto.setDescription(trim(dto.getDescription()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
