package nl.topicus.injection.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import nl.topicus.injection.annotation.Column;
import nl.topicus.injection.annotation.Entity;
import nl.topicus.injection.annotation.Id;
import nl.topicus.injection.annotation.Table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Vertegenwoordigt een Pokémon-entiteit in de database.
 */
@Entity
@Table(name = "pokemon")
public class Pokemon {

    @Id
    long id;

    @Column(name = "name")
    String name;

    @Column(name = "type")
    String type;

    @Column(name = "version")
    int version;

    @OneToMany(mappedBy = "attack", cascade = CascadeType.PERSIST)
    private List<Attack> attacks = new ArrayList<>();

    public Pokemon() {
    }

    public Pokemon(String name, String type) {
    	this.name = name;
    	this.type = type;
    }

    public List<Attack> getAttacks()
    {
        return attacks;
    }

    public void setAttacks(List<Attack> attacks)
    {
        this.attacks = attacks;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nullable
    public String getType() {
        return type;
    }

    public void setType(@Nonnull String type) {
        this.type = type;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }
}
