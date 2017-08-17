package org.histo.action.dialog.patient;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

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

	}
}
