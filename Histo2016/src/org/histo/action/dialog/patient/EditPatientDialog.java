package org.histo.action.dialog.patient;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class EditPatientDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	private Patient patient;

	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	public void initBean(Patient patient) {
		try {
			setPatient(genericDAO.reattach(patient));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			setPatient(patientDao.getPatient(patient.getId(), true));
			worklistViewHandlerAction.replacePatientInCurrentWorklist(getPatient());
		}
		super.initBean(null, Dialog.PATIENT_EDIT);

		setPatient(patient);
	}

	public void savePatientData() {
		try {
			genericDAO.savePatientData(getPatient(), "log.patient.edit");
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void onDatabaseVersionConflict() {
		worklistViewHandlerAction.replacePatientInCurrentWorklist(getTask().getParent().getId());
		super.onDatabaseVersionConflict();
	}
}
