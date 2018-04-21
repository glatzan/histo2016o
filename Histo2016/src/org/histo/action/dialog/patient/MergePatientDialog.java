package org.histo.action.dialog.patient;

import java.util.List;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.service.PatientService;
import org.histo.util.HistoUtil;
import org.histo.util.event.PatientMergeEvent;
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

	/**
	 * Patient source for merging
	 */
	private Patient patient;

	/**
	 * Selected tasks for merging
	 */
	private List<Task> tasksTomerge;

	/**
	 * True if an external patient should be deleted
	 */
	private boolean deletePatient;

	/**
	 * True if not all tasks of the source are selected
	 */
	private boolean deletePatientDisabled;

	/**
	 * Merge Option,
	 */
	private MergeOption mergeOption;

	/**
	 * Piz for search a patient for merge target
	 */
	private String piz;

	/**
	 * Patient as merge target
	 */
	private Patient patientToMerge;

	private boolean renderErrorPatientNotFound;

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
		setTasksTomerge(patient.getTasks());
		setMergeOption(MergeOption.PIZ);
		setPatientToMerge(null);
		setPiz("");
	}

	public void onChangeMergeOption() {
		renderErrorPatientNotFound = false;
		patientToMerge = null;

		if (mergeOption == MergeOption.PIZ)
			onSelectPatientViaPiz();
	}

	public void onSelectPatientViaPiz() {
		if (HistoUtil.isNotNullOrEmpty(piz) && piz.matches("^\\d{8}$")) {
			try {
				patientToMerge = patientService.serachForPiz(piz, false);
			} catch (CustomDatabaseInconsistentVersionException | JSONException | CustomExceptionToManyEntries
					| CustomNullPatientExcepetion e) {
			} finally {
				if (patientToMerge == null)
					renderErrorPatientNotFound = true;
				else
					renderErrorPatientNotFound = false;
			}
		}
	}

	public void onMergePatient(SelectEvent event) {
		logger.debug("Trying to merge patients" + (event.getObject() instanceof Boolean)
				+ ((Boolean) event.getObject()).booleanValue());
		try {
			if (event.getObject() != null && event.getObject() instanceof Boolean
					&& ((Boolean) event.getObject()).booleanValue()) {
				if (patient != null && patientToMerge != null) {

					if (patientToMerge.getId() == 0)
						patientService.addPatient(patientToMerge, false);

					patientService.mergePatient(patient, patientToMerge, tasksTomerge);
					mainHandlerAction.sendGrowlMessagesAsResource("growl.success", "growl.patient.merge.success");
					hideDialog(new PatientMergeEvent(patient, patientToMerge));
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

	public void onChangeTasksToMergeSelection() {
		deletePatientDisabled = patient.getTasks().size() != tasksTomerge.size();
System.out.println(deletePatientDisabled);
		if (deletePatientDisabled)
			deletePatient = false;
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

	/**
	 * Option for determine the merge target.
	 * 
	 * @author andi
	 *
	 */
	public enum MergeOption {
		PIZ, PATIENT;
	}
}