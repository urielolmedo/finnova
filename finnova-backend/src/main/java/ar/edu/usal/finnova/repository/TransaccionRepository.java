package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    // CU-011: historial completo, ordenado por fecha descendente
    List<Transaccion> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    // CU-012: filtro por rango de fechas
    List<Transaccion> findByUsuarioIdAndFechaBetweenOrderByFechaDesc(Long usuarioId, LocalDate desde, LocalDate hasta);

    // CU-013: filtro por categoria
    List<Transaccion> findByUsuarioIdAndCategoriaIdOrderByFechaDesc(Long usuarioId, Long categoriaId);

    // Verificar que una transaccion pertenezca al usuario antes de editar/eliminar (seguridad)
    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}