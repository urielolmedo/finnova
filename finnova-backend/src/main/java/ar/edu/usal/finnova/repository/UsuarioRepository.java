package ar.edu.usal.finnova.repository;

import ar.edu.usal.finnova.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Para el login (CU-002): buscar por email
    Optional<Usuario> findByEmail(String email);

    // Para el registro (CU-001): verificar si el email ya existe
    boolean existsByEmail(String email);
}