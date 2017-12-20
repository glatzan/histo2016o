package org.histo.action.dialog.slide;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.action.view.GlobalEditViewHandler;
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
import org.histo.ui.StainingTableChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class AddSlidesDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

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
	private GlobalEditViewHandler globalEditViewHandler;
	
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
			setBlock(genericDAO.reattach(block));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(block.getTask().getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
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

			genericDAO.savePatientData(getBlock().getTask(), "log.patient.task.sample.block.update",
					block.toString());
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
		
		globalEditViewHandler.updateDataOfTask(false);
	}
}
