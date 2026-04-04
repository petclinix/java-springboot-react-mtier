package tech.petclinix.logic.service;

import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.InvalidCredentialsException;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.jpa.UserJpaRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link UserService}.
 *
 * Repository and password encoder are mocked — no database or Spring context.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserJpaRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<UserEntity> userEntityCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(repository, passwordEncoder);
    }

    /** Returns the domain user when username exists and password matches. */
    @Test
    void authenticateReturnsDomainUserWhenCredentialsAreValid() {
        //arrange
        var username = new Username("alice");
        String storedHash = "$2a$10$...";
        var entity = new OwnerEntity(username.value(), storedHash);

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("plaintext", storedHash)).thenReturn(true);

        //act
        var user = userService.authenticate(username, "plaintext");

        //assert
        assertThat(user).isNotNull();
        assertThat(user.username()).isEqualTo("alice");
        verify(repository).findOne(any(Specification.class));
        verify(passwordEncoder).matches("plaintext", storedHash);
    }

    /** Throws InvalidCredentialsException when the password does not match. */
    @Test
    void authenticateThrowsWhenPasswordDoesNotMatch() {
        //arrange
        var username = new Username("bob");
        String storedHash = "$2a$10$abc";
        var entity = new OwnerEntity(username.value(), storedHash);

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrongpw", storedHash)).thenReturn(false);

        //act + assert
        assertThatThrownBy(() -> userService.authenticate(username, "wrongpw"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder).matches("wrongpw", storedHash);
    }

    /** Throws InvalidCredentialsException when the username does not exist. */
    @Test
    void authenticateThrowsWhenUserNotFound() {
        //arrange
        var username = new Username("missing");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> userService.authenticate(username, "any"))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(passwordEncoder);
    }

    /** Returns an Optional with the domain user when the username exists. */
    @Test
    void findByUsernameMapsEntityToDomain() {
        //arrange
        var username = new Username("charlie");
        var entity = new OwnerEntity(username.value(), "hash");

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));

        //act
        var maybeDomain = userService.findByUsername(username);

        //assert
        assertThat(maybeDomain).isPresent();
        assertThat(maybeDomain.get().username()).isEqualTo(username.value());
        verify(repository).findOne(any(Specification.class));
    }

    /** Encodes the password and persists a new user entity. */
    @Test
    void registerEncodesPasswordAndPersistsEntity() {
        //arrange
        String username = "newuser";
        String raw = "secret";
        String encoded = "encoded-secret";

        when(passwordEncoder.encode(raw)).thenReturn(encoded);
        when(repository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        var domain = userService.register(new Username(username), raw, UserType.OWNER);

        //assert
        assertThat(domain).isNotNull();
        assertThat(domain.username()).isEqualTo(username);

        verify(repository).save(userEntityCaptor.capture());
        var savedEntity = userEntityCaptor.getValue();
        assertThat(savedEntity.getUsername()).isEqualTo(username);
        assertThat(savedEntity.getPasswordHash()).isEqualTo(encoded);
        verify(passwordEncoder).encode(raw);
    }
}
