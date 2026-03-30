package uz.yusufjon.coworkingbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.booking.dto.BookingResponse;
import uz.yusufjon.coworkingbooking.booking.dto.CancelBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.CreateBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.RescheduleBookingRequest;
import uz.yusufjon.coworkingbooking.booking.service.BookingService;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Booking created successfully", response));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody CancelBookingRequest request
    ) {
        BookingResponse response = bookingService.cancelBooking(bookingId, request);
        return ResponseEntity.ok(new ApiResponse<>("Booking cancelled successfully", response));
    }

    @PatchMapping("/{bookingId}/reschedule")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleBookingRequest request
    ) {
        BookingResponse response = bookingService.rescheduleBooking(bookingId, request);
        return ResponseEntity.ok(new ApiResponse<>("Booking rescheduled successfully", response));
    }
}