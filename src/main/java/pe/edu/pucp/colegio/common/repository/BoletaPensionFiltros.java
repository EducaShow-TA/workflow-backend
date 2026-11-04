package pe.edu.pucp.colegio.common.repository;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;

/** Predicados reutilizados por las consultas SQL de detalle, conteo y totales. */
public final class BoletaPensionFiltros {
  private BoletaPensionFiltros() {}

  public static Specification<BoletaPension> listado(
      Integer estudianteId, Integer periodoId, YearMonth mes, EstadoBoleta estado) {
    return construir(estudianteId, periodoId, mes, estado, null, null, null, null, false);
  }

  public static Specification<BoletaPension> reporte(
      Integer gradoId,
      Integer seccionId,
      EstadoBoleta estado,
      LocalDate desde,
      LocalDate hasta,
      boolean soloDeuda) {
    return construir(null, null, null, estado, gradoId, seccionId, desde, hasta, soloDeuda);
  }

  private static Specification<BoletaPension> construir(
      Integer estudianteId,
      Integer periodoId,
      YearMonth mes,
      EstadoBoleta estado,
      Integer gradoId,
      Integer seccionId,
      LocalDate desde,
      LocalDate hasta,
      boolean soloDeuda) {
    return (raiz, consulta, cb) -> {
      var condiciones = new ArrayList<Predicate>();
      condiciones.add(cb.isTrue(raiz.get("active")));
      if (estudianteId != null)
        condiciones.add(
            cb.equal(
                raiz.get("compromiso").get("matricula").get("estudiante").get("id"), estudianteId));
      if (periodoId != null)
        condiciones.add(
            cb.equal(raiz.get("compromiso").get("matricula").get("periodo").get("id"), periodoId));
      if (mes != null) condiciones.add(cb.equal(raiz.get("mesCobro"), mes.atDay(1)));
      if (estado != null) condiciones.add(cb.equal(raiz.get("estadoPago"), estado));
      if (gradoId != null)
        condiciones.add(
            cb.equal(
                raiz.get("compromiso").get("matricula").get("periodo").get("grado").get("id"),
                gradoId));
      if (seccionId != null)
        condiciones.add(
            cb.equal(
                raiz.get("compromiso").get("matricula").get("periodo").get("seccion").get("id"),
                seccionId));
      if (desde != null)
        condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fechaVencimiento"), desde));
      if (hasta != null) condiciones.add(cb.lessThanOrEqualTo(raiz.get("fechaVencimiento"), hasta));
      if (soloDeuda)
        condiciones.add(raiz.get("estadoPago").in(EstadoBoleta.PENDIENTE, EstadoBoleta.EN_MORA));
      return cb.and(condiciones.toArray(Predicate[]::new));
    };
  }
}
