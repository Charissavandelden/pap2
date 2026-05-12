package nl.topicus.orm.entities;

import nl.topicus.orm.annotation.Column;
import nl.topicus.orm.annotation.DiscriminatorValue;
import nl.topicus.orm.annotation.Entity;
import nl.topicus.orm.annotation.Inheritance;
import nl.topicus.orm.annotation.Inheritance.InheritanceType;

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
