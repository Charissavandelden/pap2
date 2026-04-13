package nl.topicus.injection;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import nl.topicus.injection.entities.Pokemon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO voor het beheren van {@link Pokemon} entiteiten.
 * Erft de vijf generieke CRUD-operaties en voegt Pokémon-specifieke zoekmethoden toe.
 */
public class PokemonRepository extends AbstractDataSourceRepository<Pokemon> implements IPokemonDAO {

    public PokemonRepository(@Nonnull DataSource datasource) {
        super(Pokemon.class, datasource);
    }

    /**
     * Zoekt alle Pokémon waarvan het type de opgegeven tekst bevat (hoofdletterongevoelig).
     */
    @Override
    @Nonnull
    public List<Pokemon> findByType(@Nonnull String type) throws SQLException {
        return zoekMetLike("type", type);
    }

    /**
     * Zoekt alle Pokémon waarvan de naam de opgegeven tekst bevat (hoofdletterongevoelig).
     */
    @Override
    @Nonnull
    public List<Pokemon> findByName(@Nonnull String name) throws SQLException {
        return zoekMetLike("name", name);
    }

    /**
     * Voert een SELECT-query uit met een LIKE-filter op de opgegeven kolom.
     */
    private List<Pokemon> zoekMetLike(@Nonnull String kolom, @Nonnull String waarde) throws SQLException {
        TransactionManager transactionManager = new TransactionManager();
                List<Pokemon> results = new ArrayList<>();


        transactionManager.runInTransaction(() -> {
            try {
                String sql = "SELECT * FROM pokemon WHERE " + kolom + " LIKE ?";

                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, "%" + waarde + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(metadata.mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e); // must wrap checked exception
            }
        });


//        List<Pokemon> results = new ArrayList<>();
//        String sql = "SELECT * FROM pokemon WHERE " + kolom + " LIKE ?";
//        try (Connection conn = getConnection();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setString(1, "%" + waarde + "%");
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    results.add(metadata.mapRow(rs));
//                }
//            }
//        }
        return results;
    }
}
