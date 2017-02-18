package org.histo.model.interfaces;

import org.histo.model.patient.Patient;

public interface SaveAble {

	public Patient getPatient();

	/**
	 * Returns a hierarchical path for logging the object
	 * 
	 * @return
	 */
	public default String getLogPath() {
		return "";
	}
}
