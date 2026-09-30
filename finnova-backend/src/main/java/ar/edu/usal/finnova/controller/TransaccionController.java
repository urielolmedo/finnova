package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.TransaccionRequest;
import ar.edu.usal.finnova.dto.TransaccionResponse;
import ar.edu.usal.finnova.model.Categoria;
import ar.edu.usal.finnova.model.CotizacionHistorica;
import ar.edu.usal.finnova.model.TipoCotizacion;
import ar.edu.usal.finnova.model.Transaccion;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.CategoriaRepository;
import ar.edu.usal.finnova.repository.TransaccionRepository;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ar.edu.usal.finnova.dto.EquivalenteHistorico;
import ar.edu.usal.finnova.dto.SimulacionRequest;
import ar.edu.usal.finnova.dto.SimulacionResponse;
import ar.edu.usal.finnova.dto.SugerenciaResponse;
import ar.edu.usal.finnova.service.TipoCambioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        private static final String AVISO_ORIENTATIVO =
        "Esta información es orientativa y no constituye asesoramiento financiero profesional.";

    // CU-068: Sugerencia de compra en pesos o dolares
    @GetMapping("/sugerencia")
    public ResponseEntity<?> sugerencia(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (!usuario.isMonitoreoTipoCambioActivo()) {
            return ResponseEntity.badRequest().body("El módulo de monitoreo de tipo de cambio no está activo");
        }

        long cantidadRegistros = cotizacionRepository.countByTipo(TipoCotizacion.BLUE);
        LocalDateTime hace90dias = LocalDateTime.now().minusDays(90);
        var registros90dias = cotizacionRepository.findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(TipoCotizacion.BLUE, hace90dias);

        // Flujo alterno CU-068: menos de 30 dias de historial
        if (!registros90dias.isEmpty()) {
            LocalDateTime masAntiguo = registros90dias.get(0).getFechaHora();
            long diasDisponibles = java.time.temporal.ChronoUnit.DAYS.between(masAntiguo, LocalDateTime.now());
            if (diasDisponibles < 30) {
                return ResponseEntity.ok(new SugerenciaResponse(
                    "Todavía no hay suficiente historial para generar una sugerencia representativa.",
                    "Se requieren al menos 30 días de historial acumulado.",
                    AVISO_ORIENTATIVO,
                    (int) diasDisponibles
                ));
            }
        } else {
            return ResponseEntity.ok(new SugerenciaResponse(
                "Todavía no hay historial suficiente para generar una sugerencia.",
                "Se requieren al menos 30 días de historial acumulado.",
                AVISO_ORIENTATIVO,
                0
            ));
        }

        CotizacionHistorica actual = tipoCambioService.obtenerCotizacionVigente(TipoCotizacion.BLUE, "blue");
        BigDecimal promedio90 = tipoCambioService.calcularPromedio(TipoCotizacion.BLUE, 90);

        BigDecimal desviacion = actual.getVenta().subtract(promedio90)
                .divide(promedio90, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        String mensaje;
        String criterio = "Se compara la cotización actual del dólar blue contra su promedio de los últimos 90 días. " +
                "Una desviación de más de ±5% se considera significativa.";

        if (desviacion.compareTo(BigDecimal.valueOf(-5)) < 0) {
            mensaje = "El tipo de cambio actual es favorable para la compra de dólares respecto a su comportamiento reciente.";
        } else if (desviacion.compareTo(BigDecimal.valueOf(5)) > 0) {
            mensaje = "El tipo de cambio actual está por encima de su promedio reciente; en términos históricos podría ser más conveniente operar en pesos.";
        } else {
            mensaje = "El tipo de cambio actual se encuentra en un nivel cercano a su promedio reciente, sin una tendencia definida.";
        }

        return ResponseEntity.ok(new SugerenciaResponse(mensaje, criterio, AVISO_ORIENTATIVO, null));
    }

    // CU-069: Simular escenario de compra en dolares vs pesos
    @PostMapping("/simular")
    public ResponseEntity<?> simular(@RequestBody SimulacionRequest request, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (!usuario.isMonitoreoTipoCambioActivo()) {
            return ResponseEntity.badRequest().body("El módulo de monitoreo de tipo de cambio no está activo");
        }
        if (request.getMontoPesos() == null || request.getMontoPesos().signum() <= 0) {
            return ResponseEntity.badRequest().body("El monto debe ser un valor numérico positivo");
        }
        String casa = request.getTipoCotizacion();
        if (!"oficial".equalsIgnoreCase(casa) && !"blue".equalsIgnoreCase(casa)) {
            return ResponseEntity.badRequest().body("El tipo de cotización debe ser 'oficial' o 'blue'");
        }

        TipoCotizacion tipo = TipoCotizacion.valueOf(casa.toUpperCase());
        CotizacionHistorica actual = tipoCambioService.obtenerCotizacionVigente(tipo, casa.toLowerCase());
        if (actual == null) {
            return ResponseEntity.status(503).body("No hay cotizaciones disponibles en este momento");
        }

        boolean desactualizada = actual.getFechaHora().isBefore(LocalDateTime.now().minusMinutes(30));
        BigDecimal equivalenteActual = request.getMontoPesos().divide(actual.getVenta(), 2, RoundingMode.HALF_UP);

        List<EquivalenteHistorico> historicos = new ArrayList<>();
        for (int dias : new int[]{30, 60, 90}) {
            BigDecimal promedio = tipoCambioService.calcularPromedio(tipo, dias);
            if (promedio == null) {
                historicos.add(new EquivalenteHistorico(dias, null, null));
                continue;
            }
            BigDecimal equivalenteHistorico = request.getMontoPesos().divide(promedio, 2, RoundingMode.HALF_UP);
            BigDecimal diferencia = actual.getVenta().subtract(promedio)
                    .divide(promedio, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            historicos.add(new EquivalenteHistorico(dias, equivalenteHistorico, diferencia));
        }

        String aviso = AVISO_ORIENTATIVO + (desactualizada
                ? " La cotización utilizada corresponde a la última disponible (" + actual.getFechaHora() + "), no a un valor en tiempo real."
                : "");

        return ResponseEntity.ok(new SimulacionResponse(
                request.getMontoPesos(), casa.toLowerCase(), equivalenteActual, historicos, aviso, desactualizada
        ));
    }
    
}