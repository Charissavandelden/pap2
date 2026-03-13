package nl.topicus.injection;

import java.sql.SQLException;
import java.util.List;

public interface IPokemonDAO
{
	List<String> findByType(String type) throws SQLException;
	List<String> findByName(String name) throws SQLException;

}
