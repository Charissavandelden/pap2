package nl.topicus.injection;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

/**
 * Beheert de configuratie van de databaseverbinding en levert een geconfigureerde
 * {@link DataSource} aan de rest van de applicatie.
 */
public class ConnectionManager {

    private static final String DB_URL = "jdbc:h2:file:./data/miauw";
    private static final String DB_USER = "miauw";
    private static final String DB_PASSWORD = "";

    /**
     * Maakt een nieuwe {@link DataSource} aan die verbinding maakt met de H2-database.
     *
     * @return een geconfigureerde {@link DataSource}
     */
    public static DataSource getDataSource() {
        JdbcDataSource datasource = new JdbcDataSource();
        datasource.setUrl(DB_URL);
        datasource.setUser(DB_USER);
        datasource.setPassword(DB_PASSWORD);
        return datasource;
    }
}
