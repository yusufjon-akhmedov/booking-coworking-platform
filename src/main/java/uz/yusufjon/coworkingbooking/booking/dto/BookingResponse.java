package uz.yusufjon.coworkingbooking.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookingResponse {

    private Long id;
    private Long userId;
    private Long roomId;
    private String roomName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private LocalDateTime createdAt;
}