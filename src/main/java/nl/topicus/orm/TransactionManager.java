package nl.topicus.orm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {

	private static final java.util.Map<DataSource, TransactionManager> instances = new java.util.concurrent.ConcurrentHashMap<>();

	@Nullable
	private final DataSource injectedDataSource;

	ThreadLocal<Connection> connection = new ThreadLocal<>();

	private TransactionManager(@Nonnull DataSource dataSource)
	{
		this.injectedDataSource = dataSource;
	}

	public static TransactionManager getInstance(@Nonnull DataSource dataSource)
	{
		return instances.computeIfAbsent(dataSource, TransactionManager::new);
	}

	public Connection begin() throws SQLException {
		Connection existing = connection.get();
		if (existing != null && !existing.isClosed()) {
			return existing;
		}
		DataSource ds = (injectedDataSource != null) ? injectedDataSource : ConnectionManager.getDataSource();
		Connection fresh = ds.getConnection();
		try {
			fresh.setAutoCommit(false);
		} catch (SQLException e) {
			try { fresh.close(); } catch (SQLException ignore) {}
			throw e;
		}
		connection.set(fresh);
		return fresh;
	}

	public void commit(Connection conn) throws SQLException
	{
		try {
			conn.commit();
		} finally {
			try { conn.close(); } catch (SQLException ignore) {}
			connection.remove();
		}
		System.out.println("Transaction: succeeded");
	}

	public void rollback(Connection conn) throws SQLException
	{
		try {
			conn.rollback();
		} finally {
			try { conn.close(); } catch (SQLException ignore) {}
			connection.remove();
		}
		System.out.println("Transaction: failed ):");
	}

	public void runInTransaction(Runnable runnable)
	{
		Connection beforeBegin = connection.get();
		Connection conn;
		try {
			conn = begin();
		} catch (SQLException e) {
			throw new RuntimeException("Kan transactie niet starten", e);
		}
		boolean nieuweTransactie = (conn != beforeBegin);

		try {
			runnable.run();
			if (nieuweTransactie) {
				try {
					commit(conn);
				} catch (SQLException e) {
					throw new RuntimeException("Commit faalde", e);
				}
			}
		} catch (RuntimeException e) {
			if (nieuweTransactie) {
				try { rollback(conn); } catch (SQLException ignore) {}
			}
			throw e;
		}
	}
}
