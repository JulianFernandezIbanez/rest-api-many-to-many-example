package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.security.entities.ERole;
import com.example.security.entities.Role;
import com.example.security.entities.User;
import com.example.security.repository.RoleRepository;
import com.example.security.repository.UserRepository;
import com.example.security.service.UserDetailsServiceImpl;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-details-service;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserDetailsServiceImpl.class)
class UserDetailsServiceImplTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User registeredUser;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.save(Role.builder()
            .name(ERole.ROLE_USER)
            .build());

        registeredUser = userRepository.save(User.builder()
            .username("registered-user")
            .email("user@example.com")
            .password("encoded-password")
            .roles(Set.of(userRole))
            .build());
    }

    @Test
    @DisplayName("Debe cargar los datos y roles del usuario usando su email")
    void loadUserByUsernameReturnsUserDetailsForRegisteredEmail() {
        UserDetails userDetails =
            userDetailsService.loadUserByUsername(registeredUser.getEmail());

        assertThat(userDetails.getUsername()).isEqualTo("registered-user");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly(ERole.ROLE_USER.name());
    }

    @Test
    @DisplayName("Debe lanzar una excepción cuando el email no está registrado")
    void loadUserByUsernameThrowsWhenEmailDoesNotExist() {
        assertThatThrownBy(() ->
            userDetailsService.loadUserByUsername("missing@example.com")
        )
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("missing@example.com");
    }
}
