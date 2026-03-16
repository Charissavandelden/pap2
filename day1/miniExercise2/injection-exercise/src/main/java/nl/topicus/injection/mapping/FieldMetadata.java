package nl.topicus.injection.mapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Houdt de mapping-informatie bij voor één veld van een entiteitsklasse:
 * het bijbehorende {@link Field}, de kolomnaam in de database en of het het primaire sleutelveld is.
 */
public class FieldMetadata {

    private final Field field;
    private final String columnName;
    private final boolean isId;

    public FieldMetadata(@Nonnull Field field, @Nonnull String columnName, boolean isId) {
        this.field = field;
        this.columnName = columnName;
        this.isId = isId;
        field.setAccessible(true);
    }

    @Nonnull
    public Field getField() {
        return field;
    }

    @Nonnull
    public String getColumnName() {
        return columnName;
    }

    public boolean isId() {
        return isId;
    }

    /**
     * Leest de waarde van dit veld uit het gegeven object via reflectie.
     */
    @Nullable
    public Object getValue(@Nonnull Object instance) throws IllegalAccessException {
        return field.get(instance);
    }

    /**
     * Schrijft een waarde naar dit veld op het gegeven object via reflectie.
     */
    public void setValue(@Nonnull Object instance, @Nullable Object value) throws IllegalAccessException {
        field.set(instance, value);
    }
}
