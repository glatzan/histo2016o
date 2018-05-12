package org.histo.action.dialog.patient;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.model.patient.Patient;
import org.histo.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class ConfirmPatientData extends AbstractDialog {

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

	private String headlineAsResource;

	public void initAndPrepareBean(Patient patient) {
		initBean(patient, null);
		prepareDialog();
	}

	public void initAndPrepareBean(Patient patient, String headlineAsResource) {
		initBean(patient, headlineAsResource);
		prepareDialog();
	}

	public void initBean(Patient patient, String headlineAsResource) {
		super.initBean(null, Dialog.PATIENT_DATA_CONFIRM, false);
		setPatient(patient);
		setConfirmed(false);
		setHeadlineAsResource(headlineAsResource);
	}

	public void confirmAndHideDialog() {
		super.hideDialog(patient);	
	}
}
