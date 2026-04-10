package nl.topicus.injection;

import nl.topicus.injection.annotation.Column;
import nl.topicus.injection.annotation.Entity;
import nl.topicus.injection.annotation.Table;

import javax.annotation.Nonnull;

/**
 * Vertegenwoordigt een vuur-type Pokémon, opgeslagen in de eigen tabel "fire-pokemon".
 * Het type is altijd "Fire" en de zwakte is altijd "Water".
 */
@Entity
@Table(name = "fire-pokemon")
public class FirePokemon extends Pokemon {

    @Column(name = "weaknesses")
    String weaknesses = "Water";

    public FirePokemon() {
        setType("Fire");
    }

    @Nonnull
    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(@Nonnull String weaknesses) {
        this.weaknesses = weaknesses;
    }
}
