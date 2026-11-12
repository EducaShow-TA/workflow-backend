package pe.edu.pucp.colegio.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordPolicyValidator implements ConstraintValidator<PasswordPolicy, String> {

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null) {
      return true;
    }

    if (password.codePointCount(0, password.length()) < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72) {
      return false;
    }

    boolean minuscula = false;
    boolean mayuscula = false;
    boolean numero = false;
    boolean simbolo = false;
    for (int offset = 0; offset < password.length(); ) {
      int caracter = password.codePointAt(offset);
      minuscula |= Character.isLowerCase(caracter);
      mayuscula |= Character.isUpperCase(caracter);
      numero |= Character.isDigit(caracter);
      simbolo |= !Character.isLetterOrDigit(caracter);
      offset += Character.charCount(caracter);
    }
    return minuscula && mayuscula && numero && simbolo;
  }
}
