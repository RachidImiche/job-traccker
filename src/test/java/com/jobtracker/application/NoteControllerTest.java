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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NoteControllerTest {

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

    @Autowired
    private NoteRepository noteRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        noteRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void addNote_returnsCreated() throws Exception {
        String accessToken = registerAndGetAccessToken("note.create@example.com");
        String applicationId = createApplication(accessToken, "Acme", "Backend Engineer", "https://acme.example/jobs/1");

        mockMvc.perform(post("/api/v1/applications/{id}/notes", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNotePayload("Reached out to recruiter.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.content").value("Reached out to recruiter."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void getNotes_returnsApplicationNotes() throws Exception {
        String accessToken = registerAndGetAccessToken("note.list@example.com");
        String applicationId = createApplication(accessToken, "Globex", "SRE", "https://globex.example/jobs/2");

        createNote(accessToken, applicationId, "Phone screen scheduled");
        createNote(accessToken, applicationId, "Sent thank-you email");

        mockMvc.perform(get("/api/v1/applications/{id}/notes", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Phone screen scheduled"))
                .andExpect(jsonPath("$[1].content").value("Sent thank-you email"));
    }

    @Test
    void deleteNote_returnsNoContent() throws Exception {
        String accessToken = registerAndGetAccessToken("note.delete@example.com");
        String applicationId = createApplication(accessToken, "Initech", "Platform Engineer", "https://initech.example/jobs/3");
        String noteId = createNote(accessToken, applicationId, "Drafted preparation notes");

        mockMvc.perform(delete("/api/v1/applications/{id}/notes/{noteId}", applicationId, noteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/applications/{id}/notes", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, "MySecurePass123", "Note User")))
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
                        .content(createApplicationPayload(companyName, roleTitle, jobUrl)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> parsed = jsonParser.parseMap(response);
        return parsed.get("id").toString();
    }

    private String createNote(String accessToken, String applicationId, String content) throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications/{id}/notes", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNotePayload(content)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonParser.parseMap(response).get("id").toString();
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

    private String createApplicationPayload(String companyName, String roleTitle, String jobUrl) {
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

    private String createNotePayload(String content) {
        return """
                {
                  \"content\": \"%s\"
                }
                """.formatted(content);
    }
}
