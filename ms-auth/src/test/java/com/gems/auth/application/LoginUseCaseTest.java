package com.gems.auth.application;

import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.exceptions.InvalidCredentialsException;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase Tests")
class LoginUseCaseTest {

    @Mock
    private UserGateway userGateway;

    @Mock
    private PasswordEncoderGateway passwordEncoderGateway;

    @Mock
    private JwtGateway jwtGateway;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(userGateway, passwordEncoderGateway, jwtGateway);
    }

    @Nested
    @DisplayName("Successful Login Tests")
    class SuccessfulLoginTests {

        @Test
        @DisplayName("Should return login response when credentials are valid")
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";
            String token = "jwt-token";
            Long userId = 1L;
            String name = "John Doe";

            User user = new User(
                new UserId(userId),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);
            LoginResponse expectedResponse = new LoginResponse(
                userId, name, email, UserRole.STUDENT, token
            );

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
            when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(token);

            StepVerifier.create(loginUseCase.execute(command))
                .expectNext(expectedResponse)
                .verifyComplete();

            verify(userGateway).findByEmail(any(Email.class));
            verify(passwordEncoderGateway).matches(password, encodedPassword);
            verify(jwtGateway).generateToken(userId, UserRole.STUDENT.name());
        }

        @Test
        @DisplayName("Should return login response for teacher role")
        void shouldReturnLoginResponseForTeacherRole() {
            String email = "teacher@example.com";
            String password = "TeacherPass123!";
            String encodedPassword = "EncodedPass123!";
            String token = "jwt-token";
            Long userId = 2L;
            String name = "Jane Teacher";

            User user = new User(
                new UserId(userId),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.TEACHER
            );

            LoginCommand command = new LoginCommand(email, password);
            LoginResponse expectedResponse = new LoginResponse(
                userId, name, email, UserRole.TEACHER, token
            );

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
            when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(token);

            StepVerifier.create(loginUseCase.execute(command))
                .expectNext(expectedResponse)
                .verifyComplete();

            verify(jwtGateway).generateToken(userId, UserRole.TEACHER.name());
        }
    }

    @Nested
    @DisplayName("User Not Found Tests")
    class UserNotFoundTests {

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
            String email = "nonexistent@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.empty());

            StepVerifier.create(loginUseCase.execute(command))
                .expectError(UserNotFoundException.class)
                .verify();

            verify(userGateway).findByEmail(any(Email.class));
            verifyNoInteractions(passwordEncoderGateway);
            verifyNoInteractions(jwtGateway);
        }
    }

    @Nested
    @DisplayName("Invalid Credentials Tests")
    class InvalidCredentialsTests {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password is wrong")
        void shouldThrowInvalidCredentialsExceptionWhenPasswordIsWrong() {
            String email = "john.doe@example.com";
            String password = "WrongPassword123!";
            String encodedPassword = "EncodedPass123!";

            User user = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(false);

            StepVerifier.create(loginUseCase.execute(command))
                .expectError(InvalidCredentialsException.class)
                .verify();

            verify(userGateway).findByEmail(any(Email.class));
            verify(passwordEncoderGateway).matches(password, encodedPassword);
            verifyNoInteractions(jwtGateway);
        }

        @Test
        @DisplayName("Should handle user active status correctly")
        void shouldHandleUserActiveStatusCorrectly() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";
            String token = "jwt-token";
            Long userId = 1L;
            String name = "John Doe";

            User user = new User(
                new UserId(userId),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);
            LoginResponse expectedResponse = new LoginResponse(
                userId, name, email, UserRole.STUDENT, token
            );

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
            when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(token);

            StepVerifier.create(loginUseCase.execute(command))
                .expectNext(expectedResponse)
                .verifyComplete();

            verify(userGateway).findByEmail(any(Email.class));
            verify(passwordEncoderGateway).matches(password, encodedPassword);
            verify(jwtGateway).generateToken(userId, UserRole.STUDENT.name());
        }
    }

    @Nested
    @DisplayName("Inactive User Tests")
    class InactiveUserTests {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user is inactive")
        void shouldThrowInvalidCredentialsExceptionWhenUserIsInactive() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";

            User inactiveUser = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );
            inactiveUser.deactivate();

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(inactiveUser));

            StepVerifier.create(loginUseCase.execute(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof InvalidCredentialsException &&
                    throwable.getMessage().contains("User account is deactivated")
                )
                .verify();

            verify(userGateway).findByEmail(any(Email.class));
            verifyNoInteractions(passwordEncoderGateway);
            verifyNoInteractions(jwtGateway);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for inactive teacher")
        void shouldThrowInvalidCredentialsExceptionForInactiveTeacher() {
            String email = "teacher@example.com";
            String password = "TeacherPass123!";
            String encodedPassword = "EncodedPass123!";

            User inactiveTeacher = new User(
                new UserId(2L),
                new UserName("Jane Teacher"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.TEACHER
            );
            inactiveTeacher.deactivate();

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(inactiveTeacher));

            StepVerifier.create(loginUseCase.execute(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof InvalidCredentialsException &&
                    throwable.getMessage().contains("User account is deactivated")
                )
                .verify();

            verify(userGateway).findByEmail(any(Email.class));
            verifyNoInteractions(passwordEncoderGateway);
            verifyNoInteractions(jwtGateway);
        }
    }

    @Nested
    @DisplayName("Gateway Interaction Tests")
    class GatewayInteractionTests {

        @Test
        @DisplayName("Should call all gateways in correct order")
        void shouldCallAllGatewaysInCorrectOrder() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";
            String token = "jwt-token";

            User user = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
            when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(token);

            StepVerifier.create(loginUseCase.execute(command))
                .expectNextCount(1)
                .verifyComplete();

            verify(userGateway).findByEmail(any(Email.class));
            verify(passwordEncoderGateway).matches(password, encodedPassword);
            verify(jwtGateway).generateToken(1L, UserRole.STUDENT.name());
        }

        @Test
        @DisplayName("Should not call password encoder when user not found")
        void shouldNotCallPasswordEncoderWhenUserNotFound() {
            String email = "nonexistent@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.empty());

            StepVerifier.create(loginUseCase.execute(command))
                .expectError(UserNotFoundException.class)
                .verify();

            verify(userGateway).findByEmail(any(Email.class));
            verifyNoInteractions(passwordEncoderGateway);
            verifyNoInteractions(jwtGateway);
        }

        @Test
        @DisplayName("Should not call JWT gateway when password is wrong")
        void shouldNotCallJwtGatewayWhenPasswordIsWrong() {
            String email = "john.doe@example.com";
            String password = "WrongPassword123!";
            String encodedPassword = "EncodedPass123!";

            User user = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(false);

            StepVerifier.create(loginUseCase.execute(command))
                .expectError(InvalidCredentialsException.class)
                .verify();

            verify(userGateway).findByEmail(any(Email.class));
            verify(passwordEncoderGateway).matches(password, encodedPassword);
            verifyNoInteractions(jwtGateway);
        }
    }

    @Nested
    @DisplayName("Error Message Tests")
    class ErrorMessageTests {

        @Test
        @DisplayName("Should include correct message in UserNotFoundException")
        void shouldIncludeCorrectMessageInUserNotFoundException() {
            String email = "nonexistent@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.empty());

            StepVerifier.create(loginUseCase.execute(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UserNotFoundException &&
                    throwable.getMessage().equals("User not found")
                )
                .verify();
        }

        @Test
        @DisplayName("Should include correct message in InvalidCredentialsException for wrong password")
        void shouldIncludeCorrectMessageInInvalidCredentialsExceptionForWrongPassword() {
            String email = "john.doe@example.com";
            String password = "WrongPassword123!";
            String encodedPassword = "EncodedPass123!";

            User user = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(false);

            StepVerifier.create(loginUseCase.execute(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof InvalidCredentialsException &&
                    throwable.getMessage().equals("Invalid credentials")
                )
                .verify();
        }

        @Test
        @DisplayName("Should include correct message in InvalidCredentialsException for inactive user")
        void shouldIncludeCorrectMessageInInvalidCredentialsExceptionForInactiveUser() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";

            User inactiveUser = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );
            inactiveUser.deactivate();

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(inactiveUser));

            StepVerifier.create(loginUseCase.execute(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof InvalidCredentialsException &&
                    throwable.getMessage().equals("User account is deactivated")
                )
                .verify();
        }
    }

    @Nested
    @DisplayName("Response Data Tests")
    class ResponseDataTests {

        @Test
        @DisplayName("Should return correct login response data")
        void shouldReturnCorrectLoginResponseData() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";
            String token = "jwt-token";
            Long userId = 1L;
            String name = "John Doe";

            User user = new User(
                new UserId(userId),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
            when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(token);

            StepVerifier.create(loginUseCase.execute(command))
                .expectNextMatches(response -> 
                    response.getUserId().equals(userId) &&
                    response.getName().equals(name) &&
                    response.getEmail().equals(email) &&
                    response.getRole() == UserRole.STUDENT &&
                    response.getToken().equals(token)
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should return correct login response for teacher")
        void shouldReturnCorrectLoginResponseForTeacher() {
            String email = "teacher@example.com";
            String password = "TeacherPass123!";
            String encodedPassword = "EncodedPass123!";
            String token = "jwt-token";
            Long userId = 2L;
            String name = "Jane Teacher";

            User user = new User(
                new UserId(userId),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.TEACHER
            );

            LoginCommand command = new LoginCommand(email, password);

            when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(user));
            when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
            when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(token);

            StepVerifier.create(loginUseCase.execute(command))
                .expectNextMatches(response -> 
                    response.getUserId().equals(userId) &&
                    response.getName().equals(name) &&
                    response.getEmail().equals(email) &&
                    response.getRole() == UserRole.TEACHER &&
                    response.getToken().equals(token)
                )
                .verifyComplete();
        }
    }
}
