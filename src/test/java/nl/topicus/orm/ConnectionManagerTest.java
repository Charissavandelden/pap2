package nl.topicus.orm;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionManagerTest {

	@Test
	void dataSourceLevertEenWerkendeConnectie() throws SQLException {
		DataSource ds = ConnectionManager.getDataSource();

		try (Connection conn = ds.getConnection()) {
			assertFalse(conn.isClosed(), "Connectie moet open zijn direct na openen");
		}
	}

	@Test
	void connectieIsGeslotenNaClose() throws SQLException {
		DataSource ds = ConnectionManager.getDataSource();

		Connection conn = ds.getConnection();
		conn.close();
		assertTrue(conn.isClosed(), "Connectie moet gesloten zijn na close()");
	}
}
