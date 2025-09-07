package org.tcc.monitor.jrf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tcc.monitor.jrf.entity.ArquivoEntity;

import java.util.UUID;

@Repository
public interface ArquivoRepository extends JpaRepository<ArquivoEntity, UUID> {
}
