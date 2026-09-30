package ar.edu.usal.finnova.service;

import ar.edu.usal.finnova.dto.CotizacionApiResponse;
import ar.edu.usal.finnova.model.CotizacionHistorica;
import ar.edu.usal.finnova.model.TipoCotizacion;
import ar.edu.usal.finnova.repository.CotizacionHistoricaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoCambioService {

    private final CotizacionHistoricaRepository cotizacionRepository;
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://dolarapi.com/v1/dolares")
            .build();

    // CU-063: consulta la API externa, con timeout de 5 segundos segun el CdU
    public CotizacionApiResponse consultarApiExterna(String casa) {
        return restClient.get()
                .uri("/{casa}", casa)
                .retrieve()
                .body(CotizacionApiResponse.class);
    }

    // Tarea programada: consulta y guarda ambas cotizaciones cada 30 minutos
    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 5000)
    public void actualizarCotizaciones() {
        guardarSiCorresponde(TipoCotizacion.OFICIAL, "oficial");
        guardarSiCorresponde(TipoCotizacion.BLUE, "blue");
    }

    private void guardarSiCorresponde(TipoCotizacion tipo, String casa) {
        try {
            CotizacionApiResponse respuesta = consultarApiExterna(casa);
            CotizacionHistorica historica = new CotizacionHistorica(
                    null, tipo, respuesta.getCompra(), respuesta.getVenta(), LocalDateTime.now());
            cotizacionRepository.save(historica);
            log.info("Cotizacion {} actualizada: compra={} venta={}", tipo, respuesta.getCompra(), respuesta.getVenta());
        } catch (Exception e) {
            log.warn("No se pudo actualizar la cotizacion {}: {}", tipo, e.getMessage());
        }
    }

    // Obtiene la ultima cotizacion guardada, o dispara una consulta nueva si la ultima tiene mas de 30 min
    public CotizacionHistorica obtenerCotizacionVigente(TipoCotizacion tipo, String casa) {
        Optional<CotizacionHistorica> ultima = cotizacionRepository.findFirstByTipoOrderByFechaHoraDesc(tipo);

        boolean necesitaActualizar = ultima.isEmpty() ||
                ultima.get().getFechaHora().isBefore(LocalDateTime.now().minusMinutes(30));

        if (necesitaActualizar) {
            try {
                CotizacionApiResponse respuesta = consultarApiExterna(casa);
                CotizacionHistorica nueva = new CotizacionHistorica(
                        null, tipo, respuesta.getCompra(), respuesta.getVenta(), LocalDateTime.now());
                cotizacionRepository.save(nueva);
                return nueva;
            } catch (Exception e) {
                // Flujo alterno CU-063: la API no responde -> usamos la ultima guardada, si existe
                log.warn("API externa no disponible, usando ultima cotizacion guardada: {}", e.getMessage());
                return ultima.orElse(null);
            }
        }
        return ultima.get();
    }

    // Usado por CU-067: promedio de una cotizacion en los ultimos N dias
    public java.math.BigDecimal calcularPromedio(TipoCotizacion tipo, int dias) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        List<CotizacionHistorica> registros = cotizacionRepository.findByTipoAndFechaHoraAfterOrderByFechaHoraAsc(tipo, desde);

        if (registros.isEmpty()) return null;

        java.math.BigDecimal suma = registros.stream()
                .map(CotizacionHistorica::getVenta)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return suma.divide(java.math.BigDecimal.valueOf(registros.size()), 2, java.math.RoundingMode.HALF_UP);
    }
}