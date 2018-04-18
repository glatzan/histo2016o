package org.histo.action.dialog.patient;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.service.PatientService;
import org.primefaces.event.SelectEvent;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class MergePatientDialog extends AbstractDialog {

	public static final int MERGE_PIZ = 0;
	public static final int MERGE_PATIENT = 1;

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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ConfirmPatientData confirmPatientData;

	private Patient patient;

	private int mergeOption;

	private String piz;

	private Patient patientToMerge;

	/**
	 * Local Dialog for confirming the merge
	 */
	private ConfirmDialog confirmDialog = new ConfirmDialog();

	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	public void initBean(Patient patient) {
		super.initBean(null, Dialog.PATIENT_MERGE, false);
		setPatient(patient);
		setMergeOption(MERGE_PIZ);
		setPatientToMerge(null);
		setPiz("");
	}

	public void checkMergePatient() {
		if (mergeOption == 0) {
			try {
				patientToMerge = patientService.serachForPiz(piz, false);
			} catch (CustomDatabaseInconsistentVersionException | JSONException | CustomExceptionToManyEntries
					| CustomNullPatientExcepetion e) {
			}

			if (patientToMerge == null)
				mainHandlerAction.sendGrowlMessagesAsResource("growl.error", "growl.patientNoFound");
			else
				getConfirmDialog().initAndPrepareBean();
		}
	}

	public void onMergePatient(SelectEvent event) {
		logger.debug("Trying to merge patients" + (event.getObject() instanceof Boolean)
				+ ((Boolean) event.getObject()).booleanValue());
		try {
			if (event.getObject() != null && event.getObject() instanceof Boolean
					&& ((Boolean) event.getObject()).booleanValue()) {
				System.out.println(patient + " " + patientToMerge);
				if (patient != null && patientToMerge != null) {

					if (patientToMerge.getId() == 0)
						patientService.addPatient(patient, false);

					patientService.mergePatient(patient, patientToMerge);
					mainHandlerAction.sendGrowlMessagesAsResource("growl.success", "growl.patient.merge.success");
					hideDialog(new Boolean(true));
				}
			}
		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}

	public void onSelectPatient(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof Patient) {
			patientToMerge = (Patient) event.getObject();
		}
	}

	/**
	 * Local Dialog for confirming the merge
	 */
	public class ConfirmDialog extends AbstractDialog {

		public void initAndPrepareBean() {
			super.initAndPrepareBean(Dialog.PATIENT_MERGE_CONFIRM);
		}

		public void confirmAndClose() {
			hideDialog(new Boolean(true));
		}
	};
}