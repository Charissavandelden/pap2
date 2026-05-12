package nl.topicus.orm;

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
        super(entityClass, TransactionManager.getInstance(datasource));
        this.datasource = datasource;
    }

    /**
     * Geeft de actieve transactieconnectie terug als die aanwezig is,
     * anders een nieuwe losse connectie uit de DataSource (auto-commit gedrag).
     */
    @Override
    public Connection getConnection() throws SQLException {
        Connection actief = transactionManager.connection.get();
        if (actief != null && !actief.isClosed()) {
            return actief;
        }
        return datasource.getConnection();
    }
}
