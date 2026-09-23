package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.TipoTransaccion;
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

    long countByCategoriaId(Long categoriaId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t " +
                    "WHERE t.usuario.id = :usuarioId AND t.tipo = :tipo AND t.fecha BETWEEN :desde AND :hasta"
    )
    java.math.BigDecimal sumarPorTipoYPeriodo(Long usuarioId, TipoTransaccion tipo, LocalDate desde, LocalDate hasta);

    @org.springframework.data.jpa.repository.Query(
            "SELECT t.categoria.nombre, t.categoria.color, t.tipo, SUM(t.monto) FROM Transaccion t " +
                    "WHERE t.usuario.id = :usuarioId AND t.fecha BETWEEN :desde AND :hasta " +
                    "GROUP BY t.categoria.nombre, t.categoria.color, t.tipo"
    )
    List<Object[]> sumarAgrupadoPorCategoria(Long usuarioId, LocalDate desde, LocalDate hasta);

}