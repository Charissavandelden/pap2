package nl.topicus.injection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GenericDaoTest {

    private static DataSource dataSource;
    private PokemonRepository pokemonDao;
    private AttackRepository attackDao;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("test");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS pokemon (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), type VARCHAR(100))");
            stmt.execute("CREATE TABLE IF NOT EXISTS attack (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), damage INT)");
        }
    }

    @BeforeEach
    void setup() throws SQLException {
        pokemonDao = new PokemonRepository(dataSource);
        attackDao = new AttackRepository(dataSource);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM pokemon");
            stmt.execute("DELETE FROM attack");
        }
    }

    @Test
    void testPokemonOpslaanEnOphalenViaId() throws SQLException {
        Pokemon pokemon = new Pokemon();
        pokemon.setName("Pikachu");
        pokemon.setType("Electric");
        pokemonDao.save(pokemon);

        assertTrue(pokemon.getId() > 0, "Id moet na opslaan ingevuld zijn");

        Optional<Pokemon> gevonden = pokemonDao.findById(pokemon.getId());
        assertTrue(gevonden.isPresent());
        assertEquals("Pikachu", gevonden.get().getName());
        assertEquals("Electric", gevonden.get().getType());
    }

    @Test
    void testPokemonFindAll() throws SQLException {
        Pokemon p1 = new Pokemon();
        p1.setName("Bulbasaur");
        p1.setType("Grass");

        Pokemon p2 = new Pokemon();
        p2.setName("Squirtle");
        p2.setType("Water");

        pokemonDao.save(p1);
        pokemonDao.save(p2);

        List<Pokemon> pokemons = pokemonDao.findAll();
        assertEquals(2, pokemons.size());
    }

    @Test
    void testPokemonUpdaten() throws SQLException {
        Pokemon pokemon = new Pokemon();
        pokemon.setName("Eevee");
        pokemon.setType("Normal");
        pokemonDao.save(pokemon);

        pokemon.setName("Flareon");
        pokemon.setType("Fire");
        pokemonDao.update(pokemon);

        Optional<Pokemon> bijgewerkt = pokemonDao.findById(pokemon.getId());
        assertTrue(bijgewerkt.isPresent());
        assertEquals("Flareon", bijgewerkt.get().getName());
        assertEquals("Fire", bijgewerkt.get().getType());
    }

    @Test
    void testPokemonVerwijderen() throws SQLException {
        Pokemon pokemon = new Pokemon();
        pokemon.setName("MissingNo");
        pokemon.setType("???");
        pokemonDao.save(pokemon);

        pokemonDao.delete(pokemon.getId());

        assertFalse(pokemonDao.findById(pokemon.getId()).isPresent());
    }

    @Test
    void testFindByIdNietGevonden() throws SQLException {
        Optional<Pokemon> resultaat = pokemonDao.findById(9999L);
        assertFalse(resultaat.isPresent());
    }

    @Test
    void testAttackWerktZonderAanpassingenAanRepository() throws SQLException {
        Attack attack = new Attack("Thunderbolt", 90);
        attackDao.save(attack);

        assertTrue(attack.getId() > 0);

        Optional<Attack> gevonden = attackDao.findById(attack.getId());
        assertTrue(gevonden.isPresent());
        assertEquals("Thunderbolt", gevonden.get().getName());
        assertEquals(90, gevonden.get().getDamage());
    }

    @Test
    void testAttackCrudVolledig() throws SQLException {
        Attack attack = new Attack("Tackle", 40);
        attackDao.save(attack);

        attack.setName("Body Slam");
        attack.setDamage(85);
        attackDao.update(attack);

        Optional<Attack> bijgewerkt = attackDao.findById(attack.getId());
        assertTrue(bijgewerkt.isPresent());
        assertEquals("Body Slam", bijgewerkt.get().getName());
        assertEquals(85, bijgewerkt.get().getDamage());

        attackDao.delete(attack.getId());
        assertFalse(attackDao.findById(attack.getId()).isPresent());
    }
}
