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
    "spring.datasource.url=jdbc:h2:mem:tag-repository;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TutorialRepository tutorialRepository;

    private Tutorial tutorialWithTags;
    private Tutorial tutorialWithoutTags;

    @BeforeEach
    void setUp() {
        tutorialWithTags = Tutorial.builder()
            .title("Spring Data JPA")
            .description("Repository testing")
            .published(true)
            .build();
        tutorialWithTags.addTag(Tag.builder().name("Spring").build());
        tutorialWithTags.addTag(Tag.builder().name("Persistence").build());
        tutorialRepository.save(tutorialWithTags);

        Tutorial otherTutorial = Tutorial.builder()
            .title("Java basics")
            .description("Language fundamentals")
            .published(true)
            .build();
        otherTutorial.addTag(Tag.builder().name("Java").build());
        tutorialRepository.save(otherTutorial);

        tutorialWithoutTags = tutorialRepository.save(Tutorial.builder()
            .title("Tagless tutorial")
            .description("No associated tags")
            .published(false)
            .build());
    }

    @Test
    @DisplayName("Debe devolver los tags asociados al tutorial indicado")
    void findTagsByTutorialsIdReturnsOnlyTagsAssociatedWithTutorial() {
        var tags = tagRepository.findTagsByTutorialsId(tutorialWithTags.getId());

        assertThat(tags)
            .hasSize(2)
            .extracting("name")
            .containsExactlyInAnyOrder("Spring", "Persistence");
    }

    @Test
    @DisplayName("Debe devolver una lista vacía si el tutorial no tiene tags")
    void findTagsByTutorialsIdReturnsEmptyListWhenTutorialHasNoTags() {
        assertThat(tagRepository.findTagsByTutorialsId(tutorialWithoutTags.getId())).isEmpty();
    }
}
