package nl.topicus.injection.mapping;

import nl.topicus.injection.annotation.Column;
import nl.topicus.injection.annotation.Id;
import nl.topicus.injection.annotation.Table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leest de annotaties van een entiteitsklasse via reflectie en slaat de volledige
 * mapping-informatie op: tabelnaam, id-veld en alle geannotteerde kolommen.
 */
public class EntityMetadata<T> {

    private final Class<T> entityClass;
    private final String tableName;
    @Nullable
    private final FieldMetadata idField;
    private final List<FieldMetadata> allFields;
    private final List<FieldMetadata> nonIdFields;

    /**
     * Verwerkt de annotaties van de opgegeven entiteitsklasse en bouwt de mapping op.
     */
    public EntityMetadata(@Nonnull Class<T> entityClass) {
        this.entityClass = entityClass;

        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        this.tableName = (tableAnnotation != null) ? tableAnnotation.name() : entityClass.getSimpleName().toLowerCase();

        List<FieldMetadata> all = new ArrayList<>();
        List<FieldMetadata> nonId = new ArrayList<>();
        FieldMetadata tempId = null;

        for (Field field : entityClass.getDeclaredFields()) {
            boolean hasId = field.isAnnotationPresent(Id.class);
            boolean hasColumn = field.isAnnotationPresent(Column.class);

            if (!hasId && !hasColumn) continue;

            String colName = field.getName();
            if (hasColumn) {
                Column col = field.getAnnotation(Column.class);
                if (!col.name().isEmpty()) {
                    colName = col.name();
                }
            }

            FieldMetadata meta = new FieldMetadata(field, colName, hasId);
            all.add(meta);
            if (hasId) {
                tempId = meta;
            } else {
                nonId.add(meta);
            }
        }

        this.idField = tempId;
        this.allFields = Collections.unmodifiableList(all);
        this.nonIdFields = Collections.unmodifiableList(nonId);
    }

    @Nonnull
    public String getTableName() {
        return tableName;
    }

    @Nullable
    public FieldMetadata getIdField() {
        return idField;
    }

    @Nonnull
    public List<FieldMetadata> getAllFields() {
        return allFields;
    }

    @Nonnull
    public List<FieldMetadata> getNonIdFields() {
        return nonIdFields;
    }

    /**
     * Mapt de huidige rij van de gegeven {@link ResultSet} naar een nieuwe instantie
     * van de entiteitsklasse via reflectie.
     *
     * @throws SQLException als het aanmaken of vullen van de instantie mislukt
     */
    @Nonnull
    public T mapRow(@Nonnull ResultSet rs) throws SQLException {
        try {
            T instance = entityClass.getDeclaredConstructor().newInstance();
            for (FieldMetadata fieldMeta : allFields) {
                Object value = rs.getObject(fieldMeta.getColumnName());
                value = convertType(value, fieldMeta.getField().getType());
                fieldMeta.setValue(instance, value);
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Fout bij mappen van ResultSet naar " + entityClass.getSimpleName(), e);
        }
    }

    /**
     * Converteert een waarde uit de database naar het juiste Java primitief type
     * om type-mismatch fouten bij reflectie te voorkomen.
     */
    @Nullable
    private Object convertType(@Nullable Object value, @Nonnull Class<?> targetType) {
        if (value == null) return null;
        if (value instanceof Number number) {
            if (targetType == int.class || targetType == Integer.class) return number.intValue();
            if (targetType == long.class || targetType == Long.class) return number.longValue();
            if (targetType == double.class || targetType == Double.class) return number.doubleValue();
            if (targetType == float.class || targetType == Float.class) return number.floatValue();
        }
        return value;
    }
}
