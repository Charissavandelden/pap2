package nl.topicus.injection;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.List;

/**
 * Definieert Pokémon-specifieke zoekmethoden aanvullend op de generieke CRUD-operaties.
 */
public interface IPokemonDAO {
    /**
     * Zoekt alle Pokémon waarvan het type de opgegeven tekst bevat.
     */
    @Nonnull List<Pokemon> findByType(@Nonnull String type) throws SQLException;

    /**
     * Zoekt alle Pokémon waarvan de naam de opgegeven tekst bevat.
     */
    @Nonnull List<Pokemon> findByName(@Nonnull String name) throws SQLException;
}
