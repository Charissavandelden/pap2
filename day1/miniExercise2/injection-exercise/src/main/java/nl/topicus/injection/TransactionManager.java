package nl.topicus.injection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager extends ConnectionManager {

	//TODO: Deze class moet een SINGLETON worden, zodat we deze overal kunnen gebruiken.

	@Nullable
	private final DataSource injectedDataSource;

	ThreadLocal<Connection> connection = new ThreadLocal<>();

	public TransactionManager()
	{
		super();
		this.injectedDataSource = null;
	}

	public TransactionManager(@Nonnull DataSource dataSource)
	{
		super();
		this.injectedDataSource = dataSource;
	}

	public Connection begin() throws SQLException {
		DataSource ds = (injectedDataSource != null) ? injectedDataSource : getDataSource();
		connection.set(ds.getConnection());
		return connection.get();
	}
	
	public void commit(Connection conn) throws SQLException
	{
		//TODO: Logica implementeren voor commit, terugzetten van autocommit en het closen van de connection

		conn.commit();
		System.out.println("Transaction: succeeded");
	}

	public void rollback(Connection conn) throws SQLException
	{
		//TODO: Logica implementeren voor rollback, terugzetten van autocommit en het closen van de connection

		conn.rollback();
		System.out.println("Transaction: failed ):");
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
