package org.histo.action.dialog.patient;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.BioBankDAO;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class DeleteTaskDialog extends AbstractDialog {

	public static final int maxRevisionToDelete = 3;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

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
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private BioBankDAO bioBankDAO;

	private boolean deleteAble;

	public void initAndPrepareBean(Task task) {
		initBean(task);
		prepareDialog();
	}

	public void initBean(Task task) {
		super.initBean(task, Dialog.TASK_DELETE, false);
		setDeleteAble(!taskWasAltered());
	}

	public boolean taskWasAltered() {
		List<Task> revisions = taskDAO.getTasksRevisions(task.getId());

		logger.debug(revisions.size() + " Revsions available");

		if (revisions.size() > maxRevisionToDelete) {
			return true;
		}

		return false;
	}

	public void deleteTask() {
		try {

			if (!(transactionTemplate.execute(new TransactionCallback<Boolean>() {

				public Boolean doInTransaction(TransactionStatus transactionStatus) {
					try {
						genericDAO.refresh(getTask());
						genericDAO.refresh(getTask().getPatient());
						favouriteListDAO.removeTaskFromAllLists(getTask());
						bioBankDAO.removeAssociatedBioBank(getTask());
						genericDAO.deletePatientData(task, "log.patient.task.remove", task.toString());
						task.getPatient().getTasks().remove(task);
						return new Boolean(true);
					} catch (Exception e) {
						return new Boolean(false);
					}
				}

			})).booleanValue()) {
				throw new CustomDatabaseInconsistentVersionException(getTask());
			}

			taskDAO.lock(getTask().getParent());

		} catch (Exception e) {
			System.out.println("0");
			onDatabaseVersionConflict();
		}
	}

	public void onDatabaseVersionConflict() {
		worklistViewHandlerAction.replacePatientInCurrentWorklist(getTask().getParent().getId());
		super.onDatabaseVersionConflict();
	}

}
