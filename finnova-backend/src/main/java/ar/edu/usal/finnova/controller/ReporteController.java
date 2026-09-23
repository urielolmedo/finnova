package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.ReporteMensualResponse;
import ar.edu.usal.finnova.dto.ResumenFinancieroResponse;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import ar.edu.usal.finnova.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ar.edu.usal.finnova.dto.EvolucionMensualResponse;
import ar.edu.usal.finnova.dto.ReportePersonalizadoResponse;
import java.time.temporal.ChronoUnit;
import java.util.List;
import ar.edu.usal.finnova.service.ReportePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.format.DateTimeFormatter;
import ar.edu.usal.finnova.dto.CategoriaResumen;
import java.nio.charset.StandardCharsets;
import ar.edu.usal.finnova.dto.CompartirReporteResponse;
import ar.edu.usal.finnova.dto.ReporteCompartidoResponse;
import ar.edu.usal.finnova.model.ReporteCompartido;
import ar.edu.usal.finnova.repository.ReporteCompartidoRepository;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioRepository usuarioRepository;
    private final ReportePdfService reportePdfService;
    private final ReporteCompartidoRepository reporteCompartidoRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // CU-022: Resumen financiero general (dashboard). Por defecto, el mes en curso.
    @GetMapping("/resumen")
    public ResumenFinancieroResponse resumen(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            Authentication authentication) {

        Usuario usuario = usuarioActual(authentication);
        if (desde == null || hasta == null) {
            YearMonth mesActual = YearMonth.now();
            desde = mesActual.atDay(1);
            hasta = mesActual.atEndOfMonth();
        }
        return reporteService.calcularResumen(usuario.getId(), desde, hasta);
    }

    // CU-023: Reporte mensual, con desglose por categoria
    @GetMapping("/mensual")
    public ResponseEntity<?> reporteMensual(
            @RequestParam int anio, @RequestParam int mes, Authentication authentication) {

        if (mes < 1 || mes > 12) {
            return ResponseEntity.badRequest().body("El mes debe estar entre 1 y 12");
        }
        Usuario usuario = usuarioActual(authentication);
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        ResumenFinancieroResponse resumen = reporteService.calcularResumen(usuario.getId(), desde, hasta);
        var porCategoria = reporteService.calcularPorCategoria(usuario.getId(), desde, hasta);

        return ResponseEntity.ok(new ReporteMensualResponse(anio, mes, resumen, porCategoria));
    }
    // CU-024: Reporte por categoria en un periodo
    @GetMapping("/por-categoria")
    public ResponseEntity<?> porCategoria(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        Usuario usuario = usuarioActual(authentication);
        return ResponseEntity.ok(reporteService.calcularPorCategoria(usuario.getId(), desde, hasta));
    }

    // CU-025: Balance de ingresos vs egresos (mismo calculo que el resumen, expuesto con su propio endpoint)
    @GetMapping("/balance")
    public ResponseEntity<?> balance(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        Usuario usuario = usuarioActual(authentication);
        return ResponseEntity.ok(reporteService.calcularResumen(usuario.getId(), desde, hasta));
    }

    // CU-026: Evolucion financiera en el tiempo (totales mes a mes)
    @GetMapping("/evolucion")
    public ResponseEntity<?> evolucion(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        long mesesEntre = ChronoUnit.MONTHS.between(YearMonth.from(desde), YearMonth.from(hasta));
        if (mesesEntre < 1) {
            return ResponseEntity.badRequest().body(
                    "Se requieren al menos dos meses en el rango para generar el gráfico de evolución"
            );
        }
        Usuario usuario = usuarioActual(authentication);
        List<EvolucionMensualResponse> evolucion = reporteService.calcularEvolucion(usuario.getId(), desde, hasta);
        return ResponseEntity.ok(evolucion);
    }

    // CU-027: Reporte personalizado por rango de fechas (maximo 24 meses)
    @GetMapping("/personalizado")
    public ResponseEntity<?> reportePersonalizado(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("El rango de fechas es inválido: la fecha de inicio es posterior a la fecha de fin");
        }
        long mesesEntre = ChronoUnit.MONTHS.between(YearMonth.from(desde), YearMonth.from(hasta));
        if (mesesEntre > 24) {
            return ResponseEntity.badRequest().body("El rango máximo permitido es de 24 meses");
        }

        Usuario usuario = usuarioActual(authentication);
        ResumenFinancieroResponse resumen = reporteService.calcularResumen(usuario.getId(), desde, hasta);
        var porCategoria = reporteService.calcularPorCategoria(usuario.getId(), desde, hasta);

        return ResponseEntity.ok(new ReportePersonalizadoResponse(desde, hasta, resumen, porCategoria));
    }

    // CU-028: Exportar reporte en PDF (usa el rango dado; funciona para cualquiera de los reportes anteriores)
    @GetMapping("/exportar/pdf")
    public ResponseEntity<?> exportarPdf(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "Reporte personalizado") String titulo,
            Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("El rango de fechas es inválido");
        }

        try {
            Usuario usuario = usuarioActual(authentication);
            ResumenFinancieroResponse resumen = reporteService.calcularResumen(usuario.getId(), desde, hasta);
            var porCategoria = reporteService.calcularPorCategoria(usuario.getId(), desde, hasta);

            byte[] pdf = reportePdfService.generarPdfReporte(
                    titulo, desde, hasta, usuario.getNombre() + " " + usuario.getApellido(), resumen, porCategoria);

            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String nombreArchivo = "FinNova_Reporte_" + titulo.replace(" ", "") + "_" + fecha + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("No fue posible generar el archivo PDF. Intentá nuevamente.");
        }
    }
    // CU-029: Exportar reporte en Excel/CSV
    @GetMapping("/exportar/csv")
    public ResponseEntity<byte[]> exportarReporteCsv(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "Reporte personalizado") String titulo,
            Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("El rango de fechas es inválido".getBytes(StandardCharsets.UTF_8));
        }

        Usuario usuario = usuarioActual(authentication);
        ResumenFinancieroResponse resumen = reporteService.calcularResumen(usuario.getId(), desde, hasta);
        var porCategoria = reporteService.calcularPorCategoria(usuario.getId(), desde, hasta);

        StringBuilder csv = new StringBuilder();
        csv.append(titulo).append('\n');
        csv.append("Periodo;").append(desde).append(" al ").append(hasta).append('\n');
        csv.append('\n');

        csv.append("Resumen financiero\n");
        csv.append("Total ingresos;").append(resumen.getTotalIngresos()).append('\n');
        csv.append("Total egresos;").append(resumen.getTotalEgresos()).append('\n');
        csv.append("Balance;").append(resumen.getBalance())
                .append(" (").append(resumen.isSuperavit() ? "superavit" : "deficit").append(")\n");
        csv.append('\n');

        csv.append("Desglose por categoria\n");
        csv.append("Categoria;Ingresos;Egresos\n");
        for (CategoriaResumen c : porCategoria) {
            csv.append(c.getCategoriaNombre()).append(';')
                    .append(c.getTotalIngresos()).append(';')
                    .append(c.getTotalEgresos()).append('\n');
        }

        // BOM UTF-8, mismo criterio que en el Modulo 10, para que Excel muestre bien tildes y "ñ"
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[bom.length + contenido.length];
        System.arraycopy(bom, 0, resultado, 0, bom.length);
        System.arraycopy(contenido, 0, resultado, bom.length, contenido.length);

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String nombreArchivo = "FinNova_Reporte_" + titulo.replace(" ", "") + "_" + fecha + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resultado);
    }
    // CU-030 (parte 1): Generar un link temporal para compartir el reporte
    @PostMapping("/compartir")
    public ResponseEntity<?> compartirReporte(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "Reporte personalizado") String titulo,
            Authentication authentication) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().body("El rango de fechas es inválido");
        }

        Usuario usuario = usuarioActual(authentication);
        String token = UUID.randomUUID().toString();
        LocalDateTime expiracion = LocalDateTime.now().plusHours(24);

        ReporteCompartido compartido = new ReporteCompartido(
                null, token, usuario, titulo, desde, hasta, LocalDateTime.now(), expiracion);
        reporteCompartidoRepository.save(compartido);

        String urlPublica = frontendUrl + "/reporte-compartido/" + token;
        return ResponseEntity.ok(new CompartirReporteResponse(
                token, urlPublica, expiracion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
    }

    // CU-030 (parte 2): Ver un reporte compartido (endpoint publico, sin JWT)
    @GetMapping("/compartido/{token}")
    public ResponseEntity<?> verReporteCompartido(@PathVariable String token) {
        ReporteCompartido compartido = reporteCompartidoRepository.findByToken(token).orElse(null);

        if (compartido == null) {
            return ResponseEntity.status(404).body("El link no existe o ya no está disponible");
        }
        if (compartido.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(410).body("Este link para compartir venció. Pedile a la persona que lo generó que cree uno nuevo.");
        }

        Usuario usuario = compartido.getUsuario();
        ResumenFinancieroResponse resumen = reporteService.calcularResumen(
                usuario.getId(), compartido.getDesde(), compartido.getHasta());
        var porCategoria = reporteService.calcularPorCategoria(
                usuario.getId(), compartido.getDesde(), compartido.getHasta());

        return ResponseEntity.ok(new ReporteCompartidoResponse(
                compartido.getTitulo(), compartido.getDesde(), compartido.getHasta(),
                usuario.getNombre() + " " + usuario.getApellido(), resumen, porCategoria));
    }
}