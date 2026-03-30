package nl.topicus.injection;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Tussenlaag die het beheer van een {@link DataSource} centraliseert zodat
 * concrete DAO-subklassen niet zelf een verbinding hoeven op te zetten.
 *
 * @param <T> het type van de beheerde entiteit
 */
public abstract class AbstractDataSourceRepository<T> extends GenericRepository<T>
{

    private final DataSource datasource;

    protected AbstractDataSourceRepository(@Nonnull Class<T> entityClass, @Nonnull DataSource datasource) {
        super(entityClass, new TransactionManager(datasource));
        this.datasource = datasource;
    }

    /**
     * Haalt een nieuwe verbinding op uit de geconfigureerde {@link DataSource}.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }
}
