package org.histo.model;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "physician_sequencegenerator", sequenceName = "physician_sequence")
public class Physician {

	private long id;

	private Person person;

	@Expose
	private String pager;
	
	/**
	 * clinic internal title
	 */
	@Expose
	private String title;
	
	/**
	 * clinic internal department
	 */
	@Expose
	private String department;

	private boolean surgeon;

	private boolean extern;

	private boolean other;

	@Id
	@GeneratedValue(generator = "physician_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@Basic
	public boolean isSurgeon() {
		return surgeon;
	}

	public void setSurgeon(boolean surgeon) {
		this.surgeon = surgeon;
	}

	@Basic
	public boolean isExtern() {
		return extern;
	}

	public void setExtern(boolean extern) {
		this.extern = extern;
	}

	@Basic
	public boolean isOther() {
		return other;
	}

	public void setOther(boolean other) {
		this.other = other;
	}

	public String getPager() {
		return pager;
	}

	public void setPager(String pager) {
		this.pager = pager;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}
	
}
