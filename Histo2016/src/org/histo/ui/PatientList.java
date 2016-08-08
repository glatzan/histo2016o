package org.histo.ui;

import javax.persistence.Transient;

import org.histo.model.Patient;

/**
 * Class with temporary id for selecting patients from the clinic backend and
 * the histo backend from a p:datatable (a specific id column is needed and
 * patients from the clinic database have no id)
 * 
 * @author glatza
 *
 */
public class PatientList {
		
	/**
	 * Unique, temprary id of the patient
	 */
	private int id;
	
	/**
	 * Patient 
	 */
	private Patient patient;

	/**
	 *	True if only in clinic database
	 */
	private boolean notHistoDatabase = false; 
	
	public PatientList(int id, Patient patient){
		this.id = id;
		this.patient = patient;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public boolean isNotHistoDatabase() {
		return notHistoDatabase;
	}

	public void setNotHistoDatabase(boolean notHistoDatabase) {
		this.notHistoDatabase = notHistoDatabase;
	}
	
}
