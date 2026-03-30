package uz.yusufjon.coworkingbooking.room.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.common.exception.ResourceNotFoundException;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.CreateRoomRequest;
import uz.yusufjon.coworkingbooking.room.dto.RoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.UpdateRoomRequest;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional
    public AvailableRoomResponse createRoom(CreateRoomRequest request) {
        validateRoomSchedule(request.getOpenTime(), request.getCloseTime());

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

    @Transactional
    public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request) {
        validateRoomSchedule(request.getOpenTime(), request.getCloseTime());

        if (roomRepository.existsByNameAndIdNot(request.getName(), roomId)) {
            throw new BadRequestException("Room name is already in use");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setHourlyPrice(request.getHourlyPrice());
        room.setOpenTime(request.getOpenTime());
        room.setCloseTime(request.getCloseTime());

        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse deactivateRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (!Boolean.TRUE.equals(room.getActive())) {
            throw new BadRequestException("Room is already inactive");
        }

        room.setActive(false);
        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse activateRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (Boolean.TRUE.equals(room.getActive())) {
            throw new BadRequestException("Room is already active");
        }

        room.setActive(true);
        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getRooms(
            Boolean active,
            Integer minCapacity,
            BigDecimal maxHourlyPrice,
            String name,
            Pageable pageable
    ) {
        validateRoomFilters(minCapacity, maxHourlyPrice);
        return PageResponse.from(
                roomRepository.findAllByFilters(active, minCapacity, maxHourlyPrice, name, pageable)
                        .map(this::mapToRoomResponse)
        );
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomDetails(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        return mapToRoomResponse(room);
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

    private void validateRoomSchedule(java.time.LocalTime openTime, java.time.LocalTime closeTime) {
        if (!closeTime.isAfter(openTime)) {
            throw new BadRequestException("Close time must be after open time");
        }
    }

    private void validateRoomFilters(Integer minCapacity, BigDecimal maxHourlyPrice) {
        if (minCapacity != null && minCapacity < 1) {
            throw new BadRequestException("Minimum capacity must be at least 1");
        }

        if (maxHourlyPrice != null && maxHourlyPrice.signum() < 0) {
            throw new BadRequestException("Maximum hourly price must be non-negative");
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

    private RoomResponse mapToRoomResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .location(room.getLocation())
                .capacity(room.getCapacity())
                .hourlyPrice(room.getHourlyPrice())
                .openTime(room.getOpenTime())
                .closeTime(room.getCloseTime())
                .active(room.getActive())
                .build();
    }
}
