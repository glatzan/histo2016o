package org.histo.action.dialog.slide;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.SlideManipulationHandler;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
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
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SlideManipulationHandler slideManipulationHandler;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	/**
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInStainingPhase;

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
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.STAINING_PHASE_EXIT);
	}

	public void exitPhase() {
		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					genericDAO.reattach(task.getPatient());
					
					boolean changed = slideManipulationHandler.setStainingCompletedForAllSlides(task, true);

					task.setStainingCompletionDate(System.currentTimeMillis());

					// removing from staining or restaing list
					favouriteListDAO.removeTaskFromList(task, new PredefinedFavouriteList[] {
							PredefinedFavouriteList.StainingList, PredefinedFavouriteList.ReStainingList });

					favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.DiagnosisList);

					if (stayInStainingPhase) {
						logger.debug("Task should stay in staining phase");
						favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StayInStainingList);
					}

					genericDAO.savePatientData(task, "log.patient.task.change.stainingPhase.end");
				}
			});
		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
		
		globalEditViewHandler.updateTaskMenuModel(false);
	}
}
