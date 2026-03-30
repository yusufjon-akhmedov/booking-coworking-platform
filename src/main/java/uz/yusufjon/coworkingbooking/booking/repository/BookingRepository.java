package uz.yusufjon.coworkingbooking.booking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.coworkingbooking.booking.entity.Booking;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {


    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.room.id = :roomId
              and b.status in :statuses
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    boolean existsConflictingBooking(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.room.id = :roomId
              and b.id <> :bookingId
              and b.status in :statuses
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    boolean existsConflictingBookingExcludingBookingId(
            @Param("roomId") Long roomId,
            @Param("bookingId") Long bookingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from Booking b
            join fetch b.user
            join fetch b.room
            where b.id = :id
            """)
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);


}