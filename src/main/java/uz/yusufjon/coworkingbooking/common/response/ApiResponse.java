package uz.yusufjon.coworkingbooking.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Human-readable operation result message", example = "Booking created successfully")
    private String message;

    @Schema(description = "Response payload for the operation")
    private T data;
}
