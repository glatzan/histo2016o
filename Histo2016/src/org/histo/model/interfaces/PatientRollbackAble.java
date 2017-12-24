package org.histo.model.interfaces;

import org.histo.model.patient.Patient;

public interface PatientRollbackAble<T extends PatientRollbackAble<?>> extends HasID {

	public Patient getPatient();
	
	public T getParent();

	public void setParent(T parent);

	/**
	 * Returns a hierarchical path for logging the object
	 * 
	 * @return
	 */
	public default String getLogPath() {
		return getParent().getLogPath() + " / " + toString();
	}
}
