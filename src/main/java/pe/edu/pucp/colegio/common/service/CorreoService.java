package pe.edu.pucp.colegio.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

  private final JavaMailSender mailSender;
  private final String remitente;

  public CorreoService(JavaMailSender mailSender, @Value("${app.mail.from:}") String remitente) {
    this.mailSender = mailSender;
    this.remitente = remitente;
  }

  public void enviarCorreoRecuperacion(String destinatario, String enlace, long minutosVigencia) {

    SimpleMailMessage mensaje = new SimpleMailMessage();

    if (remitente != null && !remitente.isBlank()) {
      mensaje.setFrom(remitente);
    }

    mensaje.setTo(destinatario);
    mensaje.setSubject("Recuperación de contraseña - EducaShow");
    mensaje.setText(
        "Hola:\n\n"
            + "Recibimos una solicitud para restablecer tu contraseña.\n\n"
            + "Abre el siguiente enlace para continuar:\n"
            + enlace
            + "\n\nEste enlace vence en "
            + minutosVigencia
            + " minutos y solo puede utilizarse una vez.\n\n"
            + "Si no solicitaste este cambio, puedes ignorar este correo.\n\n"
            + "Equipo EducaShow");

    mailSender.send(mensaje);
  }
}
