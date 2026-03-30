package uz.yusufjon.coworkingbooking.room.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.CreateRoomRequest;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional
    public AvailableRoomResponse createRoom(CreateRoomRequest request) {
        validateRoomRequest(request);

        if (roomRepository.existsByName(request.getName())) {
            throw new BadRequestException("Room name is already in use");
        }

        Room room = new Room();
        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setHourlyPrice(request.getHourlyPrice());
        room.setOpenTime(request.getOpenTime());
        room.setCloseTime(request.getCloseTime());
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);
        return mapToResponse(savedRoom);
    }

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

    private void validateRoomRequest(CreateRoomRequest request) {
        if (!request.getCloseTime().isAfter(request.getOpenTime())) {
            throw new BadRequestException("Close time must be after open time");
        }
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
