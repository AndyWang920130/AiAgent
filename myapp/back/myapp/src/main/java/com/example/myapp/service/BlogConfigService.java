package com.example.myapp.service;

import com.example.myapp.contants.enumeration.BlogConfigType;
import com.example.myapp.domain.BlogConfig;
import com.example.myapp.repository.BlogConfigRepository;
import com.example.myapp.service.dto.BlogConfigDTO;
import com.example.myapp.service.mapper.BlogConfigMapper;
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
public class BlogConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogConfigService.class);

    private static final String ENTITY_NAME = "blogConfig";

    private final BlogConfigRepository blogConfigRepository;

    private final BlogConfigMapper blogConfigMapper;

    public BlogConfigService(BlogConfigRepository blogConfigRepository, BlogConfigMapper blogConfigMapper) {
        this.blogConfigRepository = blogConfigRepository;
        this.blogConfigMapper = blogConfigMapper;
    }

    public BlogConfigDTO save(BlogConfigDTO blogConfigDTO) {
        LOG.debug("Request to save BlogConfig : {}", blogConfigDTO);
        normalize(blogConfigDTO);
        if (blogConfigRepository.existsByTypeAndNameIgnoreCase(blogConfigDTO.getType(), blogConfigDTO.getName())) {
            throw new BadRequestAlertException("Name already exists", ENTITY_NAME, "nameexists");
        }
        BlogConfig blogConfig = blogConfigMapper.toEntity(blogConfigDTO);
        String username = SecurityUtil.getCurrentUsername();
        blogConfig.setCreatedBy(username);
        blogConfig.setLastModifiedBy(username);
        blogConfig = blogConfigRepository.save(blogConfig);
        return blogConfigMapper.toDto(blogConfig);
    }

    public BlogConfigDTO update(BlogConfigDTO blogConfigDTO) {
        LOG.debug("Request to update BlogConfig : {}", blogConfigDTO);
        normalize(blogConfigDTO);
        BlogConfig existing = blogConfigRepository.findById(blogConfigDTO.getId())
            .orElseThrow(() -> new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnotfound"));
        boolean duplicate = blogConfigRepository.findByTypeOrderBySortOrderAscIdAsc(blogConfigDTO.getType())
            .stream()
            .anyMatch(item -> !Objects.equals(item.getId(), blogConfigDTO.getId())
                && item.getName().equalsIgnoreCase(blogConfigDTO.getName()));
        if (duplicate) {
            throw new BadRequestAlertException("Name already exists", ENTITY_NAME, "nameexists");
        }
        existing.setType(blogConfigDTO.getType());
        existing.setName(blogConfigDTO.getName());
        existing.setValue(blogConfigDTO.getValue());
        existing.setDescription(blogConfigDTO.getDescription());
        existing.setSortOrder(blogConfigDTO.getSortOrder());
        existing.setLastModifiedBy(SecurityUtil.getCurrentUsername());
        return blogConfigMapper.toDto(blogConfigRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public List<BlogConfigDTO> findAll(Optional<BlogConfigType> type) {
        LOG.debug("Request to get BlogConfig list by type : {}", type);
        if (type.isPresent()) {
            return blogConfigMapper.toDto(blogConfigRepository.findByTypeOrderBySortOrderAscIdAsc(type.get()));
        }
        return blogConfigMapper.toDto(blogConfigRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<BlogConfigDTO> findOne(Long id) {
        LOG.debug("Request to get BlogConfig : {}", id);
        return blogConfigRepository.findById(id).map(blogConfigMapper::toDto);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete BlogConfig : {}", id);
        blogConfigRepository.deleteById(id);
    }

    private void normalize(BlogConfigDTO dto) {
        dto.setName(trim(dto.getName()));
        dto.setValue(trim(dto.getValue()));
        dto.setDescription(trim(dto.getDescription()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
