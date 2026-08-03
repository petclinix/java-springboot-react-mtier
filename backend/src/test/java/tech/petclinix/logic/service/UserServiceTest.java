package tech.petclinix.logic.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.UserType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.AdminSelfRegistrationNotAllowedException;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<UserEntity> userEntityCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(repository, passwordEncoder, eventPublisher);
    }

    /** Returns the domain user, sets lastLogin, and publishes a USER_LOGIN event when credentials are valid. */
    @Test
    void authenticateReturnsDomainUserWhenCredentialsAreValid() {
        //arrange
        var username = new Username("alice");
        String storedHash = "$2a$10$...";
        var entity = new OwnerEntity(username.value(), storedHash);

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("plaintext", storedHash)).thenReturn(true);
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //act
        var user = userService.authenticate(username, "plaintext");

        //assert
        assertThat(user).isNotNull();
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.lastLogin()).isNotNull();
        assertThat(entity.getLastLogin()).isNotNull();
        verify(repository).findOne(any(Specification.class));
        verify(passwordEncoder).matches("plaintext", storedHash);
        verify(repository).save(entity);
        verify(eventPublisher).publishEvent(new ActionEvent(username, "USER_LOGIN"));
    }

    /** Throws InvalidCredentialsException when the password does not match, without updating lastLogin. */
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
        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(eventPublisher);
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
        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(eventPublisher);
    }

    /** Throws InvalidCredentialsException when the matching user is inactive, without updating lastLogin. */
    @Test
    void authenticateThrowsWhenUserIsInactive() {
        //arrange
        var username = new Username("dave");
        String storedHash = "$2a$10$xyz";
        var entity = new OwnerEntity(username.value(), storedHash);
        entity.setActive(false);

        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("plaintext", storedHash)).thenReturn(true);

        //act + assert
        assertThatThrownBy(() -> userService.authenticate(username, "plaintext"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(eventPublisher);
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
        verify(eventPublisher).publishEvent(new ActionEvent(new Username(username), "USER_REGISTERED"));
    }

    /** Throws AdminSelfRegistrationNotAllowedException when registering as ADMIN, without touching the repository. */
    @Test
    void registerThrowsWhenUserTypeIsAdmin() {
        //arrange
        var username = new Username("wannabe-admin");

        //act + assert
        assertThatThrownBy(() -> userService.register(username, "secret", UserType.ADMIN))
                .isInstanceOf(AdminSelfRegistrationNotAllowedException.class);
        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(eventPublisher, passwordEncoder);
    }

    /** Encodes the password and persists an ADMIN entity, bypassing the self-registration guard. */
    @Test
    void registerAdminPersistsAdminEntity() {
        //arrange
        String username = "root";
        String raw = "secret";
        String encoded = "encoded-secret";
        when(passwordEncoder.encode(raw)).thenReturn(encoded);
        when(repository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //act
        var domain = userService.registerAdmin(new Username(username), raw);

        //assert
        assertThat(domain).isNotNull();
        assertThat(domain.username()).isEqualTo(username);
        verify(eventPublisher).publishEvent(new ActionEvent(new Username(username), "USER_REGISTERED"));
    }

    /** Deactivates the user and publishes a USER_DEACTIVATED event attributed to the acting admin. */
    @Test
    void deactivateSetsInactiveAndPublishesEvent() {
        //arrange
        var adminUsername = new Username("admin");
        var entity = new OwnerEntity("alice", "hash");

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //act
        var result = userService.deactivate(adminUsername, 1L);

        //assert
        assertThat(result.active()).isFalse();
        assertThat(entity.isActive()).isFalse();
        verify(eventPublisher).publishEvent(new ActionEvent(adminUsername, "USER_DEACTIVATED"));
    }

    /** Throws NotFoundException when deactivating a user id that does not exist. */
    @Test
    void deactivateThrowsNotFoundWhenUserDoesNotExist() {
        //arrange
        var adminUsername = new Username("admin");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> userService.deactivate(adminUsername, 99L))
                .isInstanceOf(tech.petclinix.logic.domain.exception.NotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    /** Activates the user and publishes a USER_ACTIVATED event attributed to the acting admin. */
    @Test
    void activateSetsActiveAndPublishesEvent() {
        //arrange
        var adminUsername = new Username("admin");
        var entity = new OwnerEntity("alice", "hash");
        entity.setActive(false);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //act
        var result = userService.activate(adminUsername, 1L);

        //assert
        assertThat(result.active()).isTrue();
        assertThat(entity.isActive()).isTrue();
        verify(eventPublisher).publishEvent(new ActionEvent(adminUsername, "USER_ACTIVATED"));
    }

    /** Throws NotFoundException when activating a user id that does not exist. */
    @Test
    void activateThrowsNotFoundWhenUserDoesNotExist() {
        //arrange
        var adminUsername = new Username("admin");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        //act + assert
        assertThatThrownBy(() -> userService.activate(adminUsername, 99L))
                .isInstanceOf(tech.petclinix.logic.domain.exception.NotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }
}
