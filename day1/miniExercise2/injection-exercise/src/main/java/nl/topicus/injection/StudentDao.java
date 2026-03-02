package nl.topicus.injection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class StudentDao
{
    private final DataSource datasource;

    public StudentDao(DataSource datasource)
    {
        this.datasource = datasource;
    }
    
    public Connection getConnection() throws SQLException 
    {
    	return datasource.getConnection();
    }
    

    /**
     * Zoekt studenten op basis van e-mailadres.
     *
     * LET OP: deze methode is KWETSBAAR voor SQL injection! De input wordt direct in de SQL-query
     * geplakt via string concatenation.
     *
     * TODO: Vervang Statement door PreparedStatement om SQL injection te voorkomen.
     */
    //    public List<String> findByEmail(String email) throws SQLException
    //    {
    //        List<String> results = new ArrayList<>();
    //        String sql = "SELECT * FROM students WHERE email = '" + email + "'";
    //
    //        System.out.println("Uitgevoerde query: " + sql);
    //
    //        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql))
    //        {
    //            while (rs.next())
    //            {
    //                results.add(rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString(
    //                        "email") + " | " + rs.getInt("age"));
    //            }
    //        }
    //        return results;
    //    }
    public List<String> findByEmail(String email) throws SQLException
    {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE email = '" + email + "'";

        System.out.println("Uitgevoerde query: " + sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT * FROM students WHERE email = ?"))
        {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    results.add(
                            rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString(
                                    "email") + " | " + rs.getInt("age"));
                }
            }
            return results;
        }
    }

        // TODO: Voeg hier een findByName(String name) methode toe die een student zoekt op naam.
        //       Gebruik hiervoor direct een PreparedStatement.
        public List<String> findByName(String name) throws SQLException {
        List<String> results = new ArrayList<>();

        try (PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT * FROM students WHERE name LIKE ?"))
        {
            stmt.setString(1, name + "%");
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    results.add(
                            rs.getLong("id") + " | " + rs.getString("name") + " | " + rs.getString(
                                    "email") + " | " + rs.getInt("age"));
                }
            }
            return results;
        }
    }
}

