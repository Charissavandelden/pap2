package nl.topicus.injection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class PokemonDao extends GenericDAO<Pokemon> implements IPokemonDAO
{
    private final DataSource datasource;

    public PokemonDao(DataSource datasource)
    {
        this.datasource = datasource;
    }

    public Connection getConnection() throws SQLException
    {
        return datasource.getConnection();
    }

    public List<String> findByType(String type) throws SQLException
    {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM pokemon WHERE type LIKE ?";
        System.out.println("Uitgevoerde query: " + sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql))
        {
            stmt.setString(1, "%" + type + "%");
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

    public List<String> findByName(String name) throws SQLException
    {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM pokemon WHERE name LIKE ?";
        System.out.println("Uitgevoerde query: " + sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql))
        {
            stmt.setString(1, "%" + name + "%");
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

    public List<String> findAll() throws SQLException
    {
        return findAll("pokemon", getConnection());
    }

    static void createResultList(List<String> results, ResultSet rs) throws SQLException
    {
        results.add(
                rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString(
                        "type"));
    }
}

