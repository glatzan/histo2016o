package org.histo.action.dialog.diagnosis;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
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
public class DiagnosisPhaseForceDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private PatientDao patientDao;

	public void initAndPrepareBeanForEnter(Task task) {
		initBean(task, Dialog.DIAGNOSIS_PHASE_FORCE_ENTER);
		prepareDialog();
	}

	public void initAndPrepareBeanForLeave(Task task) {
		initBean(task, Dialog.DIAGNOSIS_PHASE_FORCE_LEAVE);
		prepareDialog();
	}

	public void initBean(Task task, Dialog dialog) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, dialog);
	}

	public void leaveStayInStainingPhase() {
		try {
			favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.StayInDiagnosisList);
			
			if(getTask().getDiagnosisCompletionDate() == 0){
				getTask().setDiagnosisCompletionDate(System.currentTimeMillis());
				patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.task.change.diagnosisPhase.end");
			}
			
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void enterStayInStainingPhase() {
		try {
			// all is editable so normal diagnosis list
			favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.DiagnosisList);
			
			// unfinalizes all diangoses
			taskManipulationHandler.finalizeAllDiangosisRevisions(
					getTask().getDiagnosisContainer().getDiagnosisRevisions(), false);

			getTask().setDiagnosisCompletionDate(0);
			patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.task.change.diagnosisPhase.reentered");
			
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
