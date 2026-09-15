package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByPredefinidaTrueOrUsuarioId(Long usuarioId);
    boolean existsByNombreIgnoreCaseAndUsuarioId(String nombre, Long usuarioId);
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);
}