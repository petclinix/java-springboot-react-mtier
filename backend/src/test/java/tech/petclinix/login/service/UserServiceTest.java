package tech.petclinix.login.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        var encoder = new BCryptPasswordEncoder();
        userService = new UserService(encoder);
        userService.init(); // seeds "user" / "alice"
    }

    @Test
    void validateCredentials_correctPassword_returnsTrue() {
        //arrange+act
        var ok = userService.validateCredentials("user", "password");

        //assert
        assertThat(ok).isTrue();
    }

    @Test
    void validateCredentials_wrongPassword_returnsFalse() {
        //arrange+act
        var ok = userService.validateCredentials("user", "wrong");

        //assert
        assertThat(ok).isFalse();
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        //arrange+act
        var opt = userService.findByUsername("alice");

        //assert
        assertThat(opt).isPresent();
        assertThat(opt.get().username()).isEqualTo("alice");
    }

    @Test
    void findByUsername_nonExisting_returnsEmpty() {
        //arrange+act
        var opt = userService.findByUsername("does-not-exist");

        //assert
        assertThat(opt).isEmpty();
    }
}
