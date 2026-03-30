package uz.yusufjon.coworkingbooking.room.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.CreateRoomRequest;
import uz.yusufjon.coworkingbooking.room.dto.RoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.UpdateRoomRequest;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void getAvailableRoomsReturnsMappedActiveRooms() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);
        Room room = room(1L, "Focus Room", true);

        when(roomRepository.findAvailableRooms(
                eq(startTime),
                eq(endTime),
                eq(startTime.toLocalTime()),
                eq(endTime.toLocalTime()),
                anyList()
        )).thenReturn(List.of(room));

        List<AvailableRoomResponse> response = roomService.getAvailableRooms(startTime, endTime);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getName()).isEqualTo("Focus Room");
    }

    @Test
    void getAvailableRoomsWhenTimeRangeIsInvalidThrowsBadRequestException() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        assertThatThrownBy(() -> roomService.getAvailableRooms(startTime, startTime.minusMinutes(30)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("End time must be after start time");
    }

    @Test
    void createRoomPersistsActiveRoom() {
        CreateRoomRequest request = createRoomRequest("Quiet Pod");

        when(roomRepository.existsByName("Quiet Pod")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(5L);
            return room;
        });

        AvailableRoomResponse response = roomService.createRoom(request);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());

        Room persistedRoom = roomCaptor.getValue();
        assertThat(persistedRoom.getActive()).isTrue();
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Quiet Pod");
    }

    @Test
    void createRoomWhenScheduleIsInvalidThrowsBadRequestException() {
        CreateRoomRequest request = createRoomRequest("Quiet Pod");
        request.setOpenTime(LocalTime.of(18, 0));
        request.setCloseTime(LocalTime.of(8, 0));

        assertThatThrownBy(() -> roomService.createRoom(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Close time must be after open time");
    }

    @Test
    void updateRoomReturnsUpdatedRoomResponse() {
        UpdateRoomRequest request = updateRoomRequest("Renamed Room");
        Room room = room(5L, "Old Name", true);

        when(roomRepository.existsByNameAndIdNot("Renamed Room", 5L)).thenReturn(false);
        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.updateRoom(5L, request);

        assertThat(response.getName()).isEqualTo("Renamed Room");
        assertThat(response.getLocation()).isEqualTo("4th floor");
    }

    @Test
    void deactivateRoomMarksRoomInactive() {
        Room room = room(5L, "Quiet Pod", true);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.deactivateRoom(5L);

        assertThat(response.getActive()).isFalse();
        assertThat(room.getActive()).isFalse();
    }

    @Test
    void activateRoomMarksRoomActive() {
        Room room = room(5L, "Quiet Pod", false);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.activateRoom(5L);

        assertThat(response.getActive()).isTrue();
        assertThat(room.getActive()).isTrue();
    }

    @Test
    void getRoomsWhenFiltersAreInvalidThrowsBadRequestException() {
        assertThatThrownBy(() -> roomService.getRooms(null, 0, BigDecimal.ONE, null, PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Minimum capacity must be at least 1");
    }

    private CreateRoomRequest createRoomRequest(String name) {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName(name);
        request.setLocation("3rd floor");
        request.setCapacity(4);
        request.setHourlyPrice(BigDecimal.valueOf(20));
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(18, 0));
        return request;
    }

    private UpdateRoomRequest updateRoomRequest(String name) {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setName(name);
        request.setLocation("4th floor");
        request.setCapacity(8);
        request.setHourlyPrice(BigDecimal.valueOf(30));
        request.setOpenTime(LocalTime.of(9, 0));
        request.setCloseTime(LocalTime.of(20, 0));
        return request;
    }

    private Room room(Long id, String name, boolean active) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        room.setLocation("3rd floor");
        room.setCapacity(6);
        room.setHourlyPrice(BigDecimal.valueOf(20));
        room.setOpenTime(LocalTime.of(8, 0));
        room.setCloseTime(LocalTime.of(18, 0));
        room.setActive(active);
        return room;
    }
}
