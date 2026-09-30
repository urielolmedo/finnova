package ar.edu.usal.finnova.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void enviarMailRecuperacion(String destinatario, String token) {
        String link = frontendUrl + "/resetear-password?token=" + token;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("FinNova - Recuperación de contraseña");
        mensaje.setText(
                "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
                        "Hacé click en el siguiente link para elegir una nueva (válido por 15 minutos):\n" +
                        link + "\n\n" +
                        "Si vos no pediste esto, ignorá este mail."
        );
        mailSender.send(mensaje);
    }
        public void enviarAlertaTipoCambio(String destinatario, String tipoCotizacion, java.math.BigDecimal valorActual, java.math.BigDecimal umbral) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("FinNova - Alerta de tipo de cambio favorable");
        mensaje.setText(
            "El dólar " + tipoCotizacion + " bajó de tu umbral configurado.\n\n" +
            "Cotización actual: $" + valorActual + "\n" +
            "Tu umbral configurado: $" + umbral + "\n\n" +
            "Esta información es orientativa y no constituye asesoramiento financiero profesional.\n\n" +
            "Podés desactivar esta alerta o ajustar el umbral desde la configuración del módulo de tipo de cambio en FinNova."
        );
        mailSender.send(mensaje);
    }
}