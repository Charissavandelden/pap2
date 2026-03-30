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
public abstract class GenericRepository<T> {

    protected final EntityMetadata<T> metadata;
    TransactionManager transactionManager;

    protected GenericRepository(@Nonnull Class<T> entityClass) {
        this.metadata = new EntityMetadata<>(entityClass);
        this.transactionManager = new TransactionManager();
    }

    protected GenericRepository(@Nonnull Class<T> entityClass, @Nonnull TransactionManager transactionManager) {
        this.metadata = new EntityMetadata<>(entityClass);
        this.transactionManager = transactionManager;
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
        List<T> results = new ArrayList<>();

        transactionManager.runInTransaction(() -> {
            try {
                String sql = "SELECT * FROM " + metadata.getTableName();
                try (Connection conn = getConnection();
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(metadata.mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e); // must wrap checked exception
            }
        });

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
        List<T> results = new ArrayList<>();
        transactionManager.runInTransaction(() -> {
            try {
                FieldMetadata idField = vereistIdVeld();
                String sql = "SELECT * FROM " + metadata.getTableName() + " WHERE " + idField.getColumnName() + " = ?";

                try (Connection conn = getConnection();
                        PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            results.add(metadata.mapRow(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e); // must wrap checked exception
            }
        });

        return (results == List.of()) ? Optional.empty() : results.stream().findFirst();
    }

    /**
     * Slaat een nieuwe entiteit op in de database via een INSERT-statement.
     * Het door de database gegenereerde id wordt teruggeschreven op het object.
     *
     * @param entity de op te slaan entiteit
     */
    public void save(@Nonnull T entity) throws SQLException {
        transactionManager.runInTransaction(() -> {
            try {
                List<FieldMetadata> nonIdFields = metadata.getNonIdFields();
                String cols = nonIdFields.stream().map(FieldMetadata::getColumnName).collect(Collectors.joining(", "));
                String placeholders = nonIdFields.stream().map(f -> "?").collect(Collectors.joining(", "));
                String sql = "INSERT INTO " + metadata.getTableName() + " (" + cols.toUpperCase() + ") VALUES (" + placeholders + ")";

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
                }
            } catch (SQLException | IllegalAccessException e) {
                throw new RuntimeException("Fout bij opslaan van " + entity.getClass().getSimpleName(), e);
            }
        });
    }

    /**
     * Werkt een bestaande entiteit bij in de database via een UPDATE-statement.
     * Het primaire sleutelveld wordt gebruikt om de juiste rij te identificeren.
     *
     * @param entity de bij te werken entiteit met het gevulde id-veld
     */
    public void update(@Nonnull T entity) throws SQLException {
        transactionManager.runInTransaction(() -> {
            try {
                FieldMetadata idField = vereistIdVeld();
                List<FieldMetadata> nonIdFields = metadata.getNonIdFields();
                String setClauses = nonIdFields.stream()
                        .map(f -> f.getColumnName() + " = ?")
                        .map(s -> s.equals("version") ? s + " + 1" : s) // increment version
                        .collect(Collectors.joining(", "));
                String sql = "UPDATE " + metadata.getTableName() + " SET " + setClauses
                        + " WHERE " + idField.getColumnName() + " = ? AND version = ?";

                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (int i = 0; i < nonIdFields.size(); i++) {
                        stmt.setObject(i + 1, nonIdFields.get(i).getValue(entity));
                    }
                    stmt.setObject(nonIdFields.size() + 1, idField.getValue(entity));
                    stmt.setObject(nonIdFields.size() + 2, metadata.getNonIdFields().stream().filter(s -> s.getColumnName().equals("version")).findFirst().get().getValue(entity));
                    stmt.executeUpdate();
                }
            } catch (SQLException | IllegalAccessException e) {
                throw new RuntimeException("Fout bij updaten van " + entity.getClass().getSimpleName(), e);
            }
        });
    }

    /**
     * Verwijdert de entiteit met het opgegeven id via een DELETE-statement.
     *
     * @param id de waarde van het primaire sleutelveld
     */
    public void delete(@Nonnull Object id) throws SQLException {
        transactionManager.runInTransaction(() -> {
            try {
                FieldMetadata idField = vereistIdVeld();
                String sql = "DELETE FROM " + metadata.getTableName() + " WHERE " + idField.getColumnName() + " = ?";
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, id);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
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


    public void saveAll(@Nonnull List<T> list) throws SQLException {

        transactionManager.runInTransaction(() -> {
            try {
                List<FieldMetadata> nonIdFields = metadata.getNonIdFields();
                String cols = nonIdFields.stream().map(FieldMetadata::getColumnName).collect(Collectors.joining(", "));
                String placeholders = nonIdFields.stream().map(f -> "?").collect(Collectors.joining(", "));
                String sql = "INSERT INTO " + metadata.getTableName() + " (" + cols + ") VALUES (" + placeholders + ")";

                try (Connection conn = getConnection();
                        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    for(T entity: list)
                    {
                       for (int i = 0; i < nonIdFields.size(); i++) {
                            stmt.setObject(i + 1, nonIdFields.get(i).getValue(entity));
                       }
                        stmt.addBatch();
                        try (ResultSet keys = stmt.getGeneratedKeys()) {
                            if (keys.next() && metadata.getIdField() != null) {
                                metadata.getIdField().setValue(entity, keys.getLong(1));
                            }
                        }
                        stmt.executeBatch();
                }
                }
				catch (IllegalAccessException e)
				{
//                    transactionManager.rollback(conn);
//                    conn.setAutoCommit(true);
//                    conn.close();

					throw new RuntimeException(e);
				}
			} catch (SQLException e) {
                for(T entity: list) {
                    throw new RuntimeException("Fout bij opslaan van " + entity.getClass().getSimpleName(), e);
                }
            }
        });
    }


    public void updateAll(@Nonnull List<T> list) throws SQLException {
        transactionManager.runInTransaction(() -> {
            try {
                FieldMetadata idField = vereistIdVeld();
                List<FieldMetadata> nonIdFields = metadata.getNonIdFields();
                String setClauses = nonIdFields.stream()
                        .map(f -> f.getColumnName() + " = ?")
                        .collect(Collectors.joining(", "));
                String sql = "UPDATE " + metadata.getTableName() + " SET " + setClauses
                        + " WHERE " + idField.getColumnName() + " = ?";

                try (Connection conn = getConnection();
                        PreparedStatement stmt = conn.prepareStatement(sql)) {

                    for(T entity: list)
                    {
                        for (int i = 0; i < nonIdFields.size(); i++) {
                            stmt.setObject(i + 1, nonIdFields.get(i).getValue(entity));
                        }
                        stmt.setObject(nonIdFields.size() + 1, idField.getValue(entity));
                        stmt.addBatch();

                        stmt.executeBatch();
                    }
                }
            } catch (SQLException | IllegalAccessException e) {

                for(T entity: list)
                {
                    throw new RuntimeException("Fout bij updaten van " + entity.getClass().getSimpleName(), e);
                }
            }
        });
    }
}
