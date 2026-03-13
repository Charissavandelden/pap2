package nl.topicus.injection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

public class InjectionApp
{
    public static void main(String[] args) throws IOException
    {
        DataSource datasource = ConnectionManager.getDataSource();
        PokemonDao dao = new PokemonDao(datasource);

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

        server.setExecutor(null);
        server.start();
        System.out.println("Server gestart op http://localhost:8080");
        System.out.println("Druk op Ctrl+C om te stoppen.");
    }

    private static void serveSearchByNaamEndpoint(HttpServer server, PokemonDao dao)
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
                List<String> results = dao.findByName(name);
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

    private static void serveSearchByTypeEndpoint(HttpServer server, PokemonDao dao)
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
                List<String> results = dao.findByType(type);
                sendHTTPRequest(exchange, results);
            }
            catch (SQLException e)
            {
                printError(exchange, e);
            }
        });
    }

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

    private static void setupDatabase(Connection conn) throws SQLException
    {
        try (Statement stmt = conn.createStatement())
        {
            stmt.execute("DROP TABLE IF EXISTS pokemon");
            stmt.execute("""
                    CREATE TABLE pokemon (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100),
                        type VARCHAR(100)
                    )
                """);
            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Bulbasaur', 'Grass')");
            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Charizard', 'Fire')");
            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Squirtle', 'Water')");
            stmt.executeUpdate("INSERT INTO pokemon (name, type) VALUES ('Pikachu', 'Electric')");

            System.out.println("Database gevuld met 4 pokemon.");
        }
        catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
    }

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
}
