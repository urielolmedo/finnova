package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.TransaccionRequest;
import ar.edu.usal.finnova.dto.TransaccionResponse;
import ar.edu.usal.finnova.model.Categoria;
import ar.edu.usal.finnova.model.Transaccion;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.CategoriaRepository;
import ar.edu.usal.finnova.repository.TransaccionRepository;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionRepository transaccionRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ar.edu.usal.finnova.service.ArchivoService archivoService;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // CU-007 / CU-008: Registrar ingreso o egreso (el "tipo" viene en el body)
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody TransaccionRequest request, Authentication authentication) {
        if (request.getMonto() == null || request.getMonto().signum() <= 0) {
            return ResponseEntity.badRequest().body("El monto debe ser un valor numérico positivo");
        }
        if (request.getFecha() == null || request.getFecha().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body("La fecha no puede ser posterior a la fecha actual");
        }
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId()).orElse(null);
        if (categoria == null) {
            return ResponseEntity.badRequest().body("La categoría es un campo obligatorio y debe ser válida");
        }

        Usuario usuario = usuarioActual(authentication);
        Transaccion original = new Transaccion();
        original.setUsuario(usuario);
        original.setTipo(request.getTipo());
        original.setMonto(request.getMonto());
        original.setFecha(request.getFecha());
        original.setCategoria(categoria);
        original.setDescripcion(request.getDescripcion());
        original.setEsRecurrente(request.isEsRecurrente());

        int instanciasGeneradas = 1;

        if (request.isEsRecurrente()) {
            if (request.getFrecuencia() == null || request.getFechaFinRecurrencia() == null) {
                return ResponseEntity.badRequest().body("Frecuencia y fecha de fin son obligatorias para una transacción recurrente");
            }
            if (!request.getFechaFinRecurrencia().isAfter(request.getFecha())) {
                return ResponseEntity.badRequest().body("La fecha de fin debe ser posterior a la fecha de la transacción original");
            }
            original.setFrecuencia(request.getFrecuencia());
            original.setFechaFinRecurrencia(request.getFechaFinRecurrencia());
        }

        transaccionRepository.save(original);

        // CU-015: generar las instancias futuras hasta la fecha de fin, segun la frecuencia elegida
        if (request.isEsRecurrente()) {
            LocalDate siguiente = calcularSiguienteFecha(original.getFecha(), request.getFrecuencia());
            while (!siguiente.isAfter(request.getFechaFinRecurrencia())) {
                Transaccion instancia = new Transaccion();
                instancia.setUsuario(usuario);
                instancia.setTipo(original.getTipo());
                instancia.setMonto(original.getMonto());
                instancia.setFecha(siguiente);
                instancia.setCategoria(categoria);
                instancia.setDescripcion(original.getDescripcion());
                instancia.setEsRecurrente(false); // las instancias generadas no son "originales" de otra recurrencia
                instancia.setTransaccionOrigen(original);
                transaccionRepository.save(instancia);
                instanciasGeneradas++;
                siguiente = calcularSiguienteFecha(siguiente, request.getFrecuencia());
            }
        }

        TransaccionResponse response = new TransaccionResponse(original);
        return ResponseEntity.ok(java.util.Map.of(
                "transaccion", response,
                "instanciasGeneradas", instanciasGeneradas
        ));
    }

    private LocalDate calcularSiguienteFecha(LocalDate fecha, ar.edu.usal.finnova.model.FrecuenciaRecurrencia frecuencia) {
        return switch (frecuencia) {
            case DIARIA -> fecha.plusDays(1);
            case SEMANAL -> fecha.plusWeeks(1);
            case MENSUAL -> fecha.plusMonths(1);
            case ANUAL -> fecha.plusYears(1);
        };
    }

    // CU-011: Consultar historial completo
    @GetMapping
    public List<TransaccionResponse> listar(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return transaccionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId())
                .stream().map(TransaccionResponse::new).toList();
    }

    // CU-012: Filtrar por rango de fechas
    @GetMapping("/filtrar/fecha")
    public List<TransaccionResponse> filtrarPorFecha(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return transaccionRepository.findByUsuarioIdAndFechaBetweenOrderByFechaDesc(usuario.getId(), desde, hasta)
                .stream().map(TransaccionResponse::new).toList();
    }

    // CU-013: Filtrar por categoria
    @GetMapping("/filtrar/categoria/{categoriaId}")
    public List<TransaccionResponse> filtrarPorCategoria(
            @PathVariable Long categoriaId, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return transaccionRepository.findByUsuarioIdAndCategoriaIdOrderByFechaDesc(usuario.getId(), categoriaId)
                .stream().map(TransaccionResponse::new).toList();
    }

    // CU-009: Editar transaccion registrada
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody TransaccionRequest request, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (!transaccionRepository.existsByIdAndUsuarioId(id, usuario.getId())) {
            return ResponseEntity.status(404).body("Transacción no encontrada");
        }
        if (request.getMonto() == null || request.getMonto().signum() <= 0) {
            return ResponseEntity.badRequest().body("El monto debe ser un valor numérico positivo");
        }
        if (request.getFecha() == null || request.getFecha().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body("La fecha no puede ser posterior a la fecha actual");
        }
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId()).orElse(null);
        if (categoria == null) {
            return ResponseEntity.badRequest().body("La categoría es un campo obligatorio y debe ser válida");
        }

        Transaccion t = transaccionRepository.findById(id).orElseThrow();
        t.setTipo(request.getTipo());
        t.setMonto(request.getMonto());
        t.setFecha(request.getFecha());
        t.setCategoria(categoria);
        t.setDescripcion(request.getDescripcion());
        transaccionRepository.save(t);

        return ResponseEntity.ok(new TransaccionResponse(t));
    }

    // CU-010: Eliminar transaccion
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (!transaccionRepository.existsByIdAndUsuarioId(id, usuario.getId())) {
            return ResponseEntity.status(404).body("Transacción no encontrada");
        }
        transaccionRepository.deleteById(id);
        return ResponseEntity.ok("Transacción eliminada correctamente");
    }
    
    // CU-014: Adjuntar comprobante a una transaccion
    @PostMapping(value = "/{id}/comprobante", consumes = "multipart/form-data")
    public ResponseEntity<?> adjuntarComprobante(
            @PathVariable Long id,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo,
            Authentication authentication) {

        Usuario usuario = usuarioActual(authentication);
        if (!transaccionRepository.existsByIdAndUsuarioId(id, usuario.getId())) {
            return ResponseEntity.status(404).body("Transacción no encontrada");
        }
        Transaccion t = transaccionRepository.findById(id).orElseThrow();

        try {
            // Si ya tenia un comprobante, lo reemplazamos (nota del CdU: solo se permite uno por transaccion)
            if (t.getComprobanteUrl() != null) {
                archivoService.eliminar(t.getComprobanteUrl());
            }
            String nombreArchivo = archivoService.guardar(archivo);
            t.setComprobanteUrl(nombreArchivo);
            t.setComprobanteTipo(archivo.getContentType());
            transaccionRepository.save(t);
            return ResponseEntity.ok(new TransaccionResponse(t));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().body("Error al guardar el archivo");
        }
    }
}