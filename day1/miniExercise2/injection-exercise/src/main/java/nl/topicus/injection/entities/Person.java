package nl.topicus.injection.entities;

import nl.topicus.injection.annotation.*;
import nl.topicus.injection.annotation.Inheritance.InheritanceType;

@Entity
@Inheritance(type = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn
@Table(name = "persons")
public abstract class Person {
	@Id
	private long id;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "age")
	private int age;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
	
	
}
