package pe.edu.pucp.colegio.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Política para contraseñas nuevas, limitada también por el máximo de bytes de BCrypt. */
@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPolicy {

  String message() default
      "La contraseña debe tener al menos 8 caracteres e incluir minúscula, mayúscula, número y símbolo; el máximo es 72 bytes UTF-8";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
