package org.histo.model.util;

import org.histo.model.patient.Patient;

/**
 * Object containing additional data for login. A string which describes the
 * action and optional a patient for whom the action was performed. Workaround
 * for adding more data to the LogListener
 * 
 * @author glatza
 *
 */
public class LogInfo {
	
	private Patient patient;
	
	private String info;

	public LogInfo() {
	}

	public LogInfo(String info) {
		this.info = info;
	}

	public LogInfo(String info, Patient patient) {
		this.info = info;
		this.patient = patient;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}