package uz.yusufjon.coworkingbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoworkingBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoworkingBookingApplication.class, args);
	}

}
