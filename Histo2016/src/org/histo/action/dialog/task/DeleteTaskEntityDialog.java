package org.histo.action.dialog.task;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class DeleteTaskEntityDialog extends AbstractDialog {

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
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;
	
	/**
	 * Temporary save for a task tree entity (sample, slide, block)
	 */
	private DeleteAble toDelete;

	/**
	 * True if the staining phase has ended after deleting an entity
	 */
	private boolean StainingPhaseEnded;

	public void initAndPrepareBean(Task task, DeleteAble deleteAble) {
		if (initBean(task, deleteAble))
			prepareDialog();
	}

	public boolean initBean(Task task, DeleteAble deleteAble) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
			return false;
		}
		super.initBean(task, Dialog.DELETE_TREE_ENTITY);
		setToDelete(deleteAble);

		return true;
	}

	/**
	 * Deletes samples, slides and blocks
	 */
	public void deleteTaskEntity() {
		try {
			Parent<?> p = null;

			if (toDelete instanceof Slide) {
				setStainingPhaseEnded(sampleService.deleteSlide((Slide) (p = (Slide) toDelete)));
			} else if (toDelete instanceof Block) {
				setStainingPhaseEnded(sampleService.deleteBlock((Block) (p = (Block) toDelete)));
			} else if (toDelete instanceof Sample) {
				setStainingPhaseEnded(sampleService.deleteSample((Sample) (p = (Sample) toDelete)));
			} else {
				return;
			}

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Custom close handler, if staining phase has ended return true
	 */
	public void hideDialog() {
		super.hideDialog(new Boolean(isStainingPhaseEnded()));
	}
}
