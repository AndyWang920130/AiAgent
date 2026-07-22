package com.example.myapp.web.rest;

import com.example.myapp.contants.enumeration.GameConfigType;
import com.example.myapp.service.GameConfigService;
import com.example.myapp.service.dto.GameConfigDTO;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class GameConfigResource {

    private static final Logger LOG = LoggerFactory.getLogger(GameConfigResource.class);

    private static final String ENTITY_NAME = "gameConfig";

    private final GameConfigService gameConfigService;

    public GameConfigResource(GameConfigService gameConfigService) {
        this.gameConfigService = gameConfigService;
    }

    @PostMapping("/game-configs")
    public ResponseEntity<GameConfigDTO> createGameConfig(@Valid @RequestBody GameConfigDTO gameConfigDTO) throws URISyntaxException {
        LOG.debug("REST request to save GameConfig : {}", gameConfigDTO);
        if (gameConfigDTO.getId() != null) {
            throw new BadRequestAlertException("A new game config cannot already have an ID", ENTITY_NAME, "idexists");
        }
        GameConfigDTO result = gameConfigService.save(gameConfigDTO);
        return ResponseEntity.created(new URI("/api/v1/game-configs/" + result.getId())).body(result);
    }

    @PutMapping("/game-configs/{id}")
    public ResponseEntity<GameConfigDTO> updateGameConfig(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody GameConfigDTO gameConfigDTO
    ) {
        LOG.debug("REST request to update GameConfig : {}, {}", id, gameConfigDTO);
        if (gameConfigDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, gameConfigDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        return ResponseEntity.ok(gameConfigService.update(gameConfigDTO));
    }

    @GetMapping("/game-configs")
    public ResponseEntity<List<GameConfigDTO>> getAllGameConfigs(@RequestParam(required = false) GameConfigType type) {
        LOG.debug("REST request to get GameConfig list by type : {}", type);
        return ResponseEntity.ok(gameConfigService.findAll(Optional.ofNullable(type)));
    }

    @GetMapping("/game-configs/{id}")
    public ResponseEntity<GameConfigDTO> getGameConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to get GameConfig : {}", id);
        return gameConfigService.findOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/game-configs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteGameConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete GameConfig : {}", id);
        gameConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
