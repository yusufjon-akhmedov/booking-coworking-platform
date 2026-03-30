package uz.yusufjon.coworkingbooking.booking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.coworkingbooking.booking.entity.Booking;
import uz.yusufjon.coworkingbooking.booking.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    @EntityGraph(attributePaths = {"user", "room"})
    @Query("""
            select b
            from Booking b
            where b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            select b
            from Booking b
            join fetch b.user
            join fetch b.room
            where b.user.id = :userId
            order by b.startTime desc
            """)
    List<Booking> findAllByUserIdOrderByStartTimeDesc(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "room"})
    @Query(
            value = """
                    select b
                    from Booking b
                    where (:status is null or b.status = :status)
                      and (:roomId is null or b.room.id = :roomId)
                      and (:from is null or b.startTime >= :from)
                      and (:to is null or b.endTime <= :to)
                    """,
            countQuery = """
                    select count(b)
                    from Booking b
                    where (:status is null or b.status = :status)
                      and (:roomId is null or b.room.id = :roomId)
                      and (:from is null or b.startTime >= :from)
                      and (:to is null or b.endTime <= :to)
                    """
    )
    Page<Booking> findAllByFilters(
            @Param("status") BookingStatus status,
            @Param("roomId") Long roomId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "room"})
    @Query(
            value = """
                    select b
                    from Booking b
                    where b.user.id = :userId
                      and (:status is null or b.status = :status)
                      and (:roomId is null or b.room.id = :roomId)
                      and (:from is null or b.startTime >= :from)
                      and (:to is null or b.endTime <= :to)
                    """,
            countQuery = """
                    select count(b)
                    from Booking b
                    where b.user.id = :userId
                      and (:status is null or b.status = :status)
                      and (:roomId is null or b.room.id = :roomId)
                      and (:from is null or b.startTime >= :from)
                      and (:to is null or b.endTime <= :to)
                    """
    )
    Page<Booking> findAllByUserIdAndFilters(
            @Param("userId") Long userId,
            @Param("status") BookingStatus status,
            @Param("roomId") Long roomId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update Booking b
            set b.status = :completedStatus,
                b.updatedAt = :updatedAt
            where b.status = :confirmedStatus
              and b.endTime <= :currentTime
            """)
    int markCompletedBookings(
            @Param("confirmedStatus") BookingStatus confirmedStatus,
            @Param("completedStatus") BookingStatus completedStatus,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("updatedAt") LocalDateTime updatedAt
    );

}
