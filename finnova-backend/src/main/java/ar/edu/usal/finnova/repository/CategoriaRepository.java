package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByPredefinidaTrueOrUsuarioId(Long usuarioId);
}