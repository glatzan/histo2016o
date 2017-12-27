package org.histo.action.dialog.slide;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.TaskHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class StainingPhaseExitDialog extends AbstractDialog {

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
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	/**
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInStainingPhase;

	/**
	 * If true the task will be shifted into diagnosis phase
	 */
	private boolean goToDiagnosisPhase;

	/**
	 * Initializes the bean and shows the dialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Task task) {
		initBean(task);
		prepareDialog();
	}

	/**
	 * Initializes all field of the object
	 * 
	 * @param task
	 */
	public void initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);

			if (task.isListedInFavouriteList(PredefinedFavouriteList.NotificationList))
				this.goToDiagnosisPhase = false;
			else
				this.goToDiagnosisPhase = true;

			stayInStainingPhase = false;

		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.STAINING_PHASE_EXIT);
	}

	public void exitPhase() {
		// ending staining pahse
		sampleService.endStainingPhase(task);

		if (goToDiagnosisPhase) {
			logger.debug("Adding Task to diagnosis list");
			favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.DiagnosisList);
		}

		if (stayInStainingPhase) {
			logger.debug("Task should stay in staining phase");
			favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInStainingList);
		}

		globalEditViewHandler.updateDataOfTask(true, false, true, true);
	}
}
