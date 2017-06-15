package org.histo.model.interfaces;

import org.histo.model.patient.Task;

public interface IdManuallyAltered extends PatientRollbackAble {

	public boolean isIdManuallyAltered();

	public void setIdManuallyAltered(boolean idManuallyAltered);
	
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems);
	
	public Task getTask();
}
