package uz.yusufjon.coworkingbooking.room.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AvailableRoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request
    ) {
        AvailableRoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Room created successfully", response));
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomResponse response = roomService.updateRoom(roomId, request);
        return ResponseEntity.ok(new ApiResponse<>("Room updated successfully", response));
    }

    @PatchMapping("/{roomId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> deactivateRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.deactivateRoom(roomId);
        return ResponseEntity.ok(new ApiResponse<>("Room deactivated successfully", response));
    }

    @PatchMapping("/{roomId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> activateRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.activateRoom(roomId);
        return ResponseEntity.ok(new ApiResponse<>("Room activated successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> getRooms(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) BigDecimal maxHourlyPrice,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<RoomResponse> response = roomService.getRooms(active, minCapacity, maxHourlyPrice, name, pageable);
        return ResponseEntity.ok(new ApiResponse<>("Rooms fetched successfully", response));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailableRoomResponse>>> getAvailableRooms(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime
    ) {
        List<AvailableRoomResponse> response = roomService.getAvailableRooms(startTime, endTime);
        return ResponseEntity.ok(new ApiResponse<>("Available rooms fetched successfully", response));
    }

    @GetMapping("/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetails(@PathVariable Long roomId) {
        RoomResponse response = roomService.getRoomDetails(roomId);
        return ResponseEntity.ok(new ApiResponse<>("Room details fetched successfully", response));
    }
}
