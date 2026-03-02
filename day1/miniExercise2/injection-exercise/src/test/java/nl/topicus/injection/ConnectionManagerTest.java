package nl.topicus.injection;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
class ConnectionManagerTest {

	@Test
	public void testDataSource()
	{
		DataSource ds = ConnectionManager.getDataSource();
		assertNotNull(ds);
		
		try {
			Connection connection = ds.getConnection();
			assertFalse(connection.isClosed());
			
			connection.close();
			assertTrue(connection.isClosed());
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
	}
}
