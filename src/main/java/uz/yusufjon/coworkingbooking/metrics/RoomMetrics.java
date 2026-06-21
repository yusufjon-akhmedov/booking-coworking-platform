package uz.yusufjon.coworkingbooking.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class RoomMetrics {

    private final Counter availabilitySearchCounter;
    private final Counter roomCreatedCounter;
    private final Counter roomActivatedCounter;
    private final Counter roomDeactivatedCounter;

    private final AtomicLong activeRoomCount = new AtomicLong(0);

    public RoomMetrics(MeterRegistry registry) {

        this.availabilitySearchCounter = Counter.builder("room.availability.search")
                .description("Total room availability searches by public/authenticated users")
                .register(registry);

        this.roomCreatedCounter = Counter.builder("room.created")
                .description("Total rooms created by ADMIN")
                .register(registry);

        this.roomActivatedCounter = Counter.builder("room.activated")
                .description("Total room activate operations")
                .register(registry);

        this.roomDeactivatedCounter = Counter.builder("room.deactivated")
                .description("Total room deactivate operations")
                .register(registry);

        Gauge.builder("room.active.count", activeRoomCount, AtomicLong::get)
                .description("Current number of active (bookable) rooms")
                .register(registry);
    }

    public void availabilitySearched() { availabilitySearchCounter.increment(); }
    public void roomCreated() { roomCreatedCounter.increment(); activeRoomCount.incrementAndGet(); }
    public void roomActivated() { roomActivatedCounter.increment(); activeRoomCount.incrementAndGet(); }
    public void roomDeactivated() { roomDeactivatedCounter.increment(); activeRoomCount.decrementAndGet(); }

}
