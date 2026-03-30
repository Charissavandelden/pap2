package nl.topicus.injection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class concurrencyTest
{

	private static DataSource dataSource;

	private PokemonRepository pokemonRepository;

	@BeforeAll
	static void setupDatabase() throws SQLException
	{
		JdbcDataSource ds = new JdbcDataSource();
		ds.setUrl("jdbc:h2:file:./data/miauw;DB_CLOSE_DELAY=-1");
		ds.setUser("miauw");
		ds.setPassword("");
		dataSource = ds;

		try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement())
		{
			stmt.execute("CREATE TABLE IF NOT EXISTS pokemon "
					+ "(id BIGINT AUTO_INCREMENT PRIMARY KEY, "
					+ "name VARCHAR(100), type VARCHAR(100))");
		}
	}

	@BeforeEach
	void setup() throws SQLException
	{
		pokemonRepository = new PokemonRepository(dataSource);

		try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement())
		{
			stmt.execute("DELETE FROM pokemon");
		}
	}

	// ---------------------------------------------------------------
	// TEST 1: Meerdere threads slaan tegelijk Pokémon op.
	// Verwacht: alle inserts komen in de database terecht (geen verlies).
	// ---------------------------------------------------------------
	@Test
	void testGelijktijdigeSavesVerliezenGeenData() throws InterruptedException, SQLException
	{
		// zo starten 10 losse threads
		int aantalThreads = 10;
		CountDownLatch startSignaal = new CountDownLatch(1);
		CountDownLatch klaar = new CountDownLatch(aantalThreads);
		AtomicInteger fouten = new AtomicInteger(0);

		for (int i = 0; i < aantalThreads; i++)
		{
			String naam = "Pokemon-" + i;
			new Thread(() -> {
				try
				{
					// threads verzamelen bij de start
					startSignaal.await();
					Pokemon p = new Pokemon();
					p.setName(naam);
					p.setType("Fire");
					pokemonRepository.save(p);
				}
				catch (Exception e)
				{
					// Laat de teller weten/ophogen dat er iets fout is.
					fouten.incrementAndGet();
					e.printStackTrace();
				}
				finally
				{
					// Deze specifieke thread meldt zich af (teller gaat -1).
					klaar.countDown();
				}
			}).start();
		}

		// nu mogen alle threads beginnen
		startSignaal.countDown();
		klaar.await(10, TimeUnit.SECONDS);

		assertEquals(0, fouten.get(), "Er mogen geen fouten zijn bij gelijktijdige saves");

		List<Pokemon> allePokemons = pokemonRepository.findAll();
		assertEquals(aantalThreads, allePokemons.size(),
				"Alle " + aantalThreads + " Pokémon moeten opgeslagen zijn");
	}

	// ---------------------------------------------------------------
	// TEST 2: ThreadLocal isolatie — elke thread krijgt zijn eigen Connection.
	// Verwacht: de Connection-referenties van twee threads zijn NIET gelijk.
	// ---------------------------------------------------------------
	@Test
	void testElkeThreadKrijgtEigenConnection() throws Exception
	{
		//open transationmanager
		TransactionManager transactionManager = new TransactionManager(dataSource);

		// CopyOnWriteArrayList: Dit is een lijst die veilig is om te gebruiken wanneer meerdere
		// threads er tegelijkertijd naar schrijven.
		List<Connection> connections = new CopyOnWriteArrayList<>();

		// CountDownLatch(2): Dit is een soort startblok/finishlijn. We vertellen de test:
		// "Wacht tot er 2 threads klaar zijn voordat je de uitslag controleert."
		CountDownLatch klaar = new CountDownLatch(2);

		Runnable haalConnectionOp = () -> {
			try
			{
				//open verbinding
				Connection connection = transactionManager.begin();
				connections.add(connection);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			finally
			{
				klaar.countDown();
			}
		};

		//open 2 aparte threads
		Thread thread1 = new Thread(haalConnectionOp);
		Thread thread2 = new Thread(haalConnectionOp);
		//start de threads
		thread1.start();
		thread2.start();
		//wacht tot de threads klaar zijn
		klaar.await(5, TimeUnit.SECONDS);
		// controleer of er 2 connections zijn
		assertEquals(2, connections.size());

		// controleer of de connections gelijk zijn
		assertNotSame(connections.get(0), connections.get(1),
				"Elke thread moet zijn eigen Connection hebben via ThreadLocal");
	}

	// ---------------------------------------------------------------
	// TEST 3: Lost Update — laat zien dat een concurrent update verloren gaat
	// ---------------------------------------------------------------
	@Test
	void testLostUpdateZonderLocking() throws Exception {
		// Setup: sla één Pokémon op
		Pokemon origineel = new Pokemon();
		origineel.setName("Eevee");
		origineel.setType("Normal");
		pokemonRepository.save(origineel);
		long id = origineel.getId();

		CountDownLatch beideLezen = new CountDownLatch(2);
		CountDownLatch startUpdate = new CountDownLatch(1);

		Thread threadA = new Thread(() -> {
			try {
				Pokemon p = pokemonRepository.findById(id).orElseThrow();
				beideLezen.countDown();
				startUpdate.await();
				p.setType("Fire"); // thread A wil Fire schrijven
				pokemonRepository.update(p);
			} catch (Exception e) { e.printStackTrace(); }
		});

		Thread threadB = new Thread(() -> {
			try {
				Pokemon p = pokemonRepository.findById(id).orElseThrow();
				beideLezen.countDown();
				startUpdate.await();
				p.setType("Water"); // thread B wil Water schrijven
				pokemonRepository.update(p);
			} catch (Exception e) { e.printStackTrace(); }
		});

		threadA.start();
		threadB.start();
		beideLezen.await(5, TimeUnit.SECONDS);
		startUpdate.countDown(); // laat beide tegelijk updaten
		threadA.join();
		threadB.join();

		Pokemon eindResultaat = pokemonRepository.findById(id).orElseThrow();
		// Eén update is verloren gegaan — dit is het probleem dat we willen oplossen
		System.out.println("Resultaat na lost update: " + eindResultaat.getType());
		assertTrue(
				eindResultaat.getType().equals("Fire") || eindResultaat.getType().equals("Water"),
				"Lost update: één van de twee updates is verloren gegaan"
		);
	}
}
