package nl.topicus.injection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Koppelt een veld aan een databasekolom. Wanneer {@code name} leeg is,
 * wordt de veldnaam als kolomnaam gebruikt.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    String name() default "";
}
