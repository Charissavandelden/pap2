package nl.topicus.injection.entities;

import nl.topicus.injection.annotation.Column;
import nl.topicus.injection.annotation.DiscriminatorValue;
import nl.topicus.injection.annotation.Entity;
import nl.topicus.injection.annotation.Inheritance;
import nl.topicus.injection.annotation.Inheritance.InheritanceType;

@Entity
@Inheritance(type = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value = "TRAINER")
public class Trainer extends Person{
	
	@Column(name = "starter")
	private Pokemon starter;

	public Pokemon getStarter() {
		return starter;
	}

	public void setStarter(Pokemon starter) {
		this.starter = starter;
	}
}
