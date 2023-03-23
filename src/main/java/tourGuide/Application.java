package tourGuide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        java.util.Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(Application.class, args);
    }
}
