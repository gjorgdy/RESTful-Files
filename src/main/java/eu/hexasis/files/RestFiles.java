package eu.hexasis.files;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
public class RestFiles {

	public static final String BASE_URL;
	private static final String AUTH_KEY;

	static {
		Dotenv dotenv = Dotenv.load();
		BASE_URL = dotenv.get("BASE_URL");
		AUTH_KEY = dotenv.get("AUTH_KEY");
	}

	public static void main(String[] args) {
		SpringApplication.run(RestFiles.class, args);
	}

	public static boolean isValidAuthKey(String in) {
		return AUTH_KEY != null && AUTH_KEY.equals(in);
	}

}