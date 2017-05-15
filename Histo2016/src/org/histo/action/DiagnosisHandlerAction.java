package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.dialog.PrintDialogHandler;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.ui.RevisionHolder;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DiagnosisHandlerAction implements Serializable {

	private static final long serialVersionUID = -1214161114824263589L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private TaskHandlerAction taskHandlerAction;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private PrintDialogHandler printDialogHandler;

	private Task temporaryTask;

	private Diagnosis tmpDiagnosis;

	/**
	 * List containing all old revisions and a new revision. The string contains
	 * the proposed new name
	 */
	private List<RevisionHolder> newRevisionList;

	/**
	 * Type of the new revision
	 */
	private DiagnosisRevisionType newRevisionType;

	/**
	 * Types of all available revisionTypes to create
	 */
	private DiagnosisRevisionType[] selectableRevisionTypes;

	/**
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInDiagnosisPhase;

	/**
	 * Hides dialogs associated with the slideHandlerAction, resets all
	 * variables
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		setTmpDiagnosis(null);
		setTemporaryTask(null);
		mainHandlerAction.hideDialog(dialog);
	}

	/********************************************************
	 * Diagnosis Gui
	 ********************************************************/
	public void prepareDiagnosisRevisionDialog(Task task) {
		setTemporaryTask(task);

		DiagnosisRevisionType[] types = new DiagnosisRevisionType[3];
		types[0] = DiagnosisRevisionType.DIAGNOSIS_REVISION;
		types[1] = DiagnosisRevisionType.DIAGNOSIS_CORRECTION;
		types[2] = DiagnosisRevisionType.DIAGNOSIS_COUNCIL;

		setSelectableRevisionTypes(types);
		setNewRevisionType(types[0]);

		updateDiagnosisRevisionType();
		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_REVISION_CREATE);
	}

	public void updateDiagnosisRevisionType() {
		setNewRevisionList(new ArrayList<RevisionHolder>());

		List<DiagnosisRevision> newList = new ArrayList<DiagnosisRevision>(
				getTemporaryTask().getDiagnosisContainer().getDiagnosisRevisions());
		newList.add(new DiagnosisRevision(getTemporaryTask().getDiagnosisContainer(), getNewRevisionType()));

		for (DiagnosisRevision revision : newList) {
			getNewRevisionList().add(
					new RevisionHolder(revision, TaskUtil.getDiagnosisRevisionName(newList, revision, resourceBundle)));
		}
	}

	public void copyOldNameFromDiagnosisRevision(DiagnosisRevision diagnosisRevision) {
		for (RevisionHolder revisionHolder : newRevisionList) {
			if (revisionHolder.getRevision() == diagnosisRevision)
				revisionHolder.setName(diagnosisRevision.getName());
		}
	}

	public void createDiagnosisRevisionFromGui() {

		for (RevisionHolder revisionHolder : getNewRevisionList()) {
			if (!revisionHolder.getName().equals(revisionHolder.getRevision().getName())) {
				logger.debug("Updating revision name from " + revisionHolder.getRevision().getName() + " to "
						+ revisionHolder.getName());
				// updating name
				revisionHolder.getRevision().setName(revisionHolder.getName());

				// new revision
				if (revisionHolder.getRevision().getId() == 0) {
					taskManipulationHandler.addDiagnosisRevision(revisionHolder.getRevision().getParent(),
							revisionHolder.getRevision());
				} else {
					// update revision
					mainHandlerAction.saveDataChange(revisionHolder.getRevision(),
							"log.patient.task.diagnosisContainer.diagnosisRevision.update",
							revisionHolder.getRevision().getName());
				}
			}
		}

		// saving task to database
		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.update", getTemporaryTask().getTaskID());
		hideDiagnosisRevisionDialog();
	}

	public void hideDiagnosisRevisionDialog() {
		setNewRevisionList(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_REVISION_CREATE);
	}

	/********************************************************
	 * Diagnosis Gui
	 ********************************************************/

	/********************************************************
	 * Default Dialog for ending diagnosis phase
	 ********************************************************/
	public void showDiagnosisPhaseEndDialog(Task task) {
		setTemporaryTask(task);

		setStayInDiagnosisPhase(false);

		// inits a template for previewing
		printDialogHandler.initBeanForExternalDisplay(task,
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN },
				DocumentType.DIAGNOSIS_REPORT_EXTERN);

		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_PHASE_LEAVE);
	}

	public void hideDiagnosisPhaseEndDialog(boolean stayInPhase) {
		taskManipulationHandler.finalizeAllDiangosisRevisions(
				getTemporaryTask().getDiagnosisContainer().getDiagnosisRevisions(), true);
		getTemporaryTask().setNotificationPhase(true);
		getTemporaryTask().setDiagnosisCompletionDate(System.currentTimeMillis());
		getTemporaryTask().setDiagnosisPhase(stayInPhase);

		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.diagnosisPhase.end");
		hideDialog(Dialog.DIAGNOSIS_PHASE_LEAVE);
	}

	/********************************************************
	 * Default Dialog for ending diagnosis phase
	 ********************************************************/

	/********************************************************
	 * Dialog for forcing stay and leave of diagnosis phase
	 ********************************************************/
	/**
	 * Show a dialog for forcing leave or stay in diagnosis phase.
	 * 
	 * @param task
	 * @param stay
	 */
	public void showDiagnosisForcePhaseDialog(Task task, boolean force) {
		setTemporaryTask(task);

		if (force)
			mainHandlerAction.showDialog(Dialog.DIAGNOSIS_PHASE_FORCE_ENTER);
		else
			mainHandlerAction.showDialog(Dialog.DIAGNOSIS_PHASE_FORCE_LEAVE);
	}

	/**
	 * Sets the diagnosis phase to false and finalizes all diagnoses
	 */
	public void forceLeaveDiagnosisPhaseAndHideDialog() {
		// set to notification phase if task is not finalized an no other phase
		// is active
		if (!getTemporaryTask().isFinalized() && !getTemporaryTask().getStatus().isDiagnosisPhaseAndOtherPhase()) {
			logger.debug("Setting notification phase to true");
			getTemporaryTask().setNotificationPhase(true);
		}

		getTemporaryTask().setDiagnosisPhase(false);

		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.diagnosisPhase.end");

		hideDialog(Dialog.DIAGNOSIS_PHASE_FORCE_LEAVE);
	}

	/**
	 * Sets the diagnosis phase to true and unfinalizes all diagnoses
	 */
	public void forceEnterDiagnosisPhaseAndHideDialog() {
		getTemporaryTask().setDiagnosisPhase(true);

		logger.debug("Unlocking all diangoses");
		taskManipulationHandler.finalizeAllDiangosisRevisions(
				getTemporaryTask().getDiagnosisContainer().getDiagnosisRevisions(), false);

		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.diagnosisPhase.reentered");
		hideDialog(Dialog.DIAGNOSIS_PHASE_FORCE_ENTER);
	}

	/********************************************************
	 * Dialog for forcing stay and leave of diagnosis phase
	 ********************************************************/

	public void onDiagnosisPrototypeChanged(Diagnosis diagnosis) {
		logger.debug("Updating diagnosis prototype");
		Task task = diagnosis.getParent().getParent().getParent();

		diagnosis.updateDiagnosisWithPrest(diagnosis.getDiagnosisPrototype());

		mainHandlerAction.saveDataChange(diagnosis, "log.patient.task.diagnosisContainer.diagnosis.update",
				diagnosis.getName());

		// only setting diagnosis text if one sample and no text has been added
		// jet
		if (diagnosis.getParent().getText() == null || diagnosis.getParent().getText().isEmpty()) {
			diagnosis.getParent().setText(diagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());
			logger.debug("Updating revision extended text");
			mainHandlerAction.saveDataChange(diagnosis.getParent(),
					"log.patient.task.diagnosisContainer.diagnosisRevision.update", diagnosis.getParent().getName());
		}

	}

	/**
	 * Overwrites the extended text of the diagnosisRevison if no text is
	 * present. If text was entered, a dialog will be shown.
	 * 
	 * @param tmpDiagnosis
	 */
	public void prepareCopyHistologicalRecordDialog(Diagnosis tmpDiagnosis) {

		setTmpDiagnosis(tmpDiagnosis);

		// setting diagnosistext if no text is set
		if ((tmpDiagnosis.getParent().getText() == null || tmpDiagnosis.getParent().getText().isEmpty())
				&& tmpDiagnosis.getDiagnosisPrototype() != null) {
			copyHistologicalRecord(tmpDiagnosis, true);
			logger.debug("No extended diagnosistext found, text copied");
			return;
		}

		if (tmpDiagnosis.getDiagnosisPrototype() != null) {
			logger.debug("Extended diagnosistext found, showing confing dialog");
			mainHandlerAction.showDialog(Dialog.DIAGNOSIS_RECORD_OVERWRITE);
		}

	}

	/**
	 * Will overwrite the diangosisrevision text if overwrite ist true,
	 * ostherwise the text of the diagnosis will be added
	 * 
	 * @param tmpDiagnosis
	 * @param overwrite
	 */
	public void copyHistologicalRecord(Diagnosis tmpDiagnosis, boolean overwrite) {
		logger.debug("Setting extended diagnosistext text");
		tmpDiagnosis.getParent()
				.setText(overwrite ? tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText()
						: tmpDiagnosis.getParent().getText() + "\r\n"
								+ tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());

		mainHandlerAction.saveDataChange(tmpDiagnosis.getParent(),
				"log.patient.task.diagnosisContainer.diagnosisRevision.update", tmpDiagnosis.getParent().getName());

		hideCopyHistologicalRecordDialog();
	}

	/**
	 * Hides the overwrite warning dialog
	 */
	public void hideCopyHistologicalRecordDialog() {
		setTmpDiagnosis(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_RECORD_OVERWRITE);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public Task getTemporaryTask() {
		return temporaryTask;
	}

	public void setTemporaryTask(Task temporaryTask) {
		this.temporaryTask = temporaryTask;
	}

	public Diagnosis getTmpDiagnosis() {
		return tmpDiagnosis;
	}

	public void setTmpDiagnosis(Diagnosis tmpDiagnosis) {
		this.tmpDiagnosis = tmpDiagnosis;
	}

	public List<RevisionHolder> getNewRevisionList() {
		return newRevisionList;
	}

	public void setNewRevisionList(List<RevisionHolder> newRevisionList) {
		this.newRevisionList = newRevisionList;
	}

	public DiagnosisRevisionType getNewRevisionType() {
		return newRevisionType;
	}

	public void setNewRevisionType(DiagnosisRevisionType newRevisionType) {
		this.newRevisionType = newRevisionType;
	}

	public DiagnosisRevisionType[] getSelectableRevisionTypes() {
		return selectableRevisionTypes;
	}

	public void setSelectableRevisionTypes(DiagnosisRevisionType[] selectableRevisionTypes) {
		this.selectableRevisionTypes = selectableRevisionTypes;
	}

	public boolean isStayInDiagnosisPhase() {
		return stayInDiagnosisPhase;
	}

	public void setStayInDiagnosisPhase(boolean stayInDiagnosisPhase) {
		this.stayInDiagnosisPhase = stayInDiagnosisPhase;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
