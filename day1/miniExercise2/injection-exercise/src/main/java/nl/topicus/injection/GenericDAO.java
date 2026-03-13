package nl.topicus.injection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static nl.topicus.injection.PokemonDao.createResultList;

public abstract class GenericDAO<T>
{

	// return resultset?
	public List<String> findAll(String tablename, Connection connection) throws SQLException
	{
		List<String> results = new ArrayList<>();
		String sql = "SELECT * FROM " + tablename;
		System.out.println("Uitgevoerde query: " + sql);

		try (PreparedStatement stmt = connection.prepareStatement(sql))
		{
			try (ResultSet rs = stmt.executeQuery())
			{
				while (rs.next())
				{
					createResultList(results, rs);
				}
			}
			return results;
		}
	}

	//TODO findById
	//TODO save
	//TODO update
	//TODO delete
}
