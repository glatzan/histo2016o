package org.histo.action.dialog.task;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DeleteTaskEntityDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	/**
	 * Temporary save for a task tree entity (sample, slide, block)
	 */
	private DeleteAble toDelete;

	public void initAndPrepareBean(Task task, DeleteAble deleteAble) {
		if (initBean(task,deleteAble))
			prepareDialog();
	}

	public boolean initBean(Task task,DeleteAble deleteAble) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistHandlerAction.updatePatientInCurrentWorklist(task.getPatient());
			return false;
		}
		super.initBean(task, Dialog.DELETE_TREE_ENTITY);
		setToDelete(deleteAble);
		
		return true;
	}

	public void deleteTaskEntity() {
		try {
			if (toDelete instanceof Slide) {
				Slide toDeleteSlide = (Slide) toDelete;

				logger.info("Deleting slide " + toDeleteSlide.getSlideID());

				Block parent = toDeleteSlide.getParent();

				parent.getSlides().remove(toDeleteSlide);

				parent.updateAllNames(parent.getParent().getParent().isUseAutoNomenclature());

				patientDao.savePatientAssociatedDataFailSave(parent, "log.patient.task.sample.block.update",
						parent.toString());

				patientDao.deletePatientAssociatedDataFailSave(toDeleteSlide,
						"log.patient.task.sample.block.slide.delete", toDeleteSlide.toString());

				// checking if staining flag of the task object has to be false
				receiptlogViewHandlerAction.checkStainingPhase(parent.getParent().getParent(), true);
				// generating gui list
				parent.getParent().getParent().generateSlideGuiList();

			} else if (toDelete instanceof Block) {
				Block toDeleteBlock = (Block) toDelete;
				logger.info("Deleting block " + toDeleteBlock.getBlockID());

				Sample parent = toDeleteBlock.getParent();

				parent.getBlocks().remove(toDeleteBlock);

				parent.updateAllNames(parent.getParent().isUseAutoNomenclature());

				patientDao.savePatientAssociatedDataFailSave(parent, "log.patient.task.sample.update",
						parent.toString());

				patientDao.deletePatientAssociatedDataFailSave(toDeleteBlock, "log.patient.task.sample.block.delete",
						toDeleteBlock.toString());

				// checking if staining flag of the task object has to be false
				receiptlogViewHandlerAction.checkStainingPhase(parent.getParent(), true);
				// generating gui list
				parent.getParent().generateSlideGuiList();

			} else if (toDelete instanceof Sample) {
				Sample toDeleteSample = (Sample) toDelete;
				logger.info("Deleting sample " + toDeleteSample.getSampleID());

				Task parent = toDeleteSample.getParent();

				parent.getSamples().remove(toDeleteSample);

				taskManipulationHandler.updateDiagnosisContainerToSampleCount(parent.getDiagnosisContainer(),
						parent.getSamples());

				parent.updateAllNames();

				patientDao.savePatientAssociatedDataFailSave(parent, "log.patient.task.update", parent.toString());

				patientDao.deletePatientAssociatedDataFailSave(toDeleteSample, "log.patient.task.sample.delete",
						toDeleteSample.toString());

				// checking if staining flag of the task object has to be false
				receiptlogViewHandlerAction.checkStainingPhase(parent, true);
				// generating gui list
				parent.generateSlideGuiList();
			}

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public DeleteAble getToDelete() {
		return toDelete;
	}

	public void setToDelete(DeleteAble toDelete) {
		this.toDelete = toDelete;
	}

}
