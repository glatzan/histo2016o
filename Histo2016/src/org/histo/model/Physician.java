package org.histo.model;

import javax.persistence.Basic;
import javax.persistence.Entity;

import com.google.gson.annotations.Expose;

@Entity
public class Physician extends Person {


	@Expose
	private String fullName;
	
	@Expose
	private String pager;
	
	/**
	 * clinic internal title
	 */
	@Expose
	private String clinicTitle;
	
	/**
	 * clinic internal department
	 */
	@Expose
	private String department;

	private boolean surgeon;

	private boolean extern;

	private boolean other;

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


	public String getClinicTitle() {
	    return clinicTitle;
	}

	public void setClinicTitle(String clinicTitle) {
	    this.clinicTitle = clinicTitle;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
}
