package pe.edu.pucp.colegio.common.repository;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;

/** SUM y COUNT DISTINCT se ejecutan en la base, sin materializar todas las boletas. */
public class BoletaPensionAgregadosImpl implements BoletaPensionAgregados {
  private final EntityManager entidades;

  public BoletaPensionAgregadosImpl(EntityManager entidades) {
    this.entidades = entidades;
  }

  @Override
  public BigDecimal sumarPendiente(Specification<BoletaPension> filtro) {
    var cb = entidades.getCriteriaBuilder();
    var consulta = cb.createQuery(BigDecimal.class);
    var raiz = consulta.from(BoletaPension.class);
    var importe = cb.sum(raiz.<BigDecimal>get("monto"), raiz.<BigDecimal>get("mora"));
    consulta.select(cb.coalesce(cb.sum(importe), BigDecimal.ZERO));
    consulta.where(
        cb.and(
            filtro.toPredicate(raiz, consulta, cb),
            cb.notEqual(raiz.get("estadoPago"), EstadoBoleta.PAGADA)));
    return entidades.createQuery(consulta).getSingleResult();
  }

  @Override
  public long contarFamiliasMorosas(Specification<BoletaPension> filtro) {
    var cb = entidades.getCriteriaBuilder();
    var consulta = cb.createQuery(Long.class);
    var raiz = consulta.from(BoletaPension.class);
    consulta.select(cb.countDistinct(raiz.get("compromiso").get("apoderado").get("id")));
    consulta.where(
        cb.and(
            filtro.toPredicate(raiz, consulta, cb),
            cb.equal(raiz.get("estadoPago"), EstadoBoleta.EN_MORA)));
    return entidades.createQuery(consulta).getSingleResult();
  }
}
