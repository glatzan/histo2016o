package org.histo.action.dialog.patient;

import java.util.Date;


import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class EditPatientDialog extends AbstractDialog {

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;
	
	@Autowired
	private PatientDao patientDao;
	
	private Patient patient;
	
	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	public void initBean(Patient patient) {
		try {
			setPatient(genericDAO.refresh(patient));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			setPatient(patientDao.getPatient(patient.getId(), true));
			worklistViewHandlerAction.replacePatientInCurrentWorklist(getPatient());
		}
		super.initBean(null, Dialog.PATIENT_EDIT);
		
		setPatient(patient);
	}

	// ************************ Getter/Setter ************************
	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}
}
