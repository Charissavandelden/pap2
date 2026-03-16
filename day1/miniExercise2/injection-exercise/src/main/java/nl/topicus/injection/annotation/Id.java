package nl.topicus.injection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markeert het primaire sleutelveld van een entiteit.
 * De kolomnaam wordt standaard afgeleid van de veldnaam.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {}
