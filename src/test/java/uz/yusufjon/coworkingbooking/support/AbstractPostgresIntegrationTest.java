package uz.yusufjon.coworkingbooking.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import uz.yusufjon.coworkingbooking.booking.repository.BookingRepository;
import uz.yusufjon.coworkingbooking.booking.service.BookingMaintenanceService;
import uz.yusufjon.coworkingbooking.room.entity.Room;
import uz.yusufjon.coworkingbooking.room.repository.RoomRepository;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

import java.util.UUID;

@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("coworking_booking_test")
            .withUsername("postgres")
            .withPassword("postgres");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected BookingMaintenanceService bookingMaintenanceService;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
    }

    protected User createPersistedUser(String fullName, String email, String rawPassword, Role role, boolean enabled) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    protected Room roomByName(String name) {
        return roomRepository.findAll().stream()
                .filter(room -> name.equals(room.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Room not found: " + name));
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "") + "@example.com";
    }
}
