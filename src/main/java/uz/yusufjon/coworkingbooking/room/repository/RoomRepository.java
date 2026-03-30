package uz.yusufjon.coworkingbooking.room.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;
import uz.yusufjon.coworkingbooking.room.entity.Room;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select r
            from Room r
            where r.active = true
              and r.openTime <= :startLocalTime
              and r.closeTime >= :endLocalTime
              and not exists (
                    select b.id
                    from Booking b
                    where b.room = r
                      and b.status in :statuses
                      and b.startTime < :endTime
                      and b.endTime > :startTime
              )
            order by r.id
            """)
    List<Room> findAvailableRooms(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("startLocalTime") LocalTime startLocalTime,
            @Param("endLocalTime") LocalTime endLocalTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}