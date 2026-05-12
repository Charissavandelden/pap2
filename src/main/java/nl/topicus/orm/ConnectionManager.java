package nl.topicus.orm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Beheert databaseverbindingen: deelt connecties uit en sluit ze veilig.
 * Levert daarnaast een geconfigureerde standaard-{@link DataSource} via de
 * statische factory-methode (de "configuratieklasse"-rol uit de briefing).
 */
public class ConnectionManager {

    private static final String DB_URL = "jdbc:h2:file:./data/miauw";
    private static final String DB_USER = "miauw";
    private static final String DB_PASSWORD = "";

    private final DataSource dataSource;

    /**
     * Bouwt een ConnectionManager met de standaard H2-DataSource.
     */
    public ConnectionManager() {
        this(getDataSource());
    }

    /**
     * Bouwt een ConnectionManager met een geïnjecteerde {@link DataSource}.
     */
    public ConnectionManager(@Nonnull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Levert een nieuwe verbinding via de geconfigureerde DataSource.
     * Wrapt eventuele SQLException met een duidelijke melding zodat de oorzaak
     * (bijvoorbeeld een onbereikbare DB) direct zichtbaar is.
     */
    @Nonnull
    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new SQLException(
                    "Kan geen databaseverbinding openen via " + DB_URL, e);
        }
    }

    /**
     * Sluit de verbinding null-safe en slikt close-exceptions stil zodat
     * finally-blokken in callers niet sneuvelen.
     */
    public void close(@Nullable Connection conn) {
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException e) {
            System.err.println("Kon connectie niet sluiten: " + e.getMessage());
        }
    }

    /**
     * Maakt een nieuwe standaard-{@link DataSource} aan voor de H2-database.
     * Vervult de rol van de "configuratieklasse" uit de Part-1 briefing.
     */
    @Nonnull
    public static DataSource getDataSource() {
        JdbcDataSource datasource = new JdbcDataSource();
        datasource.setUrl(DB_URL);
        datasource.setUser(DB_USER);
        datasource.setPassword(DB_PASSWORD);
        return datasource;
    }
}
