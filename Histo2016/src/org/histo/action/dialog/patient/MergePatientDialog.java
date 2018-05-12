package org.histo.action.dialog.patient;

import java.util.List;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.service.PatientService;
import org.histo.service.dao.impl.PatientDaoImpl;
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

	/**
	 * This can be set to true if piz search was performed and no patient was found.
	 */
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

	/**
	 * Method is called when the merge option (piz vs patient) is changed.
	 */
	public void onChangeMergeOption() {
		renderErrorPatientNotFound = false;
		patientToMerge = null;
		onChangeTasksToMergeSelection();

		if (mergeOption == MergeOption.PIZ)
			onSelectPatientViaPiz();
	}

	/**
	 * Searches for a patient via piz if the piz is 8 digits long.
	 */
	public void onSelectPatientViaPiz() {
		// only search if 8 dignis are present
		if (HistoUtil.isNotNullOrEmpty(piz) && piz.matches("^\\d{8}$")) {
			try {
				patientToMerge = patientService.findPatientByPiz(piz, false);
			} catch (HistoDatabaseInconsistentVersionException | JSONException | CustomExceptionToManyEntries
					| CustomNullPatientExcepetion e) {
			} finally {
				if (patientToMerge == null)
					renderErrorPatientNotFound = true;
				else
					renderErrorPatientNotFound = false;
			}
		}
	}

	/**
	 * If merging was confirmed this method will merge the two patients
	 * 
	 * @param event
	 */
	public void onMergePatient(SelectEvent event) {
		try {
			if (event.getObject() != null && event.getObject() instanceof Boolean
					&& ((Boolean) event.getObject()).booleanValue()) {
				if (patient != null && patientToMerge != null) {

					if (patientToMerge.getId() == 0)
						patientService.addPatient(patientToMerge, false);

					patientService.mergePatient(patient, patientToMerge, tasksTomerge);

					if (deletePatient && patient.getTasks().isEmpty())
						patientService.archivePatient(patient);

					mainHandlerAction.sendGrowlMessagesAsResource("growl.success", "growl.patient.merge.success");
					hideDialog(new PatientMergeEvent(patient, patientToMerge));
				}
			}
		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Is called on return of the patient select dialog, if a patient is return it
	 * will set as the patientToMerge
	 * 
	 * @param event
	 */
	public void onSelectPatient(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof Patient) {
			patientToMerge = (Patient) event.getObject();
		}
	}

	/**
	 * Is called if the task selection of the source task was altered. Deletion is
	 * only possible if all tasks are selected.
	 */
	public void onChangeTasksToMergeSelection() {
		deletePatientDisabled = patient.getTasks().size() != tasksTomerge.size();

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