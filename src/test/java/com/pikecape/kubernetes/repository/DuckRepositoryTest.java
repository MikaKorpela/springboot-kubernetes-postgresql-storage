package com.pikecape.kubernetes.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pikecape.kubernetes.common.configuration.PostgresContainerInitializer;
import com.pikecape.kubernetes.repository.entity.DuckEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ExtendWith(PostgresContainerInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
public class DuckRepositoryTest {
  @Autowired
  private DuckRepository duckRepository;

  @BeforeEach
  void setUp() {
    duckRepository.deleteAll();
  }

  private final DuckEntity donald = DuckEntity.builder()
      .name("Donald")
      .build();

  private final DuckEntity daisy = DuckEntity.builder()
      .name("Daisy")
      .build();

  @Test
  void testFindByUid() {
    DuckEntity created = duckRepository.create(donald);
    UUID id = created.getId();

    Optional<DuckEntity> result = duckRepository.findById(id);

    assertTrue(result.isPresent());
    assertEquals(donald.getName(), result.get().getName());
  }

  @Test
  void testFindAll() {
    duckRepository.create(donald);
    duckRepository.create(daisy);

    List<DuckEntity> result = duckRepository.findAll();

    assertEquals(2, result.size());
  }

  @Test
  void testCreateDuck() {
    duckRepository.create(donald);

    List<DuckEntity> result = duckRepository.findAll();

    assertEquals(1, result.size());
    assertNotNull(result.getFirst().getId());
    assertEquals(donald.getName(), result.getFirst().getName());
  }

  @Test
  void testUpdateDuck() {
    DuckEntity created = duckRepository.create(donald);

    List<DuckEntity> result = duckRepository.findAll();

    assertEquals(1, result.size());
    assertEquals(donald.getName(), result.getFirst().getName());

    created.setName("Duey");
    duckRepository.update(created);

    result = duckRepository.findAll();

    assertEquals(1, result.size());
    assertEquals("Duey", result.getFirst().getName());
  }

  @Test
  void testDeleteDuck() {
    DuckEntity created = duckRepository.create(donald);

    List<DuckEntity> result = duckRepository.findAll();

    assertEquals(1, result.size());

    duckRepository.delete(created);

    result = duckRepository.findAll();

    assertEquals(0, result.size());
  }
}
