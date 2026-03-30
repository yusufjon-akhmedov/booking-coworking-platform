package uz.yusufjon.coworkingbooking.room.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(max = 100, message = "Room name must be at most 100 characters")
    private String name;

    @Size(max = 255, message = "Location must be at most 255 characters")
    private String location;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Hourly price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Hourly price must be non-negative")
    private BigDecimal hourlyPrice;

    @NotNull(message = "Open time is required")
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    private LocalTime closeTime;
}