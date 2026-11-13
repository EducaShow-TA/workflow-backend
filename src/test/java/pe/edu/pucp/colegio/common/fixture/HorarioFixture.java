package pe.edu.pucp.colegio.common.fixture;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.BloqueHorario;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.model.TipoBloqueHorario;
import pe.edu.pucp.colegio.common.model.TurnoEscolar;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;

/** Crea una asignación semanal persistida para probar permisos y horarios. */
public final class HorarioFixture {

  private HorarioFixture() {}

  public static SesionHorario asignar(
      EntityManager entityManager,
      Seccion seccion,
      Area area,
      String usernameDocente,
      LocalDate fecha,
      LocalTime inicio,
      LocalTime fin) {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    CuentaUsuario cuenta =
        entityManager
            .createQuery(
                "select c from CuentaUsuario c where c.username = :username", CuentaUsuario.class)
            .setParameter("username", usernameDocente)
            .getSingleResult();

    PlantillaHorario plantilla = new PlantillaHorario();
    plantilla.setNombre("Fixture " + sufijo);
    plantilla.setTurno(TurnoEscolar.MANANA);
    plantilla.setHoraInicio(inicio);
    plantilla.setHoraFin(fin);
    plantilla.setDuracionHoraPedagogicaMinutos(
        (int) java.time.Duration.between(inicio, fin).toMinutes());
    plantilla.setCantidadBloquesPedagogicos(1);
    entityManager.persist(plantilla);

    BloqueHorario bloque = new BloqueHorario();
    bloque.setPlantilla(plantilla);
    bloque.setOrden(1);
    bloque.setTipo(TipoBloqueHorario.PEDAGOGICO);
    bloque.setHoraInicio(inicio);
    bloque.setHoraFin(fin);
    entityManager.persist(bloque);

    HorarioSeccion horario = new HorarioSeccion();
    horario.setSeccion(seccion);
    horario.setPlantilla(plantilla);
    horario.setAnio(fecha.getYear());
    entityManager.persist(horario);

    Aula aula = new Aula();
    aula.setCodigo("FIX-" + sufijo);
    aula.setCapacidad(40);
    entityManager.persist(aula);

    SesionHorario asignacion = new SesionHorario();
    asignacion.setHorario(horario);
    asignacion.setBloque(bloque);
    asignacion.setDia(fecha.getDayOfWeek());
    asignacion.setArea(area);
    asignacion.setDocente(cuenta);
    asignacion.setAula(aula);
    asignacion.setVigenteDesde(LocalDate.of(fecha.getYear(), 1, 1));
    entityManager.persist(asignacion);
    return asignacion;
  }
}
