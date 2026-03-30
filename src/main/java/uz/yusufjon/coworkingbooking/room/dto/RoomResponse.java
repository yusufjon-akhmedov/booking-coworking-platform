package uz.yusufjon.coworkingbooking.room.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
public class RoomResponse {

    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private BigDecimal hourlyPrice;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean active;
}
