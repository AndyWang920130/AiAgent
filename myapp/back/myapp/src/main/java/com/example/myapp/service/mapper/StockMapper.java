package com.example.myapp.service.mapper;

import com.example.myapp.domain.Stock;
import com.example.myapp.service.dto.StockDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the entity {@link Stock} and its DTO {@link StockDTO}. Only the
 * entity-to-DTO direction is generated: stocks are created from
 * {@link com.example.myapp.web.rest.vm.AddStockRequest} and every other field is
 * written from quote data, never from client input. {@code historyCount} and
 * {@code addedDate} belong to the caller's watch rather than the shared stock row, so
 * the service fills them in per request.
 */
@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "historyCount", ignore = true)
    @Mapping(target = "addedDate", ignore = true)
    StockDTO toDto(Stock entity);
}
