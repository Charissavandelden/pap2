package nl.topicus.orm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager extends ConnectionManager {

	private static final java.util.Map<DataSource, TransactionManager> instances = new java.util.concurrent.ConcurrentHashMap<>();

	@Nullable
	private final DataSource injectedDataSource;

	ThreadLocal<Connection> connection = new ThreadLocal<>();

	private TransactionManager(@Nonnull DataSource dataSource)
	{
		super();
		this.injectedDataSource = dataSource;
	}

	public static TransactionManager getInstance(@Nonnull DataSource dataSource)
	{
		return instances.computeIfAbsent(dataSource, TransactionManager::new);
	}

	public Connection begin() throws SQLException {
		DataSource ds = (injectedDataSource != null) ? injectedDataSource : getDataSource();
		connection.set(ds.getConnection());
		return connection.get();
	}
	
	public void commit(Connection conn) throws SQLException
	{
		conn.commit();
		conn.close();
		connection.remove();
		System.out.println("Transaction: succeeded");
	}

	public void rollback(Connection conn) throws SQLException
	{
		conn.rollback();
		conn.close();
		connection.remove();
		System.out.println("Transaction: failed ):");
	}

	public void runInTransaction(Runnable runnable) throws SQLException
	{
		begin();
		Connection conn = connection.get();
		conn.setAutoCommit(false);
		try {
			runnable.run();
			commit(conn);
		} catch (Exception e) {
			rollback(conn);
			throw new RuntimeException("Transaction failed", e);
		}
	}
}
