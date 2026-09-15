package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.CategoriaRequest;
import ar.edu.usal.finnova.dto.CategoriaResponse;
import ar.edu.usal.finnova.model.Categoria;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.CategoriaRepository;
import ar.edu.usal.finnova.repository.TransaccionRepository;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // CU-016: Ver categorias disponibles (predefinidas + propias del usuario)
    @GetMapping
    public List<CategoriaResponse> listar(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return categoriaRepository.findByPredefinidaTrueOrUsuarioId(usuario.getId())
                .stream().map(CategoriaResponse::new).toList();
    }

    // CU-017: Crear categoria personalizada
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CategoriaRequest request, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body("El nombre de la categoría es obligatorio");
        }
        if (request.getNombre().length() > 50) {
            return ResponseEntity.badRequest().body("El nombre no puede superar los 50 caracteres");
        }
        if (categoriaRepository.existsByNombreIgnoreCaseAndUsuarioId(request.getNombre(), usuario.getId())) {
            return ResponseEntity.badRequest().body("Ya existe una categoría con ese nombre");
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        categoria.setTipo(request.getTipo());
        categoria.setColor(request.getColor() != null ? request.getColor() : "#6c757d");
        categoria.setPredefinida(false);
        categoria.setUsuario(usuario);
        categoriaRepository.save(categoria);

        return ResponseEntity.ok(new CategoriaResponse(categoria));
    }

    // CU-018: Editar categoria personalizada
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody CategoriaRequest request, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);

        if (categoria == null) {
            return ResponseEntity.status(404).body("Categoría no encontrada");
        }
        if (categoria.isPredefinida()) {
            return ResponseEntity.badRequest().body("Las categorías predefinidas no pueden editarse");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body("El nombre de la categoría es obligatorio");
        }
        boolean nombreEnUso = categoriaRepository.existsByNombreIgnoreCaseAndUsuarioId(request.getNombre(), usuario.getId())
                && !categoria.getNombre().equalsIgnoreCase(request.getNombre());
        if (nombreEnUso) {
            return ResponseEntity.badRequest().body("Ya existe otra categoría con ese nombre");
        }

        categoria.setNombre(request.getNombre());
        if (request.getColor() != null) categoria.setColor(request.getColor());
        categoriaRepository.save(categoria);

        // Nota: no hace falta "propagar" el nombre a las transacciones (CU-018, paso 6):
        // como la relacion es por categoria_id (FK), el nombre actualizado ya se ve
        // reflejado automaticamente en cualquier consulta que traiga la transaccion con su categoria.
        return ResponseEntity.ok(new CategoriaResponse(categoria));
    }

    // CU-019: Eliminar categoria personalizada
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);

        if (categoria == null) {
            return ResponseEntity.status(404).body("Categoría no encontrada");
        }
        if (categoria.isPredefinida()) {
            return ResponseEntity.badRequest().body("Las categorías predefinidas no pueden eliminarse");
        }

        long cantidadTransacciones = transaccionRepository.countByCategoriaId(id);
        if (cantidadTransacciones > 0) {
            return ResponseEntity.badRequest().body(
                    "No se puede eliminar: hay " + cantidadTransacciones +
                            " transacción(es) asociada(s). Reasigná esas transacciones a otra categoría antes de eliminarla."
            );
        }

        categoriaRepository.delete(categoria);
        return ResponseEntity.ok("Categoría eliminada correctamente");
    }
}