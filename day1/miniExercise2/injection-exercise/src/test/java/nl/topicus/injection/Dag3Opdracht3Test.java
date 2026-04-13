package nl.topicus.injection;

import nl.topicus.injection.entities.Pokemon;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Dag3Opdracht3Test
{

	private static DataSource dataSource;
	private PokemonRepository pokemonDao;
	private AttackRepository attackDao;
	private TransactionManager transactionManager;

	@BeforeAll
	static void setupDatabase() throws SQLException
	{
		JdbcDataSource ds = new JdbcDataSource();
		ds.setUrl("jdbc:h2:mem:transactiontestdb;DB_CLOSE_DELAY=-1");
		ds.setUser("test");
		ds.setPassword("");
		dataSource = ds;

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE IF NOT EXISTS pokemon "
					+ "(id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), type VARCHAR(100))");
		}
	}

	@BeforeEach
	void setup() throws SQLException {
		transactionManager = new TransactionManager(dataSource);

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("DELETE FROM pokemon");
		}
	}

	@Test
	void castPokemonToFirePokemonTest() throws SQLException {
		Pokemon p1 = new Pokemon();
		p1.setName("Bulbasaur");
		p1.setType("Grass");

		Pokemon p2 = new Pokemon();
		p2.setName("Squirtle");
		p2.setType("Water");

		pokemonDao.saveAll(List.of(p1,p2));

		List<Pokemon> pokemons = pokemonDao.findAll();
		assertEquals(2, pokemons.size());

		for (Pokemon p : pokemons) {
			//dit mag niet
//			System.out.println((FirePokemon)p.getName());

			// antwoord op vraag 3:
			// dit zou wel met if instanceof FirePokemon werken ook
			// als Pokemon null is want instance of is null-safe,
			// maar dan wordt deze nooit true
		}
	}

}
