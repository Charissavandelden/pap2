package nl.topicus.orm.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import nl.topicus.orm.annotation.Column;
import nl.topicus.orm.annotation.DiscriminatorColumn;
import nl.topicus.orm.annotation.DiscriminatorValue;
import nl.topicus.orm.annotation.Id;
import nl.topicus.orm.annotation.Table;

/**
 * Leest de annotaties van een entiteitsklasse via reflectie en slaat de volledige
 * mapping-informatie op: tabelnaam, id-veld en alle geannotteerde kolommen.
 */
public class EntityMetadata<T> {

    private final Class<T> entityClass;
    private final String tableName;
    @Nullable
    private final FieldMetadata idField;
    private final LinkedHashSet<FieldMetadata> allFields = new LinkedHashSet<>();
    private Class<?> rootEntity;
    private final DiscriminatorColumn discriminatorColumn;
    private final java.util.Map<String, EntityMetadata<? extends T>> subtypes = new java.util.HashMap<>();

    /**
     * Verwerkt de annotaties van de opgegeven entiteitsklasse en bouwt de mapping op.
     */
    public EntityMetadata(@Nonnull Class<T> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null)
        {
        	rootEntity = findRootEntity(entityClass);
        	tableAnnotation = rootEntity.getAnnotation(Table.class);
        }
        tableName = tableAnnotation.name();
        
        this.entityClass = entityClass;
        this.idField  = findIdField(isRoot() ? entityClass : rootEntity);
        this.discriminatorColumn = entityClass.getAnnotation(DiscriminatorColumn.class);

        Set<String> gezieneKolommen = new HashSet<>();

        // Doorloop de volledige klassehiërarchie zodat geërfde @Column velden ook meegenomen worden
        Class<?> huidig = entityClass;
        while (huidig != null && huidig != Object.class) {
            for (Field field : huidig.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    String colName = column.name().isBlank() ? field.getName() : column.name();
                    if (!gezieneKolommen.add(colName))
                        continue;

                    boolean hasDefaultValue = column.defaultValue() >= 1;
                    Object defaultValue = hasDefaultValue ? column.defaultValue() : null;
                    allFields.add(new FieldMetadata(field, colName, defaultValue));
                } else if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToOne.class)) {
                    // relatie-velden worden in Middag 4 uitgewerkt
                }
            }
            huidig = huidig.getSuperclass();
        }
    }
    
    public Class<T> getEntityClass() {
    	return entityClass;
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
    public LinkedHashSet<FieldMetadata> getAllFields() {
        return allFields;
    }

//    @Nonnull
//    public List<FieldMetadata> getNonIdFields() {
//        return nonIdFields;
//    }
    
    public String getDiscriminatorValue() {
    	return entityClass.getAnnotation(DiscriminatorValue.class).value();
    }
    
    public boolean isRoot() {
    	return rootEntity == null;
    }
    
    public Class<?> getRootEntity() {
    	return rootEntity;
    }
    
    public DiscriminatorColumn getDiscriminatorColumn() {
    	return discriminatorColumn;
    }
    
    public void registerSubtype(String discriminatorWaarde, EntityMetadata<? extends T> childMetadata) {
        subtypes.put(discriminatorWaarde, childMetadata);
        allFields.addAll(childMetadata.getAllFields());
    }

    public <C extends T> void registerChild(EntityMetadata<?> childMetadata) {
        allFields.addAll(childMetadata.getAllFields());
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
            // Bepaal welke klasse aangemaakt moet worden op basis van de discriminator
            Class<?> doelKlasse = entityClass;
            if (discriminatorColumn != null && !subtypes.isEmpty()) {
                String discriminatorWaarde = rs.getString(discriminatorColumn.name());
                EntityMetadata<? extends T> subtype = subtypes.get(discriminatorWaarde);
                if (subtype != null) {
                    return subtype.mapRow(rs);
                }
            }

            @SuppressWarnings("unchecked")
            T instance = (T) doelKlasse.getDeclaredConstructor().newInstance();
            if (idField != null) {
                Object idValue = rs.getObject(idField.getColumnName());
                idValue = convertType(idValue, idField.getField().getType());
                idField.setValue(instance, idValue);
            }
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
    
    private Class<?> findRootEntity(Class<?> childClass) {
    	Class<?> parent = childClass.getSuperclass();
    	while (!parent.isAnnotationPresent(Table.class))
    	{
    		parent = parent.getSuperclass();
    	}
    	return parent;
    }
    
    private FieldMetadata findIdField(Class<?> entityClass) {
    	Field idField = Stream.of(entityClass.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Id.class))
				.findFirst()
				.orElseThrow();
    	return new FieldMetadata(idField, idField.getName(), true);
    }
}
