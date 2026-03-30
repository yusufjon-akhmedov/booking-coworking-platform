package uz.yusufjon.coworkingbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.booking.repository.BookingRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingMaintenanceService {

    private final BookingRepository bookingRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void markCompletedBookings() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.markCompletedBookings(
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED,
                now,
                now
        );
    }
}
