package nl.topicus.injection;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager extends ConnectionManager {

	//TODO: Deze class moet een SINGLETON worden, zodat we deze overal kunnen gebruiken.
	
	ThreadLocal<Connection> connection = new ThreadLocal<>();
	
	public Connection begin() throws SQLException {
		connection.set(getDataSource().getConnection());
		connection.get().setAutoCommit(false);
		return connection.get();
	}
	
	public void commit() {
		//TODO: Logica implementeren voor commit, terugzetten van autocommit en het closen van de connection
	}
	
	public void rollback() {
		//TODO: Logica implementeren voor rollback, terugzetten van autocommit en het closen van de connection
	}
	
	public void runInTransaction(Runnable runnable) {
		//TODO: Met deze methode is bedoeld als wrapper om een stuk sql bewerking, dat in zijn geheel moet slagen of falen
		try {
			begin();
			runnable.run();
			commit();
		} catch (SQLException e) {
			rollback();
			e.printStackTrace();
		}
	}
}
