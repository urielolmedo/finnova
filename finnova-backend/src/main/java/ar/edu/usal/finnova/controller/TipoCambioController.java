package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.CotizacionResponse;
import ar.edu.usal.finnova.model.CotizacionHistorica;
import ar.edu.usal.finnova.model.TipoCotizacion;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import ar.edu.usal.finnova.service.TipoCambioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ar.edu.usal.finnova.dto.*;
import ar.edu.usal.finnova.repository.CotizacionHistoricaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/tipo-cambio")
@RequiredArgsConstructor
public class TipoCambioController {

    private final TipoCambioService tipoCambioService;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionHistoricaRepository cotizacionRepository;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // CU-062: Activar/desactivar el modulo
    @PutMapping("/activar")
    public ResponseEntity<?> activarModulo(@RequestParam boolean activo, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        usuario.setMonitoreoTipoCambioActivo(activo);
        usuarioRepository.save(usuario);

        String mensaje = activo
                ? "Módulo de monitoreo de tipo de cambio activado correctamente"
                : "Módulo desactivado. El historial de cotizaciones no se elimina y podrás consultarlo si reactivás el módulo";
        return ResponseEntity.ok(mensaje);
    }

    // CU-063: Consultar cotizacion actual del dolar oficial y blue
    @GetMapping("/actual")
    public ResponseEntity<?> cotizacionActual(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (!usuario.isMonitoreoTipoCambioActivo()) {
            return ResponseEntity.badRequest().body("El módulo de monitoreo de tipo de cambio no está activo");
        }

        CotizacionHistorica oficial = tipoCambioService.obtenerCotizacionVigente(TipoCotizacion.OFICIAL, "oficial");
        CotizacionHistorica blue = tipoCambioService.obtenerCotizacionVigente(TipoCotizacion.BLUE, "blue");

        if (oficial == null || blue == null) {
            return ResponseEntity.status(503).body("No hay cotizaciones disponibles en este momento. Intentá nuevamente en unos minutos");
        }

        LocalDateTime hace30min = LocalDateTime.now().minusMinutes(30);
        var respuestaOficial = new CotizacionResponse("oficial", oficial.getCompra(), oficial.getVenta(),
                oficial.getFechaHora(), oficial.getFechaHora().isBefore(hace30min));
        var respuestaBlue = new CotizacionResponse("blue", blue.getCompra(), blue.getVenta(),
                blue.getFechaHora(), blue.getFechaHora().isBefore(hace30min));

        return ResponseEntity.ok(java.util.Map.of("oficial", respuestaOficial, "blue", respuestaBlue));
    }

    // CU-064: Historial de variacion del tipo de cambio
    @GetMapping("/historial")
    public ResponseEntity<?> historial(
            @RequestParam(defaultValue = "30") int dias, Authentication authentication) {

        Usuario usuario = usuarioActual(authentication);
        if (!usuario.isMonitoreoTipoCambioActivo()) {
            return ResponseEntity.badRequest().body("El módulo de monitoreo de tipo de cambio no está activo");
        }

        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        List<CotizacionHistorica> oficiales = cotizacionRepository
                .findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(TipoCotizacion.OFICIAL, desde);
        List<CotizacionHistorica> blues = cotizacionRepository
                .findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(TipoCotizacion.BLUE, desde);

        if (oficiales.isEmpty() && blues.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    "Todavía no hay registros históricos suficientes. El historial se construye a partir de " +
                            "la activación del módulo, con una consulta cada 30 minutos."
            );
        }

        // Armamos los puntos combinando ambas series por timestamp (asumimos que se guardan casi en simultaneo)
        List<PuntoHistorico> puntos = new ArrayList<>();
        int maxLen = Math.max(oficiales.size(), blues.size());
        for (int i = 0; i < maxLen; i++) {
            LocalDateTime fecha = i < oficiales.size() ? oficiales.get(i).getFechaHora() : blues.get(i).getFechaHora();
            BigDecimal valOficial = i < oficiales.size() ? oficiales.get(i).getVenta() : null;
            BigDecimal valBlue = i < blues.size() ? blues.get(i).getVenta() : null;
            puntos.add(new PuntoHistorico(fecha, valOficial, valBlue));
        }

        var statsOficial = calcularEstadisticas(oficiales);
        var statsBlue = calcularEstadisticas(blues);

        // Flujo alterno CU-064: si el primer registro es mas reciente que el "desde" pedido,
        // avisamos que el historial disponible es menor al rango seleccionado
        java.time.LocalDate primerRegistro = null;
        List<CotizacionHistorica> todos = new ArrayList<>();
        todos.addAll(oficiales);
        todos.addAll(blues);
        if (!todos.isEmpty()) {
            LocalDateTime masAntiguo = todos.stream().map(CotizacionHistorica::getFechaHora)
                    .min(LocalDateTime::compareTo).orElse(null);
            if (masAntiguo != null && masAntiguo.isAfter(desde.plusHours(1))) {
                primerRegistro = masAntiguo.toLocalDate();
            }
        }

        return ResponseEntity.ok(new HistorialResponse(
                puntos,
                statsOficial[0], statsOficial[1], statsOficial[2],
                statsBlue[0], statsBlue[1], statsBlue[2],
                primerRegistro
        ));
    }

    private BigDecimal[] calcularEstadisticas(List<CotizacionHistorica> registros) {
        if (registros.isEmpty()) return new BigDecimal[]{null, null, null};
        BigDecimal min = registros.stream().map(CotizacionHistorica::getVenta).min(BigDecimal::compareTo).get();
        BigDecimal max = registros.stream().map(CotizacionHistorica::getVenta).max(BigDecimal::compareTo).get();
        BigDecimal suma = registros.stream().map(CotizacionHistorica::getVenta).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(BigDecimal.valueOf(registros.size()), 2, RoundingMode.HALF_UP);
        return new BigDecimal[]{min, max, promedio};
    }

    // CU-067: Analisis comparativo actual vs promedios historicos (30/60/90 dias)
    @GetMapping("/analisis-comparativo")
    public ResponseEntity<?> analisisComparativo(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (!usuario.isMonitoreoTipoCambioActivo()) {
            return ResponseEntity.badRequest().body("El módulo de monitoreo de tipo de cambio no está activo");
        }

        var respuestaOficial = construirAnalisis(TipoCotizacion.OFICIAL, "oficial");
        var respuestaBlue = construirAnalisis(TipoCotizacion.BLUE, "blue");

        return ResponseEntity.ok(java.util.Map.of("oficial", respuestaOficial, "blue", respuestaBlue));
    }

    private AnalisisComparativoResponse construirAnalisis(TipoCotizacion tipo, String casa) {
        CotizacionHistorica actual = tipoCambioService.obtenerCotizacionVigente(tipo, casa);
        BigDecimal valorActual = actual != null ? actual.getVenta() : null;

        List<PromedioComparativo> comparativas = new ArrayList<>();
        for (int dias : new int[]{30, 60, 90}) {
            BigDecimal promedio = tipoCambioService.calcularPromedio(tipo, dias);
            if (promedio == null || valorActual == null) {
                comparativas.add(new PromedioComparativo(dias, null, null, false));
                continue;
            }
            BigDecimal desviacion = valorActual.subtract(promedio)
                    .divide(promedio, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            comparativas.add(new PromedioComparativo(dias, promedio, desviacion, desviacion.signum() < 0));
        }

        Integer diasDisponibles = null;
        long cantidadRegistros = cotizacionRepository.countByTipo(tipo);
        if (cantidadRegistros > 0) {
            LocalDateTime hace30dias = LocalDateTime.now().minusDays(30);
            long registrosUltimos30 = cotizacionRepository
                    .findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(tipo, hace30dias).size();
            if (registrosUltimos30 < cantidadRegistros) {
                // Hay menos de 30 dias de historial en total (todos los registros son mas nuevos que hace30dias)
                var primero = cotizacionRepository.findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(
                        tipo, LocalDateTime.now().minusYears(10)).stream().findFirst();
                if (primero.isPresent()) {
                    diasDisponibles = (int) java.time.temporal.ChronoUnit.DAYS.between(
                            primero.get().getFechaHora(), LocalDateTime.now());
                }
            }
        }

        return new AnalisisComparativoResponse(casa, valorActual, comparativas, diasDisponibles);
    }
}