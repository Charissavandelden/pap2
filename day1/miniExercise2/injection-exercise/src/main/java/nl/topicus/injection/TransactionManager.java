package nl.topicus.injection;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager extends ConnectionManager {

	//TODO: Deze class moet een SINGLETON worden, zodat we deze overal kunnen gebruiken.
	
	ThreadLocal<Connection> connection = new ThreadLocal<>();

	public TransactionManager()
	{
		super();
	}

	public Connection begin() throws SQLException {
		connection.set(getDataSource().getConnection());
		return connection.get();
	}
	
	public void commit(Connection conn) throws SQLException
	{
		//TODO: Logica implementeren voor commit, terugzetten van autocommit en het closen van de connection

		conn.commit();
	}

	public void rollback(Connection conn) throws SQLException
	{
		//TODO: Logica implementeren voor rollback, terugzetten van autocommit en het closen van de connection

		conn.rollback();
	}
	
	public void runInTransaction(Runnable runnable) throws SQLException
	{
		begin();

		connection.get().setAutoCommit(false);
		Connection conn = connection.get();
		//TODO: Met deze methode is bedoeld als wrapper om een stuk sql bewerking, dat in zijn geheel moet slagen of falen
		try {
			runnable.run();

			commit(conn);
		} catch (SQLException e) {
			rollback(connection.get());

			throw new SQLException("Transaction failed", e);
//			e.printStackTrace();
		} finally {
			conn.setAutoCommit(true);
		}
	}
}
