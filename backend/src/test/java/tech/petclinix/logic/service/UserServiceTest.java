package tech.petclinix.logic.service;

import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;
import tech.petclinix.persistence.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserJpaRepository jpa;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<UserEntity> userEntityCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(jpa, passwordEncoder);
    }

    @Test
    void authenticate_success_whenPasswordMatches() {
        // arrange
        String username = "alice";
        String storedHash = "$2a$10$..."; // fake bcrypt hash
        var entity = new UserEntity(username, storedHash);

        when(jpa.findByUsername(username)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("plaintext", storedHash)).thenReturn(true);

        // act
        boolean ok = userService.authenticate(username, "plaintext");

        // assert
        assertThat(ok).isTrue();
        verify(jpa).findByUsername(username);
        verify(passwordEncoder).matches("plaintext", storedHash);
    }

    @Test
    void authenticate_fails_whenPasswordDoesNotMatch() {
        // arrange
        String username = "bob";
        String storedHash = "$2a$10$abc";
        var entity = new UserEntity(username, storedHash);

        when(jpa.findByUsername(username)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrongpw", storedHash)).thenReturn(false);

        // act
        boolean ok = userService.authenticate(username, "wrongpw");

        // assert
        assertThat(ok).isFalse();
        verify(jpa).findByUsername(username);
        verify(passwordEncoder).matches("wrongpw", storedHash);
    }

    @Test
    void authenticate_fails_whenUserNotFound() {
        //arrange
        when(jpa.findByUsername("missing")).thenReturn(Optional.empty());

        //act
        boolean ok = userService.authenticate("missing", "any");

        //assert
        assertThat(ok).isFalse();
        verify(jpa).findByUsername("missing");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void findByUsername_mapsEntityToDomain() {
        //arrange
        String username = "charlie";
        String hash = "hash";
        var entity = new UserEntity(username, hash);

        when(jpa.findByUsername(username)).thenReturn(Optional.of(entity));

        //act
        var maybeDomain = userService.findByUsername(username);

        //assert
        assertThat(maybeDomain).isPresent();
        var domain = maybeDomain.get();
        assertThat(domain.username()).isEqualTo(username);
        assertThat(domain.passwordHash()).isEqualTo(hash);

        verify(jpa).findByUsername(username);
    }

    @Test
    void register_encodesPassword_andPersistsEntity() {
        //arrange
        String username = "newuser";
        String raw = "secret";
        String encoded = "encoded-secret";

        when(passwordEncoder.encode(raw)).thenReturn(encoded);

        // when save is called, emulate JPA returning the same entity (id may be null in this simplified test)
        when(jpa.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        var domain = userService.register(username, raw);

        // assert
        assertThat(domain).isNotNull();
        assertThat(domain.username()).isEqualTo(username);
        assertThat(domain.passwordHash()).isEqualTo(encoded);

        // verify we saved an entity with the encoded password
        verify(jpa).save(userEntityCaptor.capture());
        var savedEntity = userEntityCaptor.getValue();
        assertThat(savedEntity.getUsername()).isEqualTo(username);
        assertThat(savedEntity.getPasswordHash()).isEqualTo(encoded);

        verify(passwordEncoder).encode(raw);
    }
}
