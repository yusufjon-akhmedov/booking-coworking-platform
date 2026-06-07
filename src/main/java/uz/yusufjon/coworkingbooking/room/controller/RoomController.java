package uz.yusufjon.coworkingbooking.room.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.common.openapi.AdminOnlyApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CommonApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CreateRoomApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.UpdateRoomApiResponses;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.CreateRoomRequest;
import uz.yusufjon.coworkingbooking.room.dto.RoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.UpdateRoomRequest;
import uz.yusufjon.coworkingbooking.room.service.RoomService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room availability, listing, detail, and admin management endpoints")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a room",
            description = "Creates a new room. Admin only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CreateRoomApiResponses
    public ResponseEntity<ApiResponse<AvailableRoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request
    ) {
        AvailableRoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Room created successfully", response));
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a room",
            description = "Updates room details such as pricing, capacity, and opening hours. Admin only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @UpdateRoomApiResponses
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomResponse response = roomService.updateRoom(roomId, request);
        return ResponseEntity.ok(new ApiResponse<>("Room updated successfully", response));
    }

    @PatchMapping("/{roomId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deactivate a room",
            description = "Marks a room as inactive so it no longer appears in booking availability. Admin only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room deactivated successfully")
    @AdminOnlyApiResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    public ResponseEntity<ApiResponse<RoomResponse>> deactivateRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.deactivateRoom(roomId);
        return ResponseEntity.ok(new ApiResponse<>("Room deactivated successfully", response));
    }

    @PatchMapping("/{roomId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Activate a room",
            description = "Marks a room as active again so it can be used in availability and booking flows. Admin only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room activated successfully")
    @AdminOnlyApiResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    public ResponseEntity<ApiResponse<RoomResponse>> activateRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.activateRoom(roomId);
        return ResponseEntity.ok(new ApiResponse<>("Room activated successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "List rooms",
            description = "Returns a paginated room list with optional filters for active status, capacity, hourly price, and name.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms fetched successfully")
    @CommonApiResponses
    public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> getRooms(
            @Parameter(description = "Filter by active flag") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter rooms with at least this capacity") @RequestParam(required = false) Integer minCapacity,
            @Parameter(description = "Filter rooms with hourly price less than or equal to this amount") @RequestParam(required = false) BigDecimal maxHourlyPrice,
            @Parameter(description = "Filter rooms by name keyword") @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<RoomResponse> response = roomService.getRooms(active, minCapacity, maxHourlyPrice, name, pageable);
        return ResponseEntity.ok(new ApiResponse<>("Rooms fetched successfully", response));
    }

    @GetMapping("/available")
    @Operation(summary = "Find available rooms", description = "Public endpoint that returns active rooms available for the requested time range.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Available rooms fetched successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid time range", content = @Content)
    public ResponseEntity<ApiResponse<List<AvailableRoomResponse>>> getAvailableRooms(
            @RequestParam
            @Parameter(description = "Booking start time in ISO-8601 format")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,

            @RequestParam
            @Parameter(description = "Booking end time in ISO-8601 format")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime
    ) {
        List<AvailableRoomResponse> response = roomService.getAvailableRooms(startTime, endTime);
        return ResponseEntity.ok(new ApiResponse<>("Available rooms fetched successfully", response));
    }

    @GetMapping("/{roomId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get room details",
            description = "Returns detailed information for a single room.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room details fetched successfully")
    @CommonApiResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetails(@PathVariable Long roomId) {
        RoomResponse response = roomService.getRoomDetails(roomId);
        return ResponseEntity.ok(new ApiResponse<>("Room details fetched successfully", response));
    }
}
