package uz.yusufjon.coworkingbooking.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "Current page content")
    private List<T> content;
    @Schema(description = "Zero-based page index", example = "0")
    private int page;
    @Schema(description = "Requested page size", example = "20")
    private int size;
    @Schema(description = "Total number of elements", example = "15")
    private long totalElements;
    @Schema(description = "Total number of pages", example = "1")
    private int totalPages;
    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;
    @Schema(description = "Whether this is the last page", example = "true")
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
