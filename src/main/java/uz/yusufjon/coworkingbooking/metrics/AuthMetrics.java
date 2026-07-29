package uz.yusufjon.coworkingbooking.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter registrationCounter;
    private final Counter logoutCounter;
    private final Counter tokenRefreshCounter;

    public AuthMetrics(MeterRegistry registry) {

        this.loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Successfull login attempts")
                .register(registry);

        this.loginFailureCounter = Counter.builder("auth.login.failure")
                .description("Failed login attempts (bad credentials / disabled account)")
                .register(registry);

        this.registrationCounter = Counter.builder("auth.registration")
                .description("New user registration")
                .register(registry);

        this.logoutCounter = Counter.builder("auth.logout")
                .description("Logout operations (refresh token revoked")
                .register(registry);

        this.tokenRefreshCounter = Counter.builder("auth.token.refresh")
                .description("JWT access token refreshes")
                .register(registry);
    }

    public void loginSuccess() {loginSuccessCounter.increment();}
    public void loginFailure() {loginFailureCounter.increment();}
    public void userRegistered() {registrationCounter.increment();}
    public void userLoggedOut() {logoutCounter.increment();}
    public void tokenRefreshed() {tokenRefreshCounter.increment();}
}
