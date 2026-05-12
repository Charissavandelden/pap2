package nl.topicus.orm;

import nl.topicus.orm.mapping.EntityMetadata;
import nl.topicus.orm.mapping.FieldMetadata;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                        ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(metadata.mapRow(rs));
                    }
                } finally {
                    sluitAlsNietTransactioneel(conn);
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

                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            results.add(metadata.mapRow(rs));
                        }
                    }
                } finally {
                    sluitAlsNietTransactioneel(conn);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e); // must wrap checked exception
            }
        });

        return results.isEmpty() ? Optional.empty() : results.stream().findFirst();
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
                LinkedHashSet<FieldMetadata> allFields = metadata.getAllFields();
                String cols = allFields.stream().map(FieldMetadata::getColumnName).collect(Collectors.joining(", "));
                String placeholders = allFields.stream().map(f -> "?").collect(Collectors.joining(", "));
                String sql = "INSERT INTO " + metadata.getTableName() + " (" + cols + ") VALUES (" + placeholders + ")";
                
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                	Iterator<FieldMetadata> fieldIterator = allFields.iterator();
                	int index = 1;
                	while (fieldIterator.hasNext())
                	{
                		stmt.setObject(index, fieldIterator.next().getValue(entity));
                		index++;
                	}
                    stmt.executeUpdate();
                    System.out.println(stmt);
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next() && metadata.getIdField() != null) {
                            metadata.getIdField().setValue(entity, keys.getLong(1));
                        }
                    }
                } finally {
                    sluitAlsNietTransactioneel(conn);
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
                LinkedHashSet<FieldMetadata> allFields = metadata.getAllFields();
                Optional<FieldMetadata> versieVeld = allFields.stream()
                        .filter(f -> f.getColumnName().equals("version"))
                        .findFirst();

                String setClauses = allFields.stream()
                        .map(f -> f.getColumnName().equals("version")
                                ? f.getColumnName() + " = " + f.getColumnName() + " + 1"
                                : f.getColumnName() + " = ?")
                        .collect(Collectors.joining(", "));

                String sql = "UPDATE " + metadata.getTableName() + " SET " + setClauses
                        + " WHERE " + idField.getColumnName() + " = ?"
                        + (versieVeld.isPresent() ? " AND version = ?" : "");

                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    int index = 1;
                    for (FieldMetadata field : allFields) {
                        if (!field.getColumnName().equals("version")) {
                            stmt.setObject(index, field.getValue(entity));
                            index++;
                        }
                    }
                    stmt.setObject(index, idField.getValue(entity));
                    if (versieVeld.isPresent()) {
                        stmt.setObject(index + 1, versieVeld.get().getValue(entity));
                    }
                    int rows = stmt.executeUpdate();
                    if (versieVeld.isPresent() && rows == 0) {
                        throw new OptimisticLockException(
                                entity.getClass().getSimpleName()
                                        + " id=" + idField.getValue(entity)
                                        + " is door een andere transactie gewijzigd");
                    }
                    if (versieVeld.isPresent()) {
                        int huidig = (int) versieVeld.get().getValue(entity);
                        versieVeld.get().setValue(entity, huidig + 1);
                    }
                } finally {
                    sluitAlsNietTransactioneel(conn);
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
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, id);
                    stmt.executeUpdate();
                } finally {
                    sluitAlsNietTransactioneel(conn);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Sluit de verbinding alleen als het geen actieve transactieverbinding is.
     * Voorkomt dat try-with-resources de ThreadLocal-verbinding van de TransactionManager sluit.
     */
    private void sluitAlsNietTransactioneel(Connection conn) throws SQLException {
        if (conn != transactionManager.connection.get()) {
            conn.close();
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


    public void saveAll(@Nonnull List<T> list) throws SQLException {
        transactionManager.runInTransaction(() -> {
            try {
                LinkedHashSet<FieldMetadata> allFields = metadata.getAllFields();
                String cols = allFields.stream().map(FieldMetadata::getColumnName).collect(Collectors.joining(", "));
                String placeholders = allFields.stream().map(f -> "?").collect(Collectors.joining(", "));
                String sql = "INSERT INTO " + metadata.getTableName() + " (" + cols + ") VALUES (" + placeholders + ")";

                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    for (T entity : list) {
                        Iterator<FieldMetadata> fieldIterator = allFields.iterator();
                        int index = 1;
                        while (fieldIterator.hasNext()) {
                            stmt.setObject(index, fieldIterator.next().getValue(entity));
                            index++;
                        }
                        stmt.addBatch();
                    }
                    stmt.executeBatch();

                    // Keys ophalen na executeBatch, niet ervoor
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        for (T entity : list) {
                            if (keys.next() && metadata.getIdField() != null) {
                                metadata.getIdField().setValue(entity, keys.getLong(1));
                            }
                        }
                    }
                }
				catch (IllegalAccessException e)
				{
					throw new RuntimeException(e);
				} finally {
                    sluitAlsNietTransactioneel(conn);
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
                LinkedHashSet<FieldMetadata> allFields = metadata.getAllFields();
                String setClauses = allFields.stream()
                        .map(f -> f.getColumnName() + " = ?")
                        .collect(Collectors.joining(", "));
                String sql = "UPDATE " + metadata.getTableName() + " SET " + setClauses
                        + " WHERE " + idField.getColumnName() + " = ?";

                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (T entity : list) {
                        Iterator<FieldMetadata> fieldIterator = allFields.iterator();
                        int index = 1;
                        while (fieldIterator.hasNext()) {
                            stmt.setObject(index, fieldIterator.next().getValue(entity));
                            index++;
                        }
                        stmt.setObject(allFields.size() + 1, idField.getValue(entity));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                } finally {
                    sluitAlsNietTransactioneel(conn);
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
