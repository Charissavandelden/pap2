package nl.topicus.injection.entities;

import nl.topicus.injection.annotation.*;
import nl.topicus.injection.annotation.Inheritance.InheritanceType;

@Entity
@Inheritance(type = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value = "GYM_LEADER")
public class GymLeader extends Person{
	
	@Column(name = "totalBattles")
	private int totalBattles;

	public int getTotalBattles() {
		return totalBattles;
	}

	public void setTotalBattles(int totalBattles) {
		this.totalBattles = totalBattles;
	}
}
