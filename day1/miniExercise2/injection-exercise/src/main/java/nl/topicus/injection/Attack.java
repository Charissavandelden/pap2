package nl.topicus.injection;

import nl.topicus.injection.annotation.Column;
import nl.topicus.injection.annotation.Entity;
import nl.topicus.injection.annotation.Id;
import nl.topicus.injection.annotation.Table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Vertegenwoordigt een aanval die een Pokémon kan uitvoeren.
 */
@Entity
@Table(name = "attack")
public class Attack {

    @Id
    long id;

    @Column(name = "name")
    String name;

    @Column(name = "damage")
    int damage;

    public Attack() {}

    public Attack(@Nonnull String name, int damage) {
        this.name = name;
        this.damage = damage;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }
}

