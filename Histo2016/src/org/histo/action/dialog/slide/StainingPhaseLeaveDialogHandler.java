package org.histo.action.dialog.slide;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.FavouriteList;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class StainingPhaseLeaveDialogHandler extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

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
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistHandlerAction.updatePatientInCurrentWorklist(task.getPatient());
		}

		super.initBean(task, Dialog.STAINING_PHASE_LEAVE);
	}

	public void hideDialog(boolean stayInPhase) {
		try {
			// if stay in Phase is true, the task will be shifted into the stay
			// in staing list
			if (stayInPhase) {
				logger.debug("Task should stay in staining phase");
				favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInStainingList);
			}
			
			super.hideDialog();
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
