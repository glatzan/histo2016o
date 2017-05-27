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
public class StainingPhaseForceDialogHandler extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	public void initAndPrepareBeanForEnter(Task task) {
		initBean(task, Dialog.STAINING_PHASE_FORCE_ENTER);
		prepareDialog();
	}

	public void initAndPrepareBeanForLeave(Task task) {
		initBean(task, Dialog.STAINING_PHASE_FORCE_LEAVE);
		prepareDialog();
	}

	public void initBean(Task task, Dialog dialog) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistHandlerAction.updatePatientInCurrentWorklist(task.getPatient());
		}

		super.initBean(task, dialog);
	}

	public void leaveStayInStainingPhase() {
		try {
			favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.StayInStainingList);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void enterStayInStainingPhase() {
		try {
			favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInStainingList);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
