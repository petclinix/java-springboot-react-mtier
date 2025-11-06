package tech.petclinix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	private static final String template = "Hello, %s!";

	@RestController
	public static class GreetingController {
		@GetMapping("/hello")
		public Greeting hello(@RequestParam(defaultValue = "World") String name) {
			return new Greeting(String.format(template, name));
		}

		public record Greeting(String content) {
		}
	}
}
