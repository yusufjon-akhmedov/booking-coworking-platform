package uz.yusufjon.coworkingbooking.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import uz.yusufjon.coworkingbooking.booking.dto.BookingDetailResponse;
import uz.yusufjon.coworkingbooking.booking.dto.BookingResponse;
import uz.yusufjon.coworkingbooking.booking.dto.CancelBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.CreateBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.RescheduleBookingRequest;
import uz.yusufjon.coworkingbooking.booking.entity.Booking;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.booking.repository.BookingRepository;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.common.exception.BookingConflictException;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBookingSavesConfirmedBookingForAuthenticatedUser() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(2);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(7L);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setNotes("Quiet work block");

        User user = customer(1L, "alice@example.com");
        Room room = activeRoom(7L, "Focus Room");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsConflictingBooking(eq(7L), eq(startTime), eq(endTime), anyList())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(99L);
            booking.setCreatedAt(LocalDateTime.now());
            return booking;
        });

        BookingResponse response = bookingService.createBooking(1L, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());

        Booking savedBooking = bookingCaptor.getValue();
        assertThat(savedBooking.getUser()).isEqualTo(user);
        assertThat(savedBooking.getRoom()).isEqualTo(room);
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRoomId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED.name());
    }

    @Test
    void createBookingWhenTimeRangeIsInvalidThrowsBadRequestException() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(7L);
        request.setStartTime(startTime);
        request.setEndTime(startTime.minusMinutes(30));

        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("End time must be after start time");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void createBookingWhenRoomIsInactiveThrowsBadRequestException() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(7L);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "alice@example.com")));
        when(roomRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(room(7L, "Focus Room", false)));

        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room is not active");
    }

    @Test
    void createBookingWhenConflictExistsThrowsBookingConflictException() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(7L);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "alice@example.com")));
        when(roomRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(activeRoom(7L, "Focus Room")));
        when(bookingRepository.existsConflictingBooking(eq(7L), eq(startTime), eq(endTime), anyList())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Selected time slot is already booked");
    }

    @Test
    void cancelBookingAllowsOwnerToCancelFutureBooking() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(4);
        Booking booking = booking(10L, 1L, BookingStatus.CONFIRMED, startTime, startTime.plusHours(1));
        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason("Plans changed");

        when(bookingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(1L, 10L, request);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED.name());
        assertThat(booking.getCancellationReason()).isEqualTo("Plans changed");
        assertThat(booking.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelBookingWhenAlreadyCancelledThrowsBadRequestException() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(4);
        Booking booking = booking(10L, 1L, BookingStatus.CANCELLED, startTime, startTime.plusHours(1));

        when(bookingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 10L, new CancelBookingRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Booking is already cancelled");
    }

    @Test
    void cancelBookingWhenRequesterIsNotOwnerThrowsAccessDeniedException() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(4);
        Booking booking = booking(10L, 2L, BookingStatus.CONFIRMED, startTime, startTime.plusHours(1));

        when(bookingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 10L, new CancelBookingRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not allowed to manage this booking");
    }

    @Test
    void rescheduleBookingUpdatesTimesForOwner() {
        LocalDateTime originalStart = LocalDateTime.now().plusHours(5);
        Booking booking = booking(11L, 1L, BookingStatus.CONFIRMED, originalStart, originalStart.plusHours(1));
        LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime newEnd = newStart.plusHours(2);

        RescheduleBookingRequest request = new RescheduleBookingRequest();
        request.setStartTime(newStart);
        request.setEndTime(newEnd);

        when(bookingRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(booking.getRoom().getId())).thenReturn(Optional.of(booking.getRoom()));
        when(bookingRepository.existsConflictingBookingExcludingBookingId(eq(booking.getRoom().getId()), eq(11L), eq(newStart), eq(newEnd), anyList()))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.rescheduleBooking(1L, 11L, request);

        assertThat(response.getStartTime()).isEqualTo(newStart);
        assertThat(response.getEndTime()).isEqualTo(newEnd);
        assertThat(booking.getStartTime()).isEqualTo(newStart);
        assertThat(booking.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void rescheduleBookingWhenConflictExistsThrowsBookingConflictException() {
        LocalDateTime originalStart = LocalDateTime.now().plusHours(5);
        Booking booking = booking(11L, 1L, BookingStatus.CONFIRMED, originalStart, originalStart.plusHours(1));
        LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime newEnd = newStart.plusHours(1);

        RescheduleBookingRequest request = new RescheduleBookingRequest();
        request.setStartTime(newStart);
        request.setEndTime(newEnd);

        when(bookingRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(booking.getRoom().getId())).thenReturn(Optional.of(booking.getRoom()));
        when(bookingRepository.existsConflictingBookingExcludingBookingId(eq(booking.getRoom().getId()), eq(11L), eq(newStart), eq(newEnd), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.rescheduleBooking(1L, 11L, request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Selected reschedule slot is already booked");
    }

    @Test
    void getMyBookingsReturnsMappedResponsesForAuthenticatedUser() {
        LocalDateTime latestStart = LocalDateTime.now().plusDays(1);
        Booking first = booking(20L, 1L, BookingStatus.CONFIRMED, latestStart, latestStart.plusHours(1));
        Booking second = booking(21L, 1L, BookingStatus.CANCELLED, latestStart.minusDays(1), latestStart.minusDays(1).plusHours(1));

        when(bookingRepository.findAllByUserIdOrderByStartTimeDesc(1L)).thenReturn(List.of(first, second));

        List<BookingResponse> responses = bookingService.getMyBookings(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(BookingResponse::getUserId).containsOnly(1L);
        assertThat(responses).extracting(BookingResponse::getStatus)
                .containsExactly(BookingStatus.CONFIRMED.name(), BookingStatus.CANCELLED.name());
    }

    @Test
    void getBookingDetailsAsAdminCanAccessAnyBooking() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        Booking booking = booking(30L, 2L, BookingStatus.CONFIRMED, startTime, startTime.plusHours(1));
        booking.setCancellationReason("n/a");

        when(bookingRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(booking));

        BookingDetailResponse response = bookingService.getBookingDetails(1L, Role.ADMIN, 30L);

        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getUserId()).isEqualTo(2L);
    }

    @Test
    void getBookingDetailsAsDifferentCustomerThrowsAccessDeniedException() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        Booking booking = booking(30L, 2L, BookingStatus.CONFIRMED, startTime, startTime.plusHours(1));

        when(bookingRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingDetails(1L, Role.CUSTOMER, 30L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not allowed to manage this booking");
    }

    @Test
    void getBookingHistoryWhenDateRangeIsInvalidThrowsBadRequestException() {
        LocalDateTime from = LocalDateTime.now().plusDays(2);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> bookingService.getBookingHistory(1L, null, null, from, to, PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("From date must be before or equal to to date");
    }

    @Test
    void getBookingHistoryReturnsPagedMappedResponses() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        Booking booking = booking(40L, 1L, BookingStatus.CONFIRMED, startTime, startTime.plusHours(1));

        when(bookingRepository.findAllByUserIdAndFilters(eq(1L), eq(BookingStatus.CONFIRMED), eq(7L), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 20), 1));

        PageResponse<BookingResponse> response = bookingService.getBookingHistory(
                1L,
                BookingStatus.CONFIRMED,
                7L,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(40L);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    private User customer(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setFullName("Customer " + id);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return user;
    }

    private Room activeRoom(Long id, String name) {
        return room(id, name, true);
    }

    private Room room(Long id, String name, boolean active) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        room.setLocation("2nd floor");
        room.setCapacity(6);
        room.setHourlyPrice(BigDecimal.valueOf(25));
        room.setOpenTime(LocalTime.of(8, 0));
        room.setCloseTime(LocalTime.of(20, 0));
        room.setActive(active);
        return room;
    }

    private Booking booking(Long id, Long userId, BookingStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(customer(userId, "customer" + userId + "@example.com"));
        booking.setRoom(activeRoom(7L, "Focus Room"));
        booking.setStatus(status);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setNotes("Focus session");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }
}
