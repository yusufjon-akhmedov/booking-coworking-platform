package uz.yusufjon.coworkingbooking.room.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<AvailableRoomResponse> getAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        List<Room> rooms = roomRepository.findAvailableRooms(
                startTime,
                endTime,
                startTime.toLocalTime(),
                endTime.toLocalTime(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        return rooms.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Start time must be in the future");
        }

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        if (!startDate.equals(endDate)) {
            throw new BadRequestException("Search must be within the same day");
        }
    }

    private AvailableRoomResponse mapToResponse(Room room) {
        return AvailableRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .location(room.getLocation())
                .capacity(room.getCapacity())
                .hourlyPrice(room.getHourlyPrice())
                .openTime(room.getOpenTime())
                .closeTime(room.getCloseTime())
                .build();
    }
}