package uz.yusufjon.coworkingbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.booking.dto.BookingResponse;
import uz.yusufjon.coworkingbooking.booking.dto.CancelBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.CreateBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.RescheduleBookingRequest;
import uz.yusufjon.coworkingbooking.booking.entity.Booking;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.booking.repository.BookingRepository;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.common.exception.BookingConflictException;
import uz.yusufjon.coworkingbooking.common.exception.ResourceNotFoundException;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        validateRequestTimes(request.getStartTime(), request.getEndTime());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId()));

        validateRoomState(room);
        validateBookingWithinWorkingHours(room, request.getStartTime(), request.getEndTime());

        boolean conflictExists = bookingRepository.existsConflictingBooking(
                room.getId(),
                request.getStartTime(),
                request.getEndTime(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (conflictExists) {
            throw new BookingConflictException("Selected time slot is already booked");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setNotes(request.getNotes());
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, CancelBookingRequest request) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Completed booking cannot be cancelled");
        }

        if (!booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Started or past booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(request.getReason());

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    @Transactional
    public BookingResponse rescheduleBooking(Long bookingId, RescheduleBookingRequest request) {
        validateRequestTimes(request.getStartTime(), request.getEndTime());

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cancelled booking cannot be rescheduled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Completed booking cannot be rescheduled");
        }

        if (!booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Started or past booking cannot be rescheduled");
        }

        Room room = roomRepository.findByIdForUpdate(booking.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + booking.getRoom().getId()));

        validateRoomState(room);
        validateBookingWithinWorkingHours(room, request.getStartTime(), request.getEndTime());

        boolean conflictExists = bookingRepository.existsConflictingBookingExcludingBookingId(
                room.getId(),
                booking.getId(),
                request.getStartTime(),
                request.getEndTime(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (conflictExists) {
            throw new BookingConflictException("Selected reschedule slot is already booked");
        }

        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    private void validateRequestTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        if (!startDate.equals(endDate)) {
            throw new BadRequestException("Booking must start and end on the same day");
        }
    }

    private void validateRoomState(Room room) {
        if (!Boolean.TRUE.equals(room.getActive())) {
            throw new BadRequestException("Room is not active");
        }
    }

    private void validateBookingWithinWorkingHours(Room room, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.toLocalTime().isBefore(room.getOpenTime())) {
            throw new BadRequestException("Booking start time is before room open time");
        }

        if (endTime.toLocalTime().isAfter(room.getCloseTime())) {
            throw new BadRequestException("Booking end time is after room close time");
        }
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .roomId(booking.getRoom().getId())
                .roomName(booking.getRoom().getName())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}