package uz.yusufjon.coworkingbooking.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class BookingMetrics {

     /** Total number of bookings ever created */
    private final Counter bookingCreatedCounter;

    /** Total number of bookings that were cancelled */
    private final Counter bookingCanceledCounter;

    /** Total number of bookings auto-completed by the scheduler */
    private final Counter bookingCompletedCounter;

    /** Total number of reschedule operations */
    private final Counter bookingRescheduledCounter;


    private final AtomicLong activeBookingCount = new AtomicLong(0);

    private final Timer bookingCreationTimer;



    public BookingMetrics(MeterRegistry registry) {

        this.bookingCreatedCounter = Counter.builder("booking.created")
                .description("Total bookings Created")
                .register(registry);

        this.bookingCanceledCounter = Counter.builder("booking.created")
                .description("Total bookings Created")
                .register(registry);

        this.bookingCompletedCounter = Counter.builder("booking.created")
                .description("Total bookings Created")
                .register(registry);

        this.bookingRescheduledCounter = Counter.builder("booking.created")
                .description("Total bookings Created")
                .register(registry);


        Gauge.builder("booking.active.count", activeBookingCount, AtomicLong::get)
                .description("Current number of CONFIRMED (active) bookings")
                .register(registry);

        this.bookingCreationTimer = Timer.builder("booking.creation.duration")
                .description("Time taken to complete the booking creation flow")
                .register(registry);
    }


    public void bookingCreated() {
        bookingCreatedCounter.increment();
        activeBookingCount.incrementAndGet();
    }

    public void bookingCanceled() {
        bookingCanceledCounter.increment();
        activeBookingCount.decrementAndGet();
    }

    public void bookingCompleted() {
        bookingCompletedCounter.increment();
        activeBookingCount.decrementAndGet();
    }

    public void bookingRescheduled() {
        bookingRescheduledCounter.increment();
    }

    public Timer getCreationTimer() {
        return bookingCreationTimer;
    }
}
