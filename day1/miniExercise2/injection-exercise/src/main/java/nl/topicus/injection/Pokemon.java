package nl.topicus.injection;

import nl.topicus.injection.annotation.Column;
import nl.topicus.injection.annotation.Entity;
import nl.topicus.injection.annotation.Id;
import nl.topicus.injection.annotation.Table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
