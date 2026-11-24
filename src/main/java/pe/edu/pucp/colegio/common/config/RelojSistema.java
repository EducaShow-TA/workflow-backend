package pe.edu.pucp.colegio.common.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class RelojSistema extends Clock {

  private final Clock real;
  private volatile Duration desfase = Duration.ZERO;

  public RelojSistema(Clock real) {
    this.real = real;
  }

  @Override
  public ZoneId getZone() {
    return real.getZone();
  }

  @Override
  public Clock withZone(ZoneId zona) {
    return Clock.offset(real.withZone(zona), desfase);
  }

  @Override
  public Instant instant() {
    return real.instant().plus(desfase);
  }

  public Clock real() {
    return real;
  }

  public Duration desfase() {
    return desfase;
  }

  public boolean simulado() {
    return !desfase.isZero();
  }

  public void ajustar(Duration nuevoDesfase) {
    desfase = nuevoDesfase == null ? Duration.ZERO : nuevoDesfase;
  }
}
