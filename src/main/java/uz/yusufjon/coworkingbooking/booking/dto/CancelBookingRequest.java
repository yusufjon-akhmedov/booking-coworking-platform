package uz.yusufjon.coworkingbooking.booking.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequest {

    @Size(max = 255, message = "Cancellation reason must be at most 255 characters")
    private String reason;
}