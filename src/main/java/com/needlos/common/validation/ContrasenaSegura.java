package com.needlos.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Politica de contrasenas (Manual-Fases F1): 8 a 72 caracteres, con al menos una mayuscula, una
 * minuscula, un numero y un caracter especial. El maximo de 72 es el limite de BCrypt. Unico lugar
 * donde se define la politica.
 */
@Documented
@NotBlank
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,72}$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContrasenaSegura {

    String message() default
            "La contraseña debe tener entre 8 y 72 caracteres e incluir mayúscula, "
                    + "minúscula, número y carácter especial.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
