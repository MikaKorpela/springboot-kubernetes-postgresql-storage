package com.pikecape.kubernetes.repository;

import com.pikecape.kubernetes.repository.entity.DuckEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DuckRepository extends JpaRepository<DuckEntity, UUID> {
  default DuckEntity create(DuckEntity duckEntity) {
    return save(duckEntity);
  }

  default DuckEntity update(DuckEntity duckEntity) {
    return save(duckEntity);
  }
}
