package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.example.security.entities.ERole;
import com.example.security.entities.Role;
import com.example.security.repository.RoleRepository;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:role-repository;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_USER)
            .build());
        adminRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_ADMIN)
            .build());
    }

    @Test
    @DisplayName("Debe encontrar el rol de usuario por su nombre")
    void findByNameReturnsUserRole() {
        assertThat(roleRepository.findByName(ERole.ROLE_USER))
            .hasValueSatisfying(role -> {
                assertThat(role.getId()).isEqualTo(userRole.getId());
                assertThat(role.getName()).isEqualTo(ERole.ROLE_USER);
            });
    }

    @Test
    @DisplayName("Debe encontrar el rol de administrador por su nombre")
    void findByNameReturnsAdminRole() {
        assertThat(roleRepository.findByName(ERole.ROLE_ADMIN))
            .hasValueSatisfying(role -> {
                assertThat(role.getId()).isEqualTo(adminRole.getId());
                assertThat(role.getName()).isEqualTo(ERole.ROLE_ADMIN);
            });
    }

    @Test
    @DisplayName("Debe devolver un resultado vacío para un rol que no está guardado")
    void findByNameReturnsEmptyWhenRoleDoesNotExist() {
        roleRepository.delete(userRole);

        assertThat(roleRepository.findByName(ERole.ROLE_USER)).isEmpty();
    }
}
