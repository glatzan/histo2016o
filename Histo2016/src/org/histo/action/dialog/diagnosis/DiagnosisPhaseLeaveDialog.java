package org.histo.action.dialog.diagnosis;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.PrintDialogHandler;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DiagnosisPhaseLeaveDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private PrintDialogHandler printDialogHandler;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private PatientDao patientDao;

	/**
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInDiagnosisPhase;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}
		super.initBean(task, Dialog.DIAGNOSIS_PHASE_LEAVE);

		setStayInDiagnosisPhase(false);

		// inits a template for previewing
		printDialogHandler.initBeanForExternalDisplay(task,
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN },
				DocumentType.DIAGNOSIS_REPORT_EXTERN);

		return true;
	}

	public void endDiagnosisPhase() {
		try {
			taskManipulationHandler
					.finalizeAllDiangosisRevisions(getTask().getDiagnosisContainer().getDiagnosisRevisions(), true);

			// adding to notification phase
			favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.NotificationList);

			// removing from diagnosis list
			if (getTask().isListedInFavouriteList(PredefinedFavouriteList.DiagnosisList))
				favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.DiagnosisList);

			// removing from REdiagnosis list
			if (getTask().isListedInFavouriteList(PredefinedFavouriteList.ReDiagnosisList))
				favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.ReDiagnosisList);

			// adding to stay in diagnosis phase if selected
			if (isStayInDiagnosisPhase()
					&& !getTask().isListedInFavouriteList(PredefinedFavouriteList.StayInDiagnosisList))
				favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);
			else if (getTask().isListedInFavouriteList(PredefinedFavouriteList.StayInDiagnosisList))
				// removing from stay in diagnosis list
				favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);

			getTask().setDiagnosisCompletionDate(System.currentTimeMillis());

			patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.task.change.diagnosisPhase.end");

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}

	}

	// ************************ Getter/Setter ************************
	public boolean isStayInDiagnosisPhase() {
		return stayInDiagnosisPhase;
	}

	public void setStayInDiagnosisPhase(boolean stayInDiagnosisPhase) {
		this.stayInDiagnosisPhase = stayInDiagnosisPhase;
	}
}
