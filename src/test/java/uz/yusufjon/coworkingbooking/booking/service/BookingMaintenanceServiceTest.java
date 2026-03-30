package uz.yusufjon.coworkingbooking.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.booking.repository.BookingRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingMaintenanceServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void markCompletedBookingsPromotesConfirmedBookingsUsingCurrentTimestamp() {
        bookingMaintenanceService.markCompletedBookings();

        ArgumentCaptor<LocalDateTime> currentTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> updatedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(bookingRepository).markCompletedBookings(
                org.mockito.ArgumentMatchers.eq(BookingStatus.CONFIRMED),
                org.mockito.ArgumentMatchers.eq(BookingStatus.COMPLETED),
                currentTimeCaptor.capture(),
                updatedAtCaptor.capture()
        );

        assertThat(updatedAtCaptor.getValue()).isEqualTo(currentTimeCaptor.getValue());
        assertThat(currentTimeCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
