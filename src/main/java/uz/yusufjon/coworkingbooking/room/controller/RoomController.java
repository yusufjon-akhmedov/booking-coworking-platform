package uz.yusufjon.coworkingbooking.room.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.CreateRoomRequest;
import uz.yusufjon.coworkingbooking.room.service.RoomService;

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
}
