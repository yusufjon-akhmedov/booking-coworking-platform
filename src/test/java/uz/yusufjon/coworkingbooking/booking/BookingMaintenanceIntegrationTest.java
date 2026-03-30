package uz.yusufjon.coworkingbooking.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uz.yusufjon.coworkingbooking.booking.entity.Booking;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.support.AbstractPostgresIntegrationTest;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookingMaintenanceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void markCompletedBookingsUpdatesOnlyEligibleBookings() {
        User user = createPersistedUser("Scheduler Customer", uniqueEmail("scheduler"), "Secret123", Role.CUSTOMER, true);
        Room room = roomByName("Ocean Room");
        LocalDateTime now = LocalDateTime.now();

        Booking pastConfirmed = bookingRepository.save(booking(user, room, now.minusHours(2), now.minusHours(1), BookingStatus.CONFIRMED));
        Booking futureConfirmed = bookingRepository.save(booking(user, room, now.plusHours(1), now.plusHours(2), BookingStatus.CONFIRMED));
        Booking pastCancelled = bookingRepository.save(booking(user, room, now.minusHours(3), now.minusHours(2), BookingStatus.CANCELLED));

        bookingMaintenanceService.markCompletedBookings();

        assertThat(bookingRepository.findById(pastConfirmed.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(bookingRepository.findById(futureConfirmed.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingRepository.findById(pastCancelled.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    private Booking booking(User user, Room room, LocalDateTime startTime, LocalDateTime endTime, BookingStatus status) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(status);
        booking.setNotes("Scheduler test booking");
        return booking;
    }
}
