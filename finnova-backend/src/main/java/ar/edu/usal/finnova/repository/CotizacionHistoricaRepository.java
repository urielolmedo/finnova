package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.CotizacionHistorica;
import ar.edu.usal.finnova.model.TipoCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CotizacionHistoricaRepository extends JpaRepository<CotizacionHistorica, Long> {
    Optional<CotizacionHistorica> findFirstByTipoOrderByFechaHoraDesc(TipoCotizacion tipo);
    List<CotizacionHistorica> findByTipoAndFechaHoraBetweenOrderByFechaHoraAsc(
            TipoCotizacion tipo, LocalDateTime desde, LocalDateTime hasta);
    List<CotizacionHistorica> findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(TipoCotizacion tipo, LocalDateTime desde);
    long countByTipo(TipoCotizacion tipo);
}