package nl.topicus.orm;

import nl.topicus.orm.entities.FirePokemon;
import nl.topicus.orm.entities.Pokemon;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Dag3Opdracht3Test
{

	private static DataSource dataSource;
	private PokemonRepository pokemonDao;

	@BeforeAll
	static void setupDatabase() throws SQLException
	{
		JdbcDataSource ds = new JdbcDataSource();
		ds.setUrl("jdbc:h2:mem:dag3db;DB_CLOSE_DELAY=-1");
		ds.setUser("test");
		ds.setPassword("");
		dataSource = ds;

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS pokemon");
			stmt.execute("CREATE TABLE pokemon "
					+ "(id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), "
					+ "type VARCHAR(100), version INT DEFAULT 1)");
		}
	}

	@BeforeEach
	void setup() throws SQLException {
		pokemonDao = new PokemonRepository(dataSource);

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("DELETE FROM pokemon");
		}
	}

	// Opdracht Part 3 vraag 1: wat gaat er fout als je probeert te casten?
	@Test
	void castPokemonToFirePokemonTest() throws SQLException {
		Pokemon p1 = new Pokemon();
		p1.setName("Bulbasaur");
		p1.setType("Grass");

		Pokemon p2 = new Pokemon();
		p2.setName("Squirtle");
		p2.setType("Water");

		pokemonDao.saveAll(List.of(p1, p2));

		List<Pokemon> pokemons = pokemonDao.findAll();
		assertEquals(2, pokemons.size());

		for (Pokemon p : pokemons) {
			// Vraag 1: dit gooit een ClassCastException omdat mapRow() altijd
			// een Pokemon aanmaakt, nooit een FirePokemon
			assertThrows(ClassCastException.class, () -> {
				FirePokemon vuur = (FirePokemon) p;
			});

			// Vraag 3: instanceof is veilig — geeft false zonder exception
			assertFalse(p instanceof FirePokemon);
		}
	}
}
