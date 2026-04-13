package nl.topicus.injection.entities;

import nl.topicus.injection.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

/**
 * Vertegenwoordigt een aanval die een Pokémon kan uitvoeren.
 */
@Entity
@Table(name = "attack")
public class Attack
{

	@Id
	long id;

	@Column(name = "name")
	String name;

	@Column(name = "damage")
	int damage;

	@Column()
	int version;

	public void setId(long id)
	{
		this.id = id;
	}

	public int getVersion()
	{
		return version;
	}

	public void setVersion(int version)
	{
		this.version = version;
	}

	public Attack()
	{
	}

	public Attack(@Nonnull String name, int damage)
	{
		this.name = name;
		this.damage = damage;
	}

	public long getId()
	{
		return id;
	}

	@Nullable
	public String getName()
	{
		return name;
	}

	public void setName(@Nonnull String name)
	{
		this.name = name;
	}

	public int getDamage()
	{
		return damage;
	}

	public void setDamage(int damage)
	{
		this.damage = damage;
	}
}

