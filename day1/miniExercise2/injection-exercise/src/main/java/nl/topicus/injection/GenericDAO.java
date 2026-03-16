package nl.topicus.injection;

import nl.topicus.injection.mapping.EntityMetadata;
import nl.topicus.injection.mapping.FieldMetadata;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstracte generieke DAO die via {@link EntityMetadata} dynamisch SQL genereert
 * voor de vijf standaard CRUD-operaties. Subklassen hoeven alleen {@link #getConnection()}
 * te implementeren.
 *
 * @param <T> het type van de beheerde entiteit
 */
public abstract class GenericDAO<T> {

    protected final EntityMetadata<T> metadata;

    protected GenericDAO(@Nonnull Class<T> entityClass) {
        this.metadata = new EntityMetadata<>(entityClass);
    }

    /**
     * Levert een actieve databaseverbinding. De aanroepende code sluit de verbinding.
     */
    protected abstract Connection getConnection() throws SQLException;

    /**
     * Haalt alle entiteiten op uit de databasetabel.
     *
     * @return lijst van alle entiteiten, leeg wanneer de tabel geen rijen bevat
     */
    @Nonnull
    public List<T> findAll() throws SQLException {
        String sql = "SELECT * FROM " + metadata.getTableName();
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(metadata.mapRow(rs));
            }
        }
        return results;
    }

    /**
     * Zoekt één entiteit op basis van het primaire sleutelveld.
     *
     * @param id de waarde van het primaire sleutelveld
     * @return een {@link Optional} met de gevonden entiteit, of leeg wanneer niet gevonden
     */
    @Nonnull
    public Optional<T> findById(@Nonnull Object id) throws SQLException {
        FieldMetadata idField = vereistIdVeld();
        String sql = "SELECT * FROM " + metadata.getTableName() + " WHERE " + idField.getColumnName() + " = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(metadata.mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Slaat een nieuwe entiteit op in de database via een INSERT-statement.
     * Het door de database gegenereerde id wordt teruggeschreven op het object.
     *
     * @param entity de op te slaan entiteit
     */
    public void save(@Nonnull T entity) throws SQLException {
        List<FieldMetadata> nonIdFields = metadata.getNonIdFields();
        String cols = nonIdFields.stream().map(FieldMetadata::getColumnName).collect(Collectors.joining(", "));
        String placeholders = nonIdFields.stream().map(f -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + metadata.getTableName() + " (" + cols + ") VALUES (" + placeholders + ")";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < nonIdFields.size(); i++) {
                stmt.setObject(i + 1, nonIdFields.get(i).getValue(entity));
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next() && metadata.getIdField() != null) {
                    metadata.getIdField().setValue(entity, keys.getLong(1));
                }
            }
        } catch (IllegalAccessException e) {
            throw new SQLException("Fout bij opslaan van " + entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Werkt een bestaande entiteit bij in de database via een UPDATE-statement.
     * Het primaire sleutelveld wordt gebruikt om de juiste rij te identificeren.
     *
     * @param entity de bij te werken entiteit met het gevulde id-veld
     */
    public void update(@Nonnull T entity) throws SQLException {
        FieldMetadata idField = vereistIdVeld();
        List<FieldMetadata> nonIdFields = metadata.getNonIdFields();
        String setClauses = nonIdFields.stream()
                .map(f -> f.getColumnName() + " = ?")
                .collect(Collectors.joining(", "));
        String sql = "UPDATE " + metadata.getTableName() + " SET " + setClauses
                + " WHERE " + idField.getColumnName() + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < nonIdFields.size(); i++) {
                stmt.setObject(i + 1, nonIdFields.get(i).getValue(entity));
            }
            stmt.setObject(nonIdFields.size() + 1, idField.getValue(entity));
            stmt.executeUpdate();
        } catch (IllegalAccessException e) {
            throw new SQLException("Fout bij updaten van " + entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Verwijdert de entiteit met het opgegeven id via een DELETE-statement.
     *
     * @param id de waarde van het primaire sleutelveld
     */
    public void delete(@Nonnull Object id) throws SQLException {
        FieldMetadata idField = vereistIdVeld();
        String sql = "DELETE FROM " + metadata.getTableName() + " WHERE " + idField.getColumnName() + " = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Controleert of de entiteit een @Id-veld heeft en gooit een uitzondering wanneer dat niet zo is.
     */
    @Nonnull
    private FieldMetadata vereistIdVeld() throws SQLException {
        FieldMetadata idField = metadata.getIdField();
        if (idField == null) {
            throw new SQLException("Geen @Id veld gevonden op entiteit: " + metadata.getTableName());
        }
        return idField;
    }
}
