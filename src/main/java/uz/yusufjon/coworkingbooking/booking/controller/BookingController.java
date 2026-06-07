package uz.yusufjon.coworkingbooking.booking.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springdoc.core.annotations.ParameterObject;
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
import uz.yusufjon.coworkingbooking.common.openapi.AdminOnlyApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CancelBookingApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CommonApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CreateBookingApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CustomerOnlyApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.UpdateBookingApiResponses;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking creation, management, history, and detail endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a booking", description = "Creates a booking for the authenticated customer if the selected room and time slot are available.")
    @CreateBookingApiResponses
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
    @Operation(summary = "List my bookings", description = "Returns all bookings that belong to the authenticated customer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bookings fetched successfully")
    @CustomerOnlyApiResponses
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<BookingResponse> response = bookingService.getMyBookings(currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>("My bookings fetched successfully", response));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all bookings for admins", description = "Returns a paginated booking list with optional filters by status, room, and time range. Admin only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bookings fetched successfully")
    @AdminOnlyApiResponses
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getAdminBookings(
            @Parameter(description = "Filter by booking status") @RequestParam(required = false) BookingStatus status,
            @Parameter(description = "Filter by room id") @RequestParam(required = false) Long roomId,
            @Parameter(description = "Filter bookings that start on or after this timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Filter bookings that end on or before this timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<BookingResponse> response = bookingService.getAdminBookings(status, roomId, from, to, pageable);
        return ResponseEntity.ok(new ApiResponse<>("Bookings fetched successfully", response));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my booking history", description = "Returns a paginated booking history for the authenticated customer with optional filters.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking history fetched successfully")
    @CustomerOnlyApiResponses
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getBookingHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Filter by booking status") @RequestParam(required = false) BookingStatus status,
            @Parameter(description = "Filter by room id") @RequestParam(required = false) Long roomId,
            @Parameter(description = "Filter bookings that start on or after this timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Filter bookings that end on or before this timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
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
    @Operation(summary = "Get booking details", description = "Returns detailed booking information. Customers can only access their own bookings; admins can access any booking.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking details fetched successfully")
    @CommonApiResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content)
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
    @Operation(summary = "Cancel my booking", description = "Cancels a booking that belongs to the authenticated customer.")
    @CancelBookingApiResponses
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
    @Operation(summary = "Reschedule my booking", description = "Moves a booking that belongs to the authenticated customer to a new valid time slot.")
    @UpdateBookingApiResponses
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleBookingRequest request
    ) {
        BookingResponse response = bookingService.rescheduleBooking(currentUser.getUser().getId(), bookingId, request);
        return ResponseEntity.ok(new ApiResponse<>("Booking rescheduled successfully", response));
    }
}
