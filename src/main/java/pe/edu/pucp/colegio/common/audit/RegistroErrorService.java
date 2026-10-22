package pe.edu.pucp.colegio.common.audit;

import java.time.LocalDate;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.data.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/**
 * Persiste en {@code registros_error} las fallas técnicas del sistema (US-5.5).
 * Usa una transacción propia para que el registro se guarde aunque la petición
 * original haga rollback. Si el registro falla, no se propaga la excepción: el
 * error original siempre debe llegar al cliente.
 */
@Service
public class RegistroErrorService {

    private static final Logger log = LoggerFactory.getLogger(RegistroErrorService.class);
    private static final int MAX = 255;
    private static final int MAX_TRAZA = 4000;
    private static final String PREFIJO_API = "/api/v1/";

    private final RegistroErrorRepository errores;
    private final CuentaUsuarioRepository cuentas;

    public RegistroErrorService(
            RegistroErrorRepository errores, CuentaUsuarioRepository cuentas) {
        this.errores = errores;
        this.cuentas = cuentas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
            NivelLog nivel,
            String codigo,
            String metodo,
            String endpoint,
            int estado,
            String mensaje,
            Throwable causa) {
        try {
            RegistroError registro
                    = new RegistroError(
                            nivel, codigo, metodo, recortar(endpoint, MAX), estado, recortar(mensaje, MAX));
            registro.setModulo(deducirModulo(endpoint));
            registro.setIdUsuario(usuarioActual());
            if (causa != null && estado >= 500) {
                registro.setStackTrace(recortar(trazaDe(causa), MAX_TRAZA));
            }
            errores.save(registro);
        } catch (RuntimeException e) {
            log.error("event=registro_error_fallido codigo={} causa={}", codigo, e.toString());
        }
    }

    @Transactional(readOnly = true)
    public Page<RegistroError> consultar(
            NivelLog nivel, String modulo, LocalDate desde, LocalDate hasta, Pageable pageable) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        Specification<RegistroError> spec = (r, q, cb) -> cb.conjunction();
        if (nivel != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("nivel"), nivel));
        }
        if (modulo != null && !modulo.isBlank()) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("modulo"), modulo.trim()));
        }
        if (desde != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("fechaHora"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            spec = spec.and((r, q, cb) -> cb.lessThan(r.get("fechaHora"), hasta.plusDays(1).atStartOfDay()));
        }
        return errores.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public RegistroError obtener(Integer id) {
        return errores
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de error no encontrado"));
    }

    private Integer usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return cuentas.findByUsername(auth.getName()).map(CuentaUsuario::getId).orElse(null);
    }

    static String deducirModulo(String endpoint) {
        if (endpoint == null || !endpoint.startsWith(PREFIJO_API)) {
            return "sistema";
        }
        String resto = endpoint.substring(PREFIJO_API.length());
        int corte = resto.indexOf('/');
        String modulo = corte < 0 ? resto : resto.substring(0, corte);
        return modulo.isBlank() ? "sistema" : recortar(modulo, 50);
    }

    private static String trazaDe(Throwable causa) {
        StringWriter texto = new StringWriter();
        causa.printStackTrace(new PrintWriter(texto));
        return texto.toString();
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
