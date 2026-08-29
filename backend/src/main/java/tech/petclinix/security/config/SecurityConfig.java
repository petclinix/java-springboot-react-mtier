package tech.petclinix.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tech.petclinix.security.jwt.JwtFilter;
import tech.petclinix.security.jwt.JwtUtil;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    // Origins allowed to call this API cross-origin (e.g. the Vite dev server on
    // localhost:3000 hitting the backend container directly, outside Docker Compose,
    // where nginx would otherwise serve everything same-origin).
    //
    // The default below is fine for local development but MUST be tightened/overridden
    // to the real frontend origin(s) for any non-local deployment. Even though this is a
    // stateless bearer-token API (no cookies, so credentialed CORS is never enabled here),
    // an overly broad allow-list is still bad practice: it lets any origin's JavaScript
    // read this API's JSON responses (Content-Type sniffing, leaked tokens in error
    // bodies, etc.) for a logged-in victim who has a valid Authorization header stashed
    // client-side.
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtUtil jwtUtil,
                           @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
        this.jwtUtil = jwtUtil;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // JwtFilter only ever reads the standard "Authorization" header; the frontend
        // ApiClient only ever sends "Content-Type" alongside it — no custom headers.
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Never allowCredentials(true): this API is stateless bearer-token auth, not
        // cookie-based, so credentialed CORS would only widen the security surface
        // without buying anything (see the CSRF-disable rationale below).
        configuration.setAllowCredentials(false);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var jwtFilter = new JwtFilter(jwtUtil);
        http
                // CSRF protection is not needed for a stateless JWT API.
                // Browsers cannot set an Authorization header cross-origin, so a
                // forged request can never carry a valid Bearer token. CSRF tokens
                // are only relevant when credentials are stored in cookies.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/users/register").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
