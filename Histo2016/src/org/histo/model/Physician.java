package org.histo.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.ContactRole;

import com.google.gson.annotations.Expose;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
public class Physician extends Person {

	private static final long serialVersionUID = 7358147861813210904L;

	/**
	 * Full name Title, Name, Surname
	 */
	@Expose
	private String fullName;

	/**
	 * Pager Number
	 */
	@Expose
	private String pager;

	/**
	 * clinic internal title
	 */
	@Expose
	private String clinicRole;

	/**
	 * Number of the employee
	 */
	@Expose
	private String employeeNumber;

	/**
	 * Loginname of the physician
	 */
	@Expose
	private String uid;

	/**
	 * Default role of this physician
	 */
	private ContactRole defaultContactRole = ContactRole.OTHER;

	/**
	 * True if clinic employee
	 */
	private boolean clinicEmployee;

	/**
	 * Standard constructor for hibernate
	 */
	public Physician() {
	}

	/**
	 * Constructor with id
	 * 
	 * @param id
	 */
	public Physician(long id) {
		this.id = id;
	}

	public String getPager() {
		return pager;
	}

	public void setPager(String pager) {
		this.pager = pager;
	}

	public String getClinicRole() {
		return clinicRole;
	}

	public void setClinicRole(String clinicRole) {
		this.clinicRole = clinicRole;
	}

	public String getFullName() {
		if (fullName == null)
			return getTitle() + " " + getName() + " " + getSurname();
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Enumerated(EnumType.STRING)
	public ContactRole getDefaultContactRole() {
		return defaultContactRole;
	}

	public void setDefaultContactRole(ContactRole defaultContactRole) {
		this.defaultContactRole = defaultContactRole;
	}

	public boolean isClinicEmployee() {
		return clinicEmployee;
	}

	public void setClinicEmployee(boolean clinicEmployee) {
		this.clinicEmployee = clinicEmployee;
	}
}
