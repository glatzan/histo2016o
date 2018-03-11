package org.histo.action.dialog.patient;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.patient.Patient;
import org.histo.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ConfirmExternalPatientDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientService patientService;
	
	private Patient patient;
	
	private boolean confirmed;

	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	public void initBean(Patient patient) {
		super.initBean(null, Dialog.PATIENT_EXTERNAL_CONFIRM, false);
		setPatient(patient);
		setConfirmed(false);
	}

	/**
	 * Creates an external patient and closes the dialog, prepares the closure
	 * of the parent dialog
	 */
	public void createExternalPatient() {
		try {
			// creating patient
			patientService.createExternalPatient(patient);
			// adding to worklist
			worklistViewHandlerAction.addPatientToWorkList(getPatient(), true, true);
			
			setConfirmed(true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			mainHandlerAction.sendGrowlMessagesAsResource("growl.error", "growl.error.version");
			hideDialog();
			return;
		}
	}

	/**
	 * Returns true if the patient creation was confimed and the parent dialog should be closed
	 */
	public void hideDialog() {
		super.hideDialog(new Boolean(confirmed));
	}

}
