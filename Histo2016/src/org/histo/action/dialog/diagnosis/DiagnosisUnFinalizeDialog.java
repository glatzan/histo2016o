package org.histo.action.dialog.diagnosis;

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
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


@Configurable
@Getter
@Setter
public class DiagnosisUnFinalizeDialog extends AbstractDialog {
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	public void initAndPrepareBean(Task task) {
		initBean(task, Dialog.DIAGNOSIS_PHASE_UN_FINALIZE);
		prepareDialog();
	}

	public void initBean(Task task, Dialog dialog) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, dialog);
	}

	public void unFinalize() {
		try {
			
			// adding to notification phase
			favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.DiagnosisList);
			
			// finalizing task
			getTask().setFinalizationDate(0);
			getTask().setFinalized(false);
			
			patientDao.save(getTask(), "log.patient.task.change.diagnosisPhase.unFinalized",
					new Object[] { getTask() });
			
			
		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}
}
