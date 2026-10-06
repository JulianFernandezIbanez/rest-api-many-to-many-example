package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.example.entities.Tag;
import com.example.entities.Tutorial;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:tutorial-repository;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TutorialRepositoryTest {

    @Autowired
    private TutorialRepository tutorialRepository;

    private Tag javaTag;

    @BeforeEach
    void setUp() {
        tutorialRepository.save(createTutorial("Published tutorial", "Visible", true));
        tutorialRepository.save(createTutorial("Draft tutorial", "Not visible", false));
        tutorialRepository.save(createTutorial(
            "Spring Security Basics",
            "JWT authentication",
            true
        ));
        tutorialRepository.save(createTutorial(
            "Java Collections",
            "Collection APIs",
            true
        ));

        javaTag = Tag.builder().name("Java").build();
        Tutorial javaTutorial = createTutorial("Java basics", "Introduction", true);
        javaTutorial.addTag(javaTag);
        tutorialRepository.save(javaTutorial);

        Tutorial springTutorial = createTutorial("Spring basics", "Framework", true);
        springTutorial.addTag(Tag.builder().name("Spring").build());
        tutorialRepository.save(springTutorial);
    }

    @Test
    @DisplayName("Debe devolver solo los tutoriales con el estado de publicación solicitado")
    void findByPublishedReturnsOnlyTutorialsWithRequestedStatus() {
        var published = tutorialRepository.findByPublished(true);

        assertThat(published)
            .extracting("published")
            .containsOnly(true);
        assertThat(published)
            .extracting("title")
            .contains("Published tutorial");
    }

    @Test
    @DisplayName("Debe devolver los tutoriales cuyo título contiene el texto buscado")
    void findByTitleContainingReturnsMatchingTutorials() {
        var matches = tutorialRepository.findByTitleContaining("Security");

        assertThat(matches)
            .hasSize(1)
            .extracting("title")
            .containsExactly("Spring Security Basics");
    }

    @Test
    @DisplayName("Debe devolver los tutoriales asociados al tag indicado")
    void findTutorialsByTagsIdReturnsTutorialsAssociatedWithTag() {
        var matches = tutorialRepository.findTutorialsByTagsId(javaTag.getId());

        assertThat(matches)
            .hasSize(1)
            .extracting("title")
            .containsExactly("Java basics");
    }

    private Tutorial createTutorial(String title, String description, boolean published) {
        return Tutorial.builder()
            .title(title)
            .description(description)
            .published(published)
            .build();
    }
}
