package org.histo.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "person_sequencegenerator", sequenceName = "person_sequence")
public class Person implements Serializable {

	private static final long serialVersionUID = 2533238775751991883L;
	
	public static final char GENDER_MALE = 'M';
	public static final char GENDER_FEMALE = 'W';

	@Expose
	protected long id;
	@Expose
	protected char gender = ' ';
	@Expose
	protected String title = "";
	@Expose
	protected String name = "";
	@Expose
	protected String street = "";
	@Expose
	protected String houseNumber = "";
	@Expose
	protected String postcode = "";
	@Expose
	protected String town = "";
	@Expose
	protected String surname = "";
	@Expose
	protected Date birthday = null;
	@Expose
	protected String phoneNumber = "";
	@Expose
	protected String fax= "";
	@Expose
	protected String email = "";
	@Expose
	protected String land= "";

	public Person() {
	}

	public Person(String name) {
		setName(name);
	}

	@Id
	@GeneratedValue(generator = "person_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(length = 255)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(length = 255)
	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	@Column()
	@Type(type = "date")
	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	@Column
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Column
	public char getGender() {
		return gender;
	}

	public void setGender(char geneder) {
		this.gender = geneder;
	}

	@Column(length = 255)
	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	@Column(length = 255)
	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	@Column(length = 255)
	public String getTown() {
		return town;
	}

	public void setTown(String town) {
		this.town = town;
	}

	@Column(length = 255)
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Column(length = 255)
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Column
	public String getHouseNumber() {
		return houseNumber;
	}

	public void setHouseNumber(String houseNumber) {
		this.houseNumber = houseNumber;
	}

	@Column
	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	@Column
	public String getLand() {
		return land;
	}

	public void setLand(String land) {
		this.land = land;
	}

	@Transient
	public String patienDataAsGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}
}
