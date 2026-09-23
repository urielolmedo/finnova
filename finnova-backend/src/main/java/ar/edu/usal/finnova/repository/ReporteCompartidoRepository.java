package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.ReporteCompartido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReporteCompartidoRepository extends JpaRepository<ReporteCompartido, Long> {
    Optional<ReporteCompartido> findByToken(String token);
}