package nl.topicus.injection;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
class ConnectionManagerTest {

	@Test
	public void testDataSource()
	{
		DataSource ds = ConnectionManager.getDataSource();
		assertNotNull(ds);
	}
}
