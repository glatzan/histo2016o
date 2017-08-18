package org.histo.action.dialog.slide;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.ui.ListChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class AddSlidesDialogHandler extends AbstractDialog {

	@Autowired
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	private Block block;

	private String commentary;

	private boolean restaining;

	private List<ListChooser<StainingPrototype>> stainingListChooser;

	/**
	 * Initializes the bean and shows the dialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Block block) {
		if (initBean(block))
			prepareDialog();
	}

	/**
	 * Initializes all field of the object
	 * 
	 * @param task
	 */
	public boolean initBean(Block block) {
		super.initBean(null, Dialog.SLIDE_CREATE);
		try {
			setBlock(genericDAO.refresh(block));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(block.getTask().getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
			return false;
		}

		setCommentary("");
		setRestaining(taskStatusHandler.isReStainingFlag(block.getParent()));

		setStainingListChooser(new ArrayList<ListChooser<StainingPrototype>>());

		List<StainingPrototype> allStainings = utilDAO.getAllStainingPrototypes();

		getStainingListChooser().addAll(
				allStainings.stream().map(p -> new ListChooser<StainingPrototype>(p)).collect(Collectors.toList()));

		return true;
	}

	public void addSlides() {
		try {
			// checks if anything is selected, otherwise the dialog will be
			// closed
			boolean slideChoosen = false;
			for (ListChooser<StainingPrototype> slide : getStainingListChooser()) {
				if (slide.isChoosen()) {
					slideChoosen = true;
					break;
				}
			}

			if (!slideChoosen) {
				return;
			}

			// if chosen a new slide will be created
			for (ListChooser<StainingPrototype> slide : getStainingListChooser()) {
				if (slide.isChoosen()) {
					taskManipulationHandler.createSlide(slide.getListItem(), getBlock(), getCommentary(),
							isRestaining());
				}
			}

			// checking if the object need to be added to the staining list
			// again
			// something was added, so a change occured

			receiptlogViewHandlerAction.checkStainingPhase(block.getTask(), true);

			// updating statining list
			block.getTask().generateSlideGuiList();

			genericDAO.savePatientData(getBlock(), "log.patient.task.sample.block.update",
					block.toString());
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	public boolean isRestaining() {
		return restaining;
	}

	public void setRestaining(boolean restaining) {
		this.restaining = restaining;
	}

	public List<ListChooser<StainingPrototype>> getStainingListChooser() {
		return stainingListChooser;
	}

	public void setStainingListChooser(List<ListChooser<StainingPrototype>> stainingListChooser) {
		this.stainingListChooser = stainingListChooser;
	}

}
