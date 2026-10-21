package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.LocalDate;

@Entity
@Table(
    name = "periodos",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_periodo_grado_seccion_fechas",
            columnNames = {"grado_id", "seccion_id", "fecha_inicio", "fecha_fin"}))
public class Periodo extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPeriodo estado;

    @OneToMany(mappedBy = "periodo")
    private Set<Matricula> matriculas = new LinkedHashSet<>();

    public Grado getGrado() {
        return grado;
    }

    public void setGrado(Grado grado) {
        this.grado = grado;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public void setSeccion(Seccion seccion) {
        this.seccion = seccion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoPeriodo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPeriodo estado) {
        this.estado = estado;
    }

    public Set<Matricula> getMatriculas() {
        return matriculas;
    }

    public void setMatriculas(Set<Matricula> matriculas) {
        new LinkedHashSet<>(this.matriculas).forEach(matricula -> matricula.setPeriodo(null));
        this.matriculas = new LinkedHashSet<>();
        if (matriculas != null) {
            matriculas.forEach(matricula -> matricula.setPeriodo(this));
        }
    }

    public void agregarMatricula(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("La matrícula no puede ser nula");
        }
        matricula.setPeriodo(this);
    }

}
