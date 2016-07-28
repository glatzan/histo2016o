package org.histo.model;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "person_sequencegenerator", sequenceName = "person_sequence")
public class Person {

    @Expose
    private long id;
    @Expose
    private String gender;
    @Expose
    private String title;
    @Expose
    private String name;
    @Expose
    private String street;
    @Expose
    private String houseNumber;
    @Expose
    private int postcode;
    @Expose
    private String town;
    @Expose
    private String sureName;
    @Expose
    private Date birthday;
    @Expose
    private String phoneNumber;
    @Expose
    private String fax;
    @Expose
    private String email;
    
    
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

    @Column
    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    @Column
    public String getSureName() {
	return sureName;
    }

    public void setSureName(String sureName) {
	this.sureName = sureName;
    }

    @Column()
    @Type(type="date")
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
    public String getGender() {
	return gender;
    }

    public void setGender(String geneder) {
	this.gender = geneder;
    }

    @Column
    public String getStreet() {
	return street;
    }

    public void setStreet(String street) {
	this.street = street;
    }

    @Column
    public int getPostcode() {
	return postcode;
    }

    public void setPostcode(int postcode) {
	this.postcode = postcode;
    }

    @Column
    public String getTown() {
	return town;
    }

    public void setTown(String town) {
	this.town = town;
    }

    @Column
    public String getPhoneNumber() {
	return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
	this.phoneNumber = phoneNumber;
    }

    @Column
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

    @Transient
    public String patienDataAsGson() {
	final GsonBuilder builder = new GsonBuilder();
	builder.excludeFieldsWithoutExposeAnnotation();
	final Gson gson = builder.create();
	return gson.toJson(this);
    }
}
