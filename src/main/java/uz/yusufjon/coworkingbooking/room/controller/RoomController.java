package uz.yusufjon.coworkingbooking.room.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.service.RoomService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

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