package wiliammelo.clouddesk.shared;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import wiliammelo.clouddesk.auth.AuthenticationException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesNotFound() {
        var response = handler.handleNotFound(
                new ResourceNotFoundException("Missing."),
                request("/missing")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Missing.");
        assertThat(response.getBody().path()).isEqualTo("/missing");
        assertThat(response.getBody().fields()).isEmpty();
    }

    @Test
    void handlesConflict() {
        var response = handler.handleConflict(
                new ConflictException("Conflict."),
                request("/conflict")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Conflict.");
        assertThat(response.getBody().fields()).isEqualTo(Map.of());
    }

    @Test
    void handlesAuthentication() {
        var response = handler.handleAuthentication(
                new AuthenticationException("Invalid credentials."),
                request("/api/auth/login")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials.");
    }

    @Test
    void handlesBadRequest() {
        var response = handler.handleBadRequest(
                new BadRequestException("Invalid logo."),
                request("/api/companies/id/logo")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Invalid logo.");
    }

    @Test
    void handlesValidationErrorWithDefaultMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Payload(""), "payload");
        bindingResult.addError(new FieldError("payload", "name", "must not be blank"));

        var response = handler.handleValidation(
                new MethodArgumentNotValidException(parameter(), bindingResult),
                request("/validation")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed.");
        assertThat(response.getBody().fields()).containsEntry("name", "must not be blank");
    }

    @Test
    void handlesValidationErrorWithFallbackMessageAndKeepsFirstDuplicateField() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Payload(""), "payload");
        bindingResult.addError(new FieldError("payload", "name", null));
        bindingResult.addError(new FieldError("payload", "name", "second message"));

        var response = handler.handleValidation(
                new MethodArgumentNotValidException(parameter(), bindingResult),
                request("/validation")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fields()).containsEntry("name", "Invalid value.");
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }

    private org.springframework.core.MethodParameter parameter() throws NoSuchMethodException {
        Method method = Controller.class.getDeclaredMethod("create", Payload.class);
        return new org.springframework.core.MethodParameter(method, 0);
    }

    private record Payload(@NotBlank String name) {
    }

    private static class Controller {
        @SuppressWarnings("unused")
        void create(@Valid @RequestBody Payload payload) {
        }
    }
}
