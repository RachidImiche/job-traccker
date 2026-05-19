package com.jobtracker.application;

import com.jobtracker.auth.internal.RefreshTokenRepository;
import com.jobtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApplicationControllerTest {

    private MockMvc mockMvc;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_returnsCreatedApplication() throws Exception {
        String accessToken = registerAndGetAccessToken("app.create@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Acme", "Backend Engineer", "https://acme.example/jobs/1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andExpect(jsonPath("$.roleTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.jobUrl").value("https://acme.example/jobs/1"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void getAll_returnsPaginatedApplications() throws Exception {
        String accessToken = registerAndGetAccessToken("app.list@example.com");

        createApplication(accessToken, "Acme", "Engineer I", "https://acme.example/jobs/1");
        createApplication(accessToken, "Globex", "Engineer II", "https://globex.example/jobs/2");

        mockMvc.perform(get("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getById_returnsOwnedApplication() throws Exception {
        String accessToken = registerAndGetAccessToken("app.get@example.com");
        String applicationId = createApplication(accessToken, "Acme", "Platform Engineer", "https://acme.example/jobs/3");

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId))
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andExpect(jsonPath("$.roleTitle").value("Platform Engineer"));
    }

    @Test
    void update_partiallyUpdatesApplication() throws Exception {
        String accessToken = registerAndGetAccessToken("app.update@example.com");
        String applicationId = createApplication(accessToken, "Acme", "Backend Engineer", "https://acme.example/jobs/4");

        mockMvc.perform(put("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Updated Acme", "https://acme.example/jobs/updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId))
                .andExpect(jsonPath("$.companyName").value("Updated Acme"))
                .andExpect(jsonPath("$.roleTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.jobUrl").value("https://acme.example/jobs/updated"));
    }

    @Test
    void delete_removesApplication() throws Exception {
        String accessToken = registerAndGetAccessToken("app.delete@example.com");
        String applicationId = createApplication(accessToken, "Acme", "Site Reliability Engineer", "https://acme.example/jobs/5");

        mockMvc.perform(delete("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound());
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, "MySecurePass123", "App User")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonParser.parseMap(response).get("accessToken").toString();
    }

    private String createApplication(String accessToken, String companyName, String roleTitle, String jobUrl) throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(companyName, roleTitle, jobUrl)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> parsed = jsonParser.parseMap(response);
        return parsed.get("id").toString();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String registerPayload(String email, String password, String fullName) {
        return """
                {
                  \"email\": \"%s\",
                  \"password\": \"%s\",
                  \"fullName\": \"%s\"
                }
                """.formatted(email, password, fullName);
    }

    private String createPayload(String companyName, String roleTitle, String jobUrl) {
        return """
                {
                  \"companyName\": \"%s\",
                  \"roleTitle\": \"%s\",
                  \"jobUrl\": \"%s\",
                  \"salaryMin\": 90000,
                  \"salaryMax\": 140000,
                  \"location\": \"Remote\"
                }
                """.formatted(companyName, roleTitle, jobUrl);
    }

    private String updatePayload(String companyName, String jobUrl) {
        return """
                {
                  \"companyName\": \"%s\",
                  \"jobUrl\": \"%s\"
                }
                """.formatted(companyName, jobUrl);
    }
}
