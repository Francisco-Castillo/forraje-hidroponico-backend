package com.fvh.sensores.lectura.consulta;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Solo consultas; no se usa para insertar. */
public interface LecturaJpaRepository extends JpaRepository<LecturaEntity, LecturaId>, JpaSpecificationExecutor<LecturaEntity> {

	Optional<LecturaEntity> findFirstByNodoOrderByIdTsDesc(String nodo);
}
