package nl.topicus.injection;

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

public class InjectionApp {

    public static void main(String[] args) throws SQLException, IOException {
    	DataSource datasource = ConnectionManager.getDataSource();
        try (Connection conn = datasource.getConnection()) {
        	setupDatabase(conn);
		} catch (Exception e2) {
			// TODO: handle exception
		}
        StudentDao dao = new StudentDao(datasource);
    	

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve de frontend pagina
        server.createContext("/", exchange -> {
            try (InputStream is = InjectionApp.class.getResourceAsStream("/frontend/index.html")) {
                byte[] html = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html);
                }
            }
        });

        // API endpoint voor zoeken op e-mail
        server.createContext("/api/search", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String email = "";
            if (query != null && query.startsWith("email=")) {
                email = URLDecoder.decode(query.substring(6), StandardCharsets.UTF_8);
            }

            try {
                List<String> results = dao.findByEmail(email);
                String json = buildJson(results);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                byte[] response = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (SQLException e) {
                String error = "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                byte[] response = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });

        // TODO: Voeg hier een nieuw endpoint /api/search-by-name toe
        //       dat de findByName methode van StudentDao aanroept.
        // API endpoint voor zoeken op naam
        server.createContext("/api/search-by-name", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String name = "";
            if (query != null && query.startsWith("name=")) {
                name = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
            }

            try {
                List<String> results = dao.findByName(name);
                String json = buildJson(results);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                byte[] response = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (SQLException e) {
                String error = "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                byte[] response = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server gestart op http://localhost:8080");
        System.out.println("Druk op Ctrl+C om te stoppen.");
    }

    private static void setupDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS students");
            stmt.execute("""
                CREATE TABLE students (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    age INT
                )
            """);

            stmt.executeUpdate("INSERT INTO students (name, email, age) VALUES ('Alice de Vries', 'alice@university.nl', 21)");
            stmt.executeUpdate("INSERT INTO students (name, email, age) VALUES ('Bob Jansen', 'bob@university.nl', 23)");
            stmt.executeUpdate("INSERT INTO students (name, email, age) VALUES ('Charlie Bakker', 'charlie@university.nl', 22)");
            stmt.executeUpdate("INSERT INTO students (name, email, age) VALUES ('Diana Smit', 'diana@university.nl', 24)");
            stmt.executeUpdate("INSERT INTO students (name, email, age) VALUES ('Eva Peters', 'eva@university.nl', 20)");

            System.out.println("Database gevuld met 5 studenten.");
        }
    }

    private static String buildJson(List<String> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        for (int i = 0; i < results.size(); i++) {
            sb.append("\"").append(results.get(i).replace("\"", "'")).append("\"");
            if (i < results.size() - 1) sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }
}
