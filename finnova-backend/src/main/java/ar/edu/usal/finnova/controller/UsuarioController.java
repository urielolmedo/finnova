package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.ActualizarPerfilRequest;
import ar.edu.usal.finnova.dto.ModulosActivosRequest;
import ar.edu.usal.finnova.dto.UsuarioResponse;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(new UsuarioResponse(usuario));
    }

    // CU-005: Editar perfil de usuario
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody ActualizarPerfilRequest request, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(new UsuarioResponse(usuario));
    }

    // CU-006: Configurar modulos activos del sistema
    @PutMapping("/modulos")
    public ResponseEntity<?> actualizarModulos(@RequestBody ModulosActivosRequest request, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setModulosActivos(String.join(",", request.getModulos()));
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(new UsuarioResponse(usuario));
    }
}