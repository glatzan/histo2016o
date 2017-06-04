package org.histo.action.dialog.patient;

import java.util.Date;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
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
	private WorklistHandlerAction worklistHandlerAction;
	
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
			logger.debug("!! Version inconsistent with Database updating");
			setPatient(patientDao.getPatient(patient.getId(), true));
			worklistHandlerAction.updatePatientInCurrentWorklist(getPatient());
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
