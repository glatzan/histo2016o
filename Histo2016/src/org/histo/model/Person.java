package org.histo.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Gender;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.LogAble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "person_sequencegenerator", sequenceName = "person_sequence")
public class Person implements Serializable, LogAble, ArchivAble {

	private static final long serialVersionUID = 2533238775751991883L;

	private long version;

	@Expose
	protected long id;
	@Expose
	protected Gender gender = Gender.UNKNOWN;
	@Expose
	protected String title = "";
	@Expose
	protected String name = "";
	@Expose
	protected String street = "";
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
	protected String mobileNumber = "";
	@Expose
	protected String fax = "";
	@Expose
	protected String email = "";
	@Expose
	protected String country = "";
	@Expose
	protected String department = "";

	protected boolean archived;

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

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
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

	@Type(type = "date")
	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Enumerated(EnumType.ORDINAL)
	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender geneder) {
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

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	@Column(length = 255)
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	@Transient
	public String patienDataAsGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}

	/**
	 * Returns a full name with title, name and surname.
	 * 
	 * @return
	 */
	@Transient
	public String getFullName() {
		StringBuilder result = new StringBuilder();

		if (getTitle() != null && !getTitle().isEmpty())
			result.append(getTitle() + " ");

		if (getName() != null && !getName().isEmpty())
			result.append(getName() + " ");

		if (getSurname() != null && !getSurname().isEmpty())
			result.append(getSurname() + " ");

		// remove the last space from the string
		if (result.length() > 0)
			return result.substring(0, result.length() - 1);
		else
			return "";
	}

	/**
	 * Returns a title + name, if no title is provided,
	 * 
	 * @return
	 */
	@Transient
	public String getFullNameAndTitle() {
		StringBuilder result = new StringBuilder();

		if (getTitle() != null && !getTitle().isEmpty())
			result.append(getTitle() + " ");
		else {
			// TODO hardcoded!
			if (getGender() == Gender.FEMALE)
				result.append("Frau ");
			else
				result.append("Herr ");

		}

		int index = result.indexOf("Apl.");
		if (index != -1)
			result.replace(index, 4, "");

		if (getName() != null && !getName().isEmpty())
			result.append(getName() + " ");

		return result.substring(0, result.length() - 1);
	}

	/********************************************************
	 * Interace archive able
	 ********************************************************/
	@Override
	public boolean isArchived() {
		return archived;
	}

	@Override
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	@Override
	@Transient
	public String getTextIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Transient
	public Dialog getArchiveDialog() {
		// TODO Auto-generated method stub
		return null;
	}
	/********************************************************
	 * Interace archive able
	 ********************************************************/

}
