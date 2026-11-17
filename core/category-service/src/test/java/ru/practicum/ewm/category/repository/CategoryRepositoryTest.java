package ru.practicum.ewm.category.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.category.model.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void savesAndFindsCategoryByName() {
        Category saved = categoryRepository.save(Category.builder()
                .name("Concerts")
                .build());

        assertTrue(categoryRepository.findByName("Concerts").isPresent());
        assertEquals(saved.getId(), categoryRepository.findByName("Concerts").orElseThrow().getId());
    }
}
