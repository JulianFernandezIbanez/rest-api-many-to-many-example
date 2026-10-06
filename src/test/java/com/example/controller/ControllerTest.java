package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;
import com.example.security.entities.ERole;
import com.example.security.entities.Role;
import com.example.security.entities.User;
import com.example.security.repository.RoleRepository;
import com.example.security.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:controller-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional
class ControllerTest {

    private static final String TEST_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TutorialRepository tutorialRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tutorial tutorial;
    private Tag tag;

    @BeforeEach
    void setUp() {
        tutorialRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_USER)
            .build());
        Role adminRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_ADMIN)
            .build());

        createUser("test-user", "user@test.com", userRole);
        createUser("test-admin", "admin@test.com", adminRole);

        tutorial = Tutorial.builder()
            .title("Spring testing")
            .description("Controller integration tests")
            .published(true)
            .build();
        tag = Tag.builder().name("Spring").build();
        tutorial.addTag(tag);
        tutorialRepository.save(tutorial);
    }

    @Test
    @DisplayName("Debe registrar un usuario y permitir iniciar sesión con su email")
    void authControllerRegistersAndAuthenticatesUserByEmail() throws Exception {
        String signupBody = """
            {
              "username": "new-user",
              "email": "new-user@test.com",
              "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("User registered successfully"));

        String signinBody = """
            {
              "email": "new-user@test.com",
              "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signinBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.email").value("new-user@test.com"));
    }

    @Test
    @DisplayName("Debe devolver 401 al consultar tutoriales sin autenticación")
    void tutorialControllerRejectsUnauthenticatedRead() throws Exception {
        mockMvc.perform(get("/api/tutorials"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe permitir consultar tutoriales a un usuario autenticado")
    void tutorialControllerAllowsAuthenticatedRead() throws Exception {
        mockMvc.perform(get("/api/tutorials")
                .header("Authorization", bearerToken("user@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Spring testing"));
    }

    @Test
    @DisplayName("Debe impedir que un usuario sin rol ADMIN cree tutoriales")
    void tutorialControllerForbidsTutorialCreationForUserRole() throws Exception {
        String body = """
            {
              "title": "Forbidden tutorial",
              "description": "A normal user must not create this",
              "published": true
            }
            """;

        mockMvc.perform(post("/api/tutorials")
                .header("Authorization", bearerToken("user@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe permitir a ADMIN crear, modificar y borrar tutoriales")
    void tutorialControllerAllowsAdminToManageTutorials() throws Exception {
        String token = bearerToken("admin@test.com");
        String createBody = """
            {
              "title": "New tutorial",
              "description": "Created by admin",
              "published": true
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/tutorials")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("New tutorial"))
            .andReturn();

        long createdId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
        String updateBody = """
            {
              "title": "Updated tutorial",
              "description": "Updated by admin",
              "published": false
            }
            """;

        mockMvc.perform(put("/api/tutorials/{id}", createdId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated tutorial"))
            .andExpect(jsonPath("$.published").value(false));

        mockMvc.perform(delete("/api/tutorials/{id}", createdId)
                .header("Authorization", token))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Debe devolver los tags asociados a un tutorial autenticado")
    void tagControllerReturnsTagsForTutorial() throws Exception {
        mockMvc.perform(get("/api/tutorials/{id}/tags", tutorial.getId())
                .header("Authorization", bearerToken("user@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Spring"));
    }

    @Test
    @DisplayName("Debe permitir consultar un tag y sus tutoriales asociados")
    void tagControllerReturnsTagAndAssociatedTutorials() throws Exception {
        String token = bearerToken("user@test.com");

        mockMvc.perform(get("/api/tags/{id}", tag.getId())
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Spring"));

        mockMvc.perform(get("/api/tags/{id}/tutorials", tag.getId())
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Spring testing"));
    }

    @Test
    @DisplayName("Debe impedir que un usuario sin rol ADMIN modifique tags")
    void tagControllerForbidsTagUpdateForUserRole() throws Exception {
        mockMvc.perform(put("/api/tags/{id}", tag.getId())
                .header("Authorization", bearerToken("user@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Forbidden update"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe permitir a ADMIN añadir y modificar tags")
    void tagControllerAllowsAdminToAddAndUpdateTags() throws Exception {
        String token = bearerToken("admin@test.com");

        MvcResult addResult = mockMvc.perform(post("/api/tutorials/{id}/tags", tutorial.getId())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Security"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Security"))
            .andReturn();

        JsonNode createdTag = objectMapper.readTree(addResult.getResponse().getContentAsString());
        long createdTagId = createdTag.get("id").asLong();

        mockMvc.perform(put("/api/tags/{id}", createdTagId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "JWT Security"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("JWT Security"));
    }

    @Test
    @DisplayName("Debe permitir a ADMIN eliminar tags")
    void tagControllerAllowsAdminToDeleteTag() throws Exception {
        Tag unassociatedTag = tagRepository.save(
            Tag.builder().name("Disposable").build()
        );

        mockMvc.perform(delete("/api/tags/{id}", unassociatedTag.getId())
                .header("Authorization", bearerToken("admin@test.com")))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Debe permitir a ADMIN quitar un tag de un tutorial")
    void tagControllerAllowsAdminToRemoveTagFromTutorial() throws Exception {
        mockMvc.perform(delete(
                    "/api/tutorials/{tutorialId}/tags/{tagId}",
                    tutorial.getId(),
                    tag.getId()
                )
                .header("Authorization", bearerToken("admin@test.com")))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Debe permitir a ADMIN eliminar todos los tutoriales")
    void tutorialControllerAllowsAdminToDeleteAllTutorials() throws Exception {
        mockMvc.perform(delete("/api/tutorials")
                .header("Authorization", bearerToken("admin@test.com")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tutorials")
                .header("Authorization", bearerToken("user@test.com")))
            .andExpect(status().isNoContent());
    }

    private void createUser(String username, String email, Role role) {
        userRepository.save(User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(TEST_PASSWORD))
            .roles(Set.of(role))
            .build());
    }

    private String bearerToken(String email) throws Exception {
        String loginBody = objectMapper.writeValueAsString(
            java.util.Map.of("email", email, "password", TEST_PASSWORD)
        );
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("token")
            .asText();
        return "Bearer " + token;
    }
}
