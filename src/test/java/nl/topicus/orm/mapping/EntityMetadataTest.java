package nl.topicus.orm.mapping;

import nl.topicus.orm.entities.Pokemon;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EntityMetadataTest {

    @Test
    void leestTabelNaamUitTableAnnotatie() {
        EntityMetadata<Pokemon> metadata = new EntityMetadata<>(Pokemon.class);
        assertEquals("pokemon", metadata.getTableName());
    }

    @Test
    void identificeertIdVeld() {
        EntityMetadata<Pokemon> metadata = new EntityMetadata<>(Pokemon.class);
        FieldMetadata idField = metadata.getIdField();
        assertNotNull(idField, "Pokemon heeft een @Id-veld");
        assertEquals("id", idField.getColumnName());
        assertTrue(idField.isId());
    }

    @Test
    void leestAlleColumnVelden() {
        EntityMetadata<Pokemon> metadata = new EntityMetadata<>(Pokemon.class);

        Set<String> kolomNamen = metadata.getAllFields().stream()
                .map(FieldMetadata::getColumnName)
                .collect(Collectors.toSet());

        assertTrue(kolomNamen.contains("name"));
        assertTrue(kolomNamen.contains("type"));
        assertTrue(kolomNamen.contains("version"));
    }
}
