package wiliammelo.clouddesk.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void createsPasswordEncoder() {
        assertThat(securityConfig.passwordEncoder().matches("password", securityConfig.passwordEncoder().encode("password")))
                .isTrue();
    }

    @Test
    void authenticationEntryPointReturnsUnauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.authenticationEntryPoint()
                .commence(new MockHttpServletRequest(), response, mock(AuthenticationException.class));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void userDetailsServiceDoesNotExposeDefaultUsers() {
        assertThatThrownBy(() -> securityConfig.userDetailsService().loadUserByUsername("owner@cloud.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("owner@cloud.test");
    }
}
