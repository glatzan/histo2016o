package org.histo.model.interfaces;

public interface IdManuallyAltered extends PatientRollbackAble {

	public boolean isIdManuallyAltered();

	public void setIdManuallyAltered(boolean idManuallyAltered);
}
