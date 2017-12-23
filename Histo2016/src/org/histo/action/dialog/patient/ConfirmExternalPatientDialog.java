package org.histo.action.dialog.patient;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.patient.Patient;
import org.primefaces.context.RequestContext;
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

	private Patient patient;

	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	public void initBean(Patient patient) {
		super.initBean(null, Dialog.PATIENT_EXTERNAL_CONFIRM, false);
		setPatient(patient);
	}

	/**
	 * Creates an external patient and closes the dialog, prepares the closure
	 * of the parent dialog
	 */
	public void createExternalPatient() {
		try {
			dialogHandlerAction.getAddPatientDialogHandler().getExternalPatientTab().addExternalPatient(patient, true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("growl.version.error"),
					resourceBundle.get("growl.version.error.text"));
			hideDialog();
			return;
		}

		hideDialog(true);
	}

	public void hideDialog() {
		hideDialog(false);
	}

	public void hideDialog(boolean closeParent) {
		RequestContext.getCurrentInstance().closeDialog(new Boolean(closeParent));
	}

}
