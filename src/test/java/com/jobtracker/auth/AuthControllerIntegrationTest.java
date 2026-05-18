package com.jobtracker.auth;

import com.jobtracker.application.TestcontainersConfiguration;
import com.jobtracker.auth.dto.CreateUserRequest;
import com.jobtracker.auth.dto.LoginRequest;
import com.jobtracker.auth.internal.RefreshTokenRepository;
import com.jobtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_createsUserAndReturnsTokenPair() throws Exception {
        CreateUserRequest request = new CreateUserRequest("test@example.com", "MySecurePass123", "Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void login_returnsTokenPairForValidCredentials() throws Exception {
        CreateUserRequest registerRequest = new CreateUserRequest("login@example.com", "MySecurePass123", "Login User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("login@example.com", "MySecurePass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.fullName").value("Login User"));
    }

    @Test
    void refresh_rotatesRefreshTokenAndReturnsNewPair() throws Exception {
        CreateUserRequest registerRequest = new CreateUserRequest("refresh@example.com", "MySecurePass123", "Refresh User");

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String initialRefreshToken = extractRefreshToken(registerResponse);

        String refreshedResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload(initialRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedRefreshToken = extractRefreshToken(refreshedResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload(initialRefreshToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    private String registerPayload(CreateUserRequest request) {
        return """
                {
                  \"email\": \"%s\",
                  \"password\": \"%s\",
                  \"fullName\": \"%s\"
                }
                """.formatted(request.email(), request.password(), request.fullName());
    }

    private String loginPayload(LoginRequest request) {
        return """
                {
                  \"email\": \"%s\",
                  \"password\": \"%s\"
                }
                """.formatted(request.email(), request.password());
    }

    private String refreshPayload(String refreshToken) {
        return """
                {
                  \"refreshToken\": \"%s\"
                }
                """.formatted(refreshToken);
    }

    private String extractRefreshToken(String responseBody) {
        Map<String, Object> parsed = jsonParser.parseMap(responseBody);
        return parsed.get("refreshToken").toString();
    }
}
