package org.histo.action.dialog.patient;

import java.util.List;

import org.hibernate.LockMode;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.BioBankDAO;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.BioBank;
import org.histo.model.FavouriteList;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
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

			taskDAO.deleteTask(getTask());
			
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}

		// boolean returnValue = transactionTemplate.execute(session -> {
		// session.buildLockRequest(new
		// LockOptions(LockMode.OPTIMISTIC_FORCE_INCREMENT)).lock(getTask().getParent());
		// Commit commit = new Commit(repository);
		// commit.getChanges().add(new Change("README.txt", "0a1,5..."));
		// commit.getChanges().add(new Change("web.xml", "17c17..."));
		// session.persist(commit);
		//
		// return true;
		// });

		// // removing task from patient
		// .getTasks().remove(getTask());patientDao.deletePatientAssociatedDataFailSave(getTask(),"log.patient.task.remove",getTask());}
	}

}
