package org.histo.action.dialog.patient;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.util.event.PatientMergeEvent;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

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
			worklistViewHandlerAction.replacePatientInCurrentWorklist(getPatient(), false);
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

	/**
	 * Is called from return of the merge dialog. If merging was successful the edit
	 * patient dialog will be closed with the event object
	 * 
	 * @param event
	 */
	public void onMergeReturn(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof PatientMergeEvent) {
			hideDialog((PatientMergeEvent)event.getObject());
		}
	}

	public void onDatabaseVersionConflict() {
		worklistViewHandlerAction.replacePatientInCurrentWorklist(getTask().getParent());
		super.onDatabaseVersionConflict();
	}
}
