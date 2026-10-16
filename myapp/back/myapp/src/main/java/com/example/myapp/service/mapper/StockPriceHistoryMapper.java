package com.example.myapp.service.mapper;

import com.example.myapp.domain.StockPriceHistory;
import com.example.myapp.service.dto.StockPriceHistoryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper for the entity {@link StockPriceHistory} and its DTO
 * {@link StockPriceHistoryDTO}. Read-only: history rows are appended by
 * {@link com.example.myapp.service.StockService} from quote data, never from
 * client input, so no DTO-to-entity direction is generated.
 */
@Mapper(componentModel = "spring")
public interface StockPriceHistoryMapper {

    @Mapping(target = "stockId", source = "stock.id")
    StockPriceHistoryDTO toDto(StockPriceHistory entity);

    List<StockPriceHistoryDTO> toDto(List<StockPriceHistory> entityList);
}
