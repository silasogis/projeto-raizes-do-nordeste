package com.raizesdonordeste.api.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estrutura padronizada de resposta paginada da API")
public class PaginatedResponseDto<T> {

    @Schema(description = "Lista de itens retornados na página atual")
    private List<T> items;

    @Schema(description = "Número da página atual (iniciando em 0)", example = "0")
    private int page;

    @Schema(description = "Quantidade máxima de itens solicitada por página (limit)", example = "10")
    private int limit;

    @Schema(description = "Total absoluto de registros encontrados no banco", example = "24")
    private long totalItems;

    @Schema(description = "Total de páginas disponíveis", example = "3")
    private int totalPages;

    @Schema(description = "Indica se existe uma próxima página", example = "true")
    private boolean hasNext;

    @Schema(description = "Indica se existe uma página anterior", example = "false")
    private boolean hasPrevious;

    public static <T> PaginatedResponseDto<T> fromPage(Page<T> page) {
        return PaginatedResponseDto.<T>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .limit(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static <E, T> PaginatedResponseDto<T> fromPage(Page<E> page, Function<E, T> mapper) {
        List<T> mappedItems = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return PaginatedResponseDto.<T>builder()
                .items(mappedItems)
                .page(page.getNumber())
                .limit(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
