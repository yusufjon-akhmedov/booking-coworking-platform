package uz.yusufjon.coworkingbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.booking.dto.BookingResponse;
import uz.yusufjon.coworkingbooking.booking.dto.CancelBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.CreateBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.RescheduleBookingRequest;
import uz.yusufjon.coworkingbooking.booking.service.BookingService;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(currentUser.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Booking created successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<BookingResponse> response = bookingService.getMyBookings(currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>("My bookings fetched successfully", response));
    }

    @PatchMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long bookingId,
            @Valid @RequestBody CancelBookingRequest request
    ) {
        BookingResponse response = bookingService.cancelBooking(currentUser.getUser().getId(), bookingId, request);
        return ResponseEntity.ok(new ApiResponse<>("Booking cancelled successfully", response));
    }

    @PatchMapping("/{bookingId}/reschedule")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleBookingRequest request
    ) {
        BookingResponse response = bookingService.rescheduleBooking(currentUser.getUser().getId(), bookingId, request);
        return ResponseEntity.ok(new ApiResponse<>("Booking rescheduled successfully", response));
    }
}
