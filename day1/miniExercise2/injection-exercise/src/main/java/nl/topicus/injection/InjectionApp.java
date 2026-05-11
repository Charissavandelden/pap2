package nl.topicus.injection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import nl.topicus.injection.entities.Attack;
import nl.topicus.injection.entities.FirePokemon;
import nl.topicus.injection.entities.GymLeader;
import nl.topicus.injection.entities.Person;
import nl.topicus.injection.entities.Pokemon;
import nl.topicus.injection.entities.Trainer;
import nl.topicus.injection.mapping.EntityMetadata;
import nl.topicus.injection.mapping.FieldMetadata;
import nl.topicus.injection.mapping.MetadataHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

public class InjectionApp
{
	private static final HashSet<EntityMetadata<?>> rootEntities = new HashSet<>();
	public static final DataSource datasource = TransactionManager.getDataSource();
	
    public static void main(String[] args) throws IOException
    {
        PokemonRepository dao = new PokemonRepository(datasource);
        
        registerEntity(Attack.class);
        registerEntity(Pokemon.class);
        registerEntity(Person.class);
        registerEntity(Trainer.class);
        registerEntity(GymLeader.class);

        try (Connection conn = datasource.getConnection())
        {
            setupDatabase(conn);
        }
        catch (SQLException e)
        {
            System.out.println("Alles kaput: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve de frontend pagina
        ServeFrontend(server);

        // API endpoint voor zoeken op Pokémon type
        serveSearchByTypeEndpoint(server, dao);

        // API endpoint voor zoeken op Pokémon naam
        serveSearchByNaamEndpoint(server, dao);

        // API endpoint voor ophalen alle Pokemon
        serveFindAllEndpoint(server, dao);

        server.setExecutor(null);
        server.start();
        System.out.println("Server gestart op http://localhost:8080");
        System.out.println("Druk op Ctrl+C om te stoppen.");
    }

    private static void serveSearchByNaamEndpoint(HttpServer server, PokemonRepository dao)
    {
        server.createContext("/api/search-by-name", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String name = "";
            if (query != null && query.startsWith("name="))
            {
                name = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
            }

            try
            {
                List<String> results = naarStrings(dao.findByName(name));
                sendHTTPRequest(exchange, results);
            }
            catch (SQLException e)
            {
                printError(exchange, e);
            }
        });
    }

    /**
     * Registreert het API-endpoint dat alle Pokémon teruggeeft.
     */
    private static void serveFindAllEndpoint(HttpServer server, PokemonRepository dao)
    {
        server.createContext("/api/find-all", exchange -> {
            try
            {
                List<String> results = naarStrings(dao.findAll());
                sendHTTPRequest(exchange, results);
            }
            catch (SQLException e)
            {
                printError(exchange, e);
            }
        });
    }

    private static void sendHTTPRequest(HttpExchange exchange, List<String> results)
            throws IOException
    {
        String json = buildJson(results);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody())
        {
            os.write(response);
        }
    }

    /**
     * Registreert het API-endpoint voor het zoeken van Pokémon op type.
     */
    private static void serveSearchByTypeEndpoint(HttpServer server, PokemonRepository dao)
	{
        server.createContext("/api/search-by-type", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String type = "";
            if (query != null && query.startsWith("type="))
            {
                type = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
            }

            try
            {
                List<String> results = naarStrings(dao.findByType(type));
                sendHTTPRequest(exchange, results);
            }
            catch (SQLException e)
            {
                printError(exchange, e);
            }
        });
    }

    /**
     * Registreert het endpoint dat de HTML-frontend serveert.
     */
    private static void ServeFrontend(HttpServer server)
    {
        server.createContext("/", exchange -> {
            try (InputStream is = InjectionApp.class.getResourceAsStream("/frontend/index.html"))
            {
                byte[] html = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.length);
                try (OutputStream os = exchange.getResponseBody())
                {
                    os.write(html);
                }
            }
        });
    }

    private static void printError(HttpExchange exchange, SQLException e) throws IOException
    {
        String error = "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        byte[] response = error.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(500, response.length);
        try (OutputStream os = exchange.getResponseBody())
        {
            os.write(response);
        }
    }

    /**
     * Maakt de verschillende tabellen aan en vult deze met startdata.
     */
    private static void setupDatabase(Connection conn) throws SQLException
    {
    	System.out.println("====== DROPPING & CREATING TABLES ======");
    	rootEntities.forEach(rootEntity -> dropAndCreateTable(rootEntity, conn));
    	
    	System.out.println("\n====== INSERTING ENTITIES ======");
    	PokemonRepository pokemonDAO = new PokemonRepository(datasource);
        try (Statement stmt = conn.createStatement())
        {
        	ArrayList<Pokemon> starterPokemon = new ArrayList<Pokemon>();
        	starterPokemon.add(new Pokemon("Bulbasaur", "Grass"));
        	starterPokemon.add(new Pokemon("Squirtle", "Water"));
        	starterPokemon.add(new Pokemon("Pikachu", "Electric"));
        	
        	System.out.println("Inserting " + starterPokemon.size() + " pokemon");
        	for (Pokemon pokemon : starterPokemon) {
				pokemonDAO.save(pokemon);
			}
//        	pokemonDAO.saveAll(starterPokemon);
        	
//            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Bulbasaur', 'Grass')");
//            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Squirtle', 'Water')");
//            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Pikachu', 'Electric')");

            System.out.println("Database gevuld met 3 pokemon.");

            System.out.println("Create fire-pokemon table");
            stmt.execute("DROP TABLE IF EXISTS \"fire-pokemon\"");
            stmt.execute("""
                    CREATE TABLE "fire-pokemon" (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100),
                        type VARCHAR(100),
                        version INT DEFAULT 1,
                        weaknesses VARCHAR(100)
                    )
                """);

            System.out.println("Inserting 1 fire pokemon");
            stmt.executeUpdate("INSERT INTO \"fire-pokemon\" (name, type, weaknesses) VALUES ('Charizard', 'Fire', 'Water')");

            System.out.println("Database gevuld met 1 fire pokemon.");

            System.out.println("Create attack table");
            stmt.execute("""
                    CREATE TABLE attack (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100),
                        damage INT,
                        version INT DEFAULT 1,
                        pokemon_id BIGINT,
                        FOREIGN KEY (pokemon_id) REFERENCES pokemon(id)
                    )
                """);

            System.out.println("Inserting 6 attacks");
            stmt.executeUpdate("INSERT INTO attack (name, damage, pokemon_id) VALUES ('Vine Whip', 45, 1)");
            stmt.executeUpdate("INSERT INTO attack (name, damage, pokemon_id) VALUES ('Solar Beam', 120, 1)");
            stmt.executeUpdate("INSERT INTO attack (name, damage, pokemon_id) VALUES ('Water Gun', 40, 2)");
            stmt.executeUpdate("INSERT INTO attack (name, damage, pokemon_id) VALUES ('Hydro Pump', 110, 2)");
            stmt.executeUpdate("INSERT INTO attack (name, damage, pokemon_id) VALUES ('Thunderbolt', 90, 3)");
            stmt.executeUpdate("INSERT INTO attack (name, damage, pokemon_id) VALUES ('Thunder', 110, 3)");

            System.out.println("Database gevuld met 6 attacks.");

//            System.out.println("Create persons table");
//            stmt.execute("DROP TABLE IF EXISTS persons");
//            stmt.execute("""
//                    CREATE TABLE persons (
//                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
//                        name VARCHAR(100),
//                        age INT,
//                        starter VARCHAR(100),
//                        totalBattles INT
//                    )
//                """);
            
            printTable(Pokemon.class);
        }
        catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Converteert een lijst Pokémon naar leesbare weergavestrings voor de JSON-respons.
     */
    private static List<String> naarStrings(List<Pokemon> pokemons)
    {
        return pokemons.stream()
                .map(p -> p.getId() + " | " + p.getName() + " | " + p.getType())
                .collect(Collectors.toList());
    }

    /**
     * Bouwt een JSON-object met een {@code results}-array op basis van de opgegeven lijst.
     */
    private static String buildJson(List<String> results)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        for (int i = 0; i < results.size(); i++)
        {
            sb.append("\"").append(results.get(i).replace("\"", "'")).append("\"");
            if (i < results.size() - 1)
                sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }
    
    private static void registerEntity(Class<?> entityClass) {
    	EntityMetadata<?> entityMetadata = new EntityMetadata<>(entityClass);
    	if (entityMetadata.isRoot())
    		rootEntities.add(entityMetadata);
    	else {
    		
    		rootEntities.stream()
    		.filter(rootEntity -> rootEntity.getEntityClass().isAssignableFrom(entityMetadata.getRootEntity()))
    		.findFirst()
    		.get()
    		.registerChild(entityMetadata);
    	}
    }
    
    private static void dropAndCreateTable(EntityMetadata<?> rootEntity, Connection conn)
    {
    	try (PreparedStatement stmt = conn.prepareStatement("DROP TABLE IF EXISTS " + rootEntity.getTableName())) {
    		stmt.execute();
    		System.out.println("\nDropped table " + rootEntity.getTableName() + " with SQL: " + stmt);
        } catch (SQLException e) {
			System.out.println("Couldn't drop table: " + rootEntity.getTableName());
			e.printStackTrace();
		}

    	StringBuilder tableBuilder = new StringBuilder("CREATE TABLE " + rootEntity.getTableName() + " (id BIGINT AUTO_INCREMENT PRIMARY KEY, ");
    	
    	if (rootEntity.getDiscriminatorColumn() != null) {
    		tableBuilder.append(rootEntity.getDiscriminatorColumn().name() + " VARCHAR(255), ");
    	}
    	
    	Iterator<FieldMetadata> fieldIterator = rootEntity.getAllFields().iterator();
    	while (fieldIterator.hasNext()) {
    		FieldMetadata field = fieldIterator.next();
   			tableBuilder.append(toDBQueryColumnParameter(field));
    		tableBuilder.append(fieldIterator.hasNext() ? ", " : ")");
    	}
    	
    	try (PreparedStatement stmt = conn.prepareStatement(tableBuilder.toString())) {
            stmt.execute();
            System.out.println("Creating table " + rootEntity.getTableName() + " with SQL: " + stmt);
        } catch (SQLException e) {
			System.out.println("Couldn't generate table for entity: " + rootEntity.getTableName());
			e.printStackTrace();
		}
    }
    
    private static String toDBQueryColumnParameter(FieldMetadata fieldMetadata)
    {
    	StringBuilder dbColumnBuilder = new StringBuilder(fieldMetadata.getColumnName());
    	String type = MetadataHelper.columnTypeFor(fieldMetadata.getField().getType());
    	dbColumnBuilder.append(type);
    	
    	if (fieldMetadata.hasDefaultValue())
    		dbColumnBuilder.append(" DEFAULT " + fieldMetadata.getDefaultValue());
    	
    	return dbColumnBuilder.toString();
    }
    
    private static void printTable(Class<?> entityClass)
    {
    	System.out.println("Retrieving all data for entity: " + entityClass.getSimpleName());
    }
}
