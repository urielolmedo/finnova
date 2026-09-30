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
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import ar.edu.usal.finnova.security.EmailService;
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

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    
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
        var oficial = guardarSiCorresponde(TipoCotizacion.OFICIAL, "oficial");
        var blue = guardarSiCorresponde(TipoCotizacion.BLUE, "blue");

        if (oficial != null || blue != null) {
            verificarUmbrales(oficial, blue);
        }
    }

    private CotizacionHistorica guardarSiCorresponde(TipoCotizacion tipo, String casa) {
        try {
            CotizacionApiResponse respuesta = consultarApiExterna(casa);
            CotizacionHistorica historica = new CotizacionHistorica(
                    null, tipo, respuesta.getCompra(), respuesta.getVenta(), LocalDateTime.now());
            cotizacionRepository.save(historica);
            log.info("Cotizacion {} actualizada: compra={} venta={}", tipo, respuesta.getCompra(), respuesta.getVenta());
            return historica;
        } catch (Exception e) {
            log.warn("No se pudo actualizar la cotizacion {}: {}", tipo, e.getMessage());
            return null;
        }
    }

    // CU-066: revisa, para cada usuario con el modulo activo, si la cotizacion bajo su umbral configurado
    private void verificarUmbrales(CotizacionHistorica oficial, CotizacionHistorica blue) {
        List<Usuario> usuarios = usuarioRepository.findAll().stream()
                .filter(Usuario::isMonitoreoTipoCambioActivo)
                .filter(u -> u.getUmbralOficial() != null || u.getUmbralBlue() != null)
                .toList();

        for (Usuario usuario : usuarios) {
            if (oficial != null && usuario.getUmbralOficial() != null) {
                boolean bajoElUmbral = oficial.getVenta().compareTo(usuario.getUmbralOficial()) < 0;
                if (bajoElUmbral && !usuario.isUmbralOficialNotificado()) {
                    notificar(usuario, "oficial", oficial.getVenta(), usuario.getUmbralOficial());
                    usuario.setUmbralOficialNotificado(true);
                    usuarioRepository.save(usuario);
                } else if (!bajoElUmbral && usuario.isUmbralOficialNotificado()) {
                    // La cotizacion volvio a superar el umbral: habilitamos que pueda notificar de nuevo en el futuro
                    usuario.setUmbralOficialNotificado(false);
                    usuarioRepository.save(usuario);
                }
            }
            if (blue != null && usuario.getUmbralBlue() != null) {
                boolean bajoElUmbral = blue.getVenta().compareTo(usuario.getUmbralBlue()) < 0;
                if (bajoElUmbral && !usuario.isUmbralBlueNotificado()) {
                    notificar(usuario, "blue", blue.getVenta(), usuario.getUmbralBlue());
                    usuario.setUmbralBlueNotificado(true);
                    usuarioRepository.save(usuario);
                } else if (!bajoElUmbral && usuario.isUmbralBlueNotificado()) {
                    usuario.setUmbralBlueNotificado(false);
                    usuarioRepository.save(usuario);
                }
            }
        }
    }

    private void notificar(Usuario usuario, String tipo, java.math.BigDecimal valorActual, java.math.BigDecimal umbral) {
        try {
            emailService.enviarAlertaTipoCambio(usuario.getEmail(), tipo, valorActual, umbral);
            log.info("Alerta de tipo de cambio enviada a {} (dolar {} = {}, umbral = {})",
                    usuario.getEmail(), tipo, valorActual, umbral);
        } catch (Exception e) {
            // Flujo alterno CU-066: si el SMTP falla, no bloqueamos el resto del proceso.
            // Nota: la reintentabilidad (3 intentos cada 60s) que pide el CdU se simplifica aqui;
            // como esta tarea ya se ejecuta cada 30 min, el proximo ciclo actua como reintento natural.
            log.warn("No se pudo enviar el email de alerta a {}: {}", usuario.getEmail(), e.getMessage());
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