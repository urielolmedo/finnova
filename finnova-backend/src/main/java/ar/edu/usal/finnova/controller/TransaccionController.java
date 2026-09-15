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

        int cantidadRepeticiones = 1;

        if (request.isEsRecurrente()) {
            if (request.getFrecuencia() == null) {
                return ResponseEntity.badRequest().body("La frecuencia es obligatoria para una transacción recurrente");
            }
            boolean tieneCantidad = request.getCantidadRepeticiones() != null;
            boolean tieneFechaFin = request.getFechaFinRecurrencia() != null;

            if (tieneCantidad == tieneFechaFin) {
                // los dos cargados o los dos vacios: no sabemos cual usar
                return ResponseEntity.badRequest().body("Indicá la cantidad de repeticiones o una fecha de fin, pero no ambas ni ninguna");
            }

            if (tieneCantidad) {
                if (request.getCantidadRepeticiones() < 1 || request.getCantidadRepeticiones() > 60) {
                    return ResponseEntity.badRequest().body("La cantidad de repeticiones debe ser un número entre 1 y 60");
                }
                cantidadRepeticiones = request.getCantidadRepeticiones();
            } else {
                if (!request.getFechaFinRecurrencia().isAfter(request.getFecha())) {
                    return ResponseEntity.badRequest().body("La fecha de fin debe ser posterior a la fecha de la transacción original");
                }
                // Convertimos la fecha de fin a cantidad de repeticiones, para generar igual en ambos modos
                cantidadRepeticiones = 1;
                LocalDate siguiente = original.getFecha();
                while (true) {
                    LocalDate candidata = calcularSiguienteFecha(siguiente, request.getFrecuencia());
                    if (candidata.isAfter(request.getFechaFinRecurrencia())) break;
                    siguiente = candidata;
                    cantidadRepeticiones++;
                }
            }

            original.setFrecuencia(request.getFrecuencia());

            // Guardamos la fecha de la ultima instancia como referencia informativa, sea cual sea el modo elegido
            LocalDate fechaUltima = original.getFecha();
            for (int i = 1; i < cantidadRepeticiones; i++) {
                fechaUltima = calcularSiguienteFecha(fechaUltima, request.getFrecuencia());
            }
            original.setFechaFinRecurrencia(fechaUltima);
        }

        transaccionRepository.save(original);

        int instanciasGeneradas = 1;
        if (request.isEsRecurrente()) {
            LocalDate siguiente = original.getFecha();
            for (int i = 1; i < cantidadRepeticiones; i++) {
                siguiente = calcularSiguienteFecha(siguiente, request.getFrecuencia());
                Transaccion instancia = new Transaccion();
                instancia.setUsuario(usuario);
                instancia.setTipo(original.getTipo());
                instancia.setMonto(original.getMonto());
                instancia.setFecha(siguiente);
                instancia.setCategoria(categoria);
                instancia.setDescripcion(original.getDescripcion());
                instancia.setEsRecurrente(false);
                instancia.setTransaccionOrigen(original);
                transaccionRepository.save(instancia);
                instanciasGeneradas++;
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