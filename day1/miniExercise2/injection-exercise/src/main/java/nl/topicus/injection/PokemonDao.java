package nl.topicus.injection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class PokemonDao
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

        try (PreparedStatement stmt = getConnection().prepareStatement(
                sql))
        {
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    results.add(
                            rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString(
                                    "type"));
                }
            }
            return results;
        }
    }

        // TODO: Voeg hier een findByName(String name) methode toe die een student zoekt op naam.
        //       Gebruik hiervoor direct een PreparedStatement.
        public List<String> findByName(String name) throws SQLException
        {
            List<String> results = new ArrayList<>();
            String sql = "SELECT * FROM pokemon WHERE name LIKE ?";
            System.out.println("Uitgevoerde query: " + sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(
                sql))
        {
            stmt.setString(1, name + "%");
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    results.add(
                            rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString(
                                    "type"));
                }
            }
            return results;
        }
    }
}

