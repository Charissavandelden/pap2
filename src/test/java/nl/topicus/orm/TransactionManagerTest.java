package nl.topicus.orm;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.topicus.orm.entities.Attack;
import nl.topicus.orm.entities.Pokemon;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionManagerTest {

    private static DataSource dataSource;
    private TransactionManager transactionManager;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:transactiontestdb;DB_CLOSE_DELAY=-1");
        ds.setUser("test");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS pokemon "
                    + "(id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), type VARCHAR(100), version INT DEFAULT 1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS attack "
                    + "(id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), damage INT, version INT DEFAULT 1, pokemon_id BIGINT)");
        }
    }

    @BeforeEach
    void setup() throws SQLException {
        transactionManager = TransactionManager.getInstance(dataSource);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM pokemon");
            stmt.execute("DELETE FROM attack");
        }
    }

    @Test
    void testTweeInsertsInEenTransactieWordenBeideOpgeslagen() throws SQLException {
        transactionManager.runInTransaction(() -> {
            try {
                Connection conn = transactionManager.connection.get();

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO pokemon (name, type) VALUES (?, ?)")) {
                    stmt.setString(1, "Pikachu");
                    stmt.setString(2, "Electric");
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO pokemon (name, type) VALUES (?, ?)")) {
                    stmt.setString(1, "Bulbasaur");
                    stmt.setString(2, "Grass");
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pokemon")) {
            rs.next();
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void testRollbackSlaatDataNietOp() throws SQLException {
        Connection conn = transactionManager.begin();
        conn.setAutoCommit(false);

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO pokemon (name, type) VALUES (?, ?)")) {
            stmt.setString(1, "Charmander");
            stmt.setString(2, "Fire");
            stmt.executeUpdate();
        }

        transactionManager.rollback(conn);

        try (Connection verifyConn = dataSource.getConnection();
             Statement stmt = verifyConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pokemon")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }
    
    @Test
    void testConcurrency() throws SQLException {
        Connection conn = transactionManager.begin();
        conn.setAutoCommit(false);

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO pokemon (name, type) VALUES (?, ?)")) {
            stmt.setString(1, "Charmander");
            stmt.setString(2, "Fire");
            stmt.executeUpdate();
        }

        String updateStmt = "UPDATE FROM pokemon WHERE id = ? SET type = ?";

        conn.close();

        try (Connection verifyConn = dataSource.getConnection();
             Statement stmt = verifyConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pokemon")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void tweeRepoSavesInEenTransactieCommitten() throws SQLException {
        PokemonRepository pokemonRepo = new PokemonRepository(dataSource);
        AttackRepository attackRepo = new AttackRepository(dataSource);

        transactionManager.runInTransaction(() -> {
            try {
                Pokemon p = new Pokemon();
                p.setName("Pikachu");
                p.setType("Electric");
                pokemonRepo.save(p);

                Attack a = new Attack("Thunderbolt", 90);
                attackRepo.save(a);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(1, pokemonRepo.findAll().size(), "Pokémon moet zijn opgeslagen na commit");
        assertEquals(1, attackRepo.findAll().size(), "Attack moet zijn opgeslagen na commit");
    }
}

