package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.example.security.entities.ERole;
import com.example.security.entities.Role;
import com.example.security.entities.User;
import com.example.security.repository.RoleRepository;
import com.example.security.repository.UserRepository;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-repository;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_USER)
            .build());
        Role adminRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_ADMIN)
            .build());

        testUser = userRepository.save(User.builder()
            .username("repository-user")
            .email("repository-user@example.com")
            .password("encoded-password")
            .roles(Set.of(userRole, adminRole))
            .build());
    }

    @Test
    @DisplayName("Debe encontrar un usuario por su nombre de usuario")
    void findByUsernameReturnsMatchingUser() {
        assertThat(userRepository.findByUsername("repository-user"))
            .hasValueSatisfying(user -> {
                assertThat(user.getId()).isEqualTo(testUser.getId());
                assertThat(user.getEmail()).isEqualTo("repository-user@example.com");
            });
    }

    @Test
    @DisplayName("Debe encontrar un usuario por su email")
    void findByEmailReturnsMatchingUser() {
        assertThat(userRepository.findByEmail("repository-user@example.com"))
            .hasValueSatisfying(user -> {
                assertThat(user.getId()).isEqualTo(testUser.getId());
                assertThat(user.getUsername()).isEqualTo("repository-user");
            });
    }

    @Test
    @DisplayName("Debe indicar que el nombre de usuario existe")
    void existsByUsernameReturnsTrueForRegisteredUsername() {
        assertThat(userRepository.existsByUsername("repository-user")).isTrue();
    }

    @Test
    @DisplayName("Debe indicar que el email existe")
    void existsByEmailReturnsTrueForRegisteredEmail() {
        assertThat(userRepository.existsByEmail("repository-user@example.com")).isTrue();
    }

    @Test
    @DisplayName("Debe indicar que un nombre de usuario inexistente no está registrado")
    void existsByUsernameReturnsFalseForUnknownUsername() {
        assertThat(userRepository.existsByUsername("unknown-user")).isFalse();
    }

    @Test
    @DisplayName("Debe indicar que un email inexistente no está registrado")
    void existsByEmailReturnsFalseForUnknownEmail() {
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("Debe devolver un resultado vacío al buscar un email no registrado")
    void findByEmailReturnsEmptyWhenEmailDoesNotExist() {
        assertThat(userRepository.findByEmail("unknown@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Debe guardar los roles asociados al usuario")
    void savePersistsUserRoles() {
        assertThat(userRepository.findByEmail("repository-user@example.com"))
            .hasValueSatisfying(user ->
                assertThat(user.getRoles())
                    .extracting("name")
                    .containsExactlyInAnyOrder(ERole.ROLE_USER, ERole.ROLE_ADMIN)
            );
    }
}
