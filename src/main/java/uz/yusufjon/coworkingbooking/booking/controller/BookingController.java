package uz.yusufjon.coworkingbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.booking.dto.BookingResponse;
import uz.yusufjon.coworkingbooking.booking.dto.BookingDetailResponse;
import uz.yusufjon.coworkingbooking.booking.dto.CancelBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.CreateBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.RescheduleBookingRequest;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.booking.service.BookingService;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import java.time.LocalDateTime;
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

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getAdminBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<BookingResponse> response = bookingService.getAdminBookings(status, roomId, from, to, pageable);
        return ResponseEntity.ok(new ApiResponse<>("Bookings fetched successfully", response));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getBookingHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<BookingResponse> response = bookingService.getBookingHistory(
                currentUser.getUser().getId(),
                status,
                roomId,
                from,
                to,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>("Booking history fetched successfully", response));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetails(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long bookingId
    ) {
        BookingDetailResponse response = bookingService.getBookingDetails(
                currentUser.getUser().getId(),
                currentUser.getUser().getRole(),
                bookingId
        );
        return ResponseEntity.ok(new ApiResponse<>("Booking details fetched successfully", response));
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
