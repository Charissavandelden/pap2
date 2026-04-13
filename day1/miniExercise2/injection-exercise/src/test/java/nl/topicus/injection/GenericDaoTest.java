package nl.topicus.injection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import nl.topicus.injection.entities.Attack;
import nl.topicus.injection.entities.Pokemon;

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

    @Test
    void testPokemonBatchFindAll() throws SQLException {
        Pokemon p1 = new Pokemon();
        p1.setName("Bulbasaur");
        p1.setType("Grass");

        Pokemon p2 = new Pokemon();
        p2.setName("Squirtle");
        p2.setType("Water");

        pokemonDao.saveAll(List.of(p1,p2));

        List<Pokemon> pokemons = pokemonDao.findAll();
        assertEquals(2, pokemons.size());
    }

    @Test
    void testPokemonBatchSuperSnel() throws SQLException {

        //Test wordt 10 keer uitgevoerd.
        for (int i = 0; i < 10; i++)
        {
            Pokemon p1 = new Pokemon();
            p1.setName("Bulbasaur");
            p1.setType("Grass");

            Pokemon p2 = new Pokemon();
            p2.setName("Squirtle");
            p2.setType("Water");

            Pokemon p3 = new Pokemon();
            Pokemon p4 = new Pokemon();
            Pokemon p5 = new Pokemon();
            Pokemon p6 = new Pokemon();
            Pokemon p7 = new Pokemon();
            Pokemon p8 = new Pokemon();
            Pokemon p9 = new Pokemon();
            Pokemon p10 = new Pokemon();
            Pokemon p11 = new Pokemon();
            Pokemon p12 = new Pokemon();
            Pokemon p13 = new Pokemon();
            Pokemon p14 = new Pokemon();
            Pokemon p15 = new Pokemon();
            Pokemon p16 = new Pokemon();
            Pokemon p17 = new Pokemon();
            Pokemon p18 = new Pokemon();
            Pokemon p19 = new Pokemon();

            long start = System.currentTimeMillis();
            pokemonDao.saveAll(List.of(p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19));

            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            System.out.println(timeElapsed);

            long start2 = System.currentTimeMillis();
            pokemonDao.save(p1);
            pokemonDao.save(p2);
            pokemonDao.save(p3);
            pokemonDao.save(p4);
            pokemonDao.save(p5);
            pokemonDao.save(p6);
            pokemonDao.save(p7);
            pokemonDao.save(p8);
            pokemonDao.save(p9);
            pokemonDao.save(p10);
            pokemonDao.save(p11);
            pokemonDao.save(p12);
            pokemonDao.save(p13);
            pokemonDao.save(p14);
            pokemonDao.save(p15);
            pokemonDao.save(p16);
            pokemonDao.save(p17);
            pokemonDao.save(p18);
            pokemonDao.save(p19);

            pokemonDao.save(p1);
            pokemonDao.save(p2);
            pokemonDao.save(p3);
            pokemonDao.save(p4);
            pokemonDao.save(p5);
            pokemonDao.save(p6);
            pokemonDao.save(p7);
            pokemonDao.save(p8);
            pokemonDao.save(p9);
            pokemonDao.save(p10);
            pokemonDao.save(p11);
            pokemonDao.save(p12);
            pokemonDao.save(p13);
            pokemonDao.save(p14);
            pokemonDao.save(p15);
            pokemonDao.save(p16);
            pokemonDao.save(p17);
            pokemonDao.save(p18);
            pokemonDao.save(p19);

            pokemonDao.save(p1);
            pokemonDao.save(p2);
            pokemonDao.save(p3);
            pokemonDao.save(p4);
            pokemonDao.save(p5);
            pokemonDao.save(p6);
            pokemonDao.save(p7);
            pokemonDao.save(p8);
            pokemonDao.save(p9);
            pokemonDao.save(p10);
            pokemonDao.save(p11);
            pokemonDao.save(p12);
            pokemonDao.save(p13);
            pokemonDao.save(p14);
            pokemonDao.save(p15);
            pokemonDao.save(p16);
            pokemonDao.save(p17);
            pokemonDao.save(p18);
            pokemonDao.save(p19);

            pokemonDao.save(p1);
            pokemonDao.save(p2);
            pokemonDao.save(p3);
            pokemonDao.save(p4);
            pokemonDao.save(p5);
            pokemonDao.save(p6);
            pokemonDao.save(p7);
            pokemonDao.save(p8);
            pokemonDao.save(p9);
            pokemonDao.save(p10);
            pokemonDao.save(p11);
            pokemonDao.save(p12);
            pokemonDao.save(p13);
            pokemonDao.save(p14);
            pokemonDao.save(p15);
            pokemonDao.save(p16);
            pokemonDao.save(p17);
            pokemonDao.save(p18);
            pokemonDao.save(p19);

            long finish2 = System.currentTimeMillis();
            long timeElapsed2 = finish2 - start2;

            assertTrue(timeElapsed2 > timeElapsed);
        }
    }

    @Test
    void testPokemonUpdateALL() throws SQLException {
        Pokemon p1 = new Pokemon();
        p1.setName("Eevee");
        p1.setType("Normal");
        pokemonDao.save(p1);

        Pokemon p2 = new Pokemon();
        p2.setName("Eevee");
        p2.setType("Normal");
        pokemonDao.save(p2);

        p1.setName("Magikarp");
        p1.setType("Water");

        p2.setName("Flareon");
        p2.setType("Fire");
        pokemonDao.updateAll(List.of(p1, p2));

        Optional<Pokemon> bijgewerkt1 = pokemonDao.findById(p2.getId());
        assertTrue(bijgewerkt1.isPresent());
        assertEquals("Flareon", bijgewerkt1.get().getName());
        assertEquals("Fire", bijgewerkt1.get().getType());

        Optional<Pokemon> bijgewerkt2 = pokemonDao.findById(p1.getId());
        assertTrue(bijgewerkt2.isPresent());
        assertEquals("Magikarp", bijgewerkt2.get().getName());
        assertEquals("Water", bijgewerkt2.get().getType());
    }
}
