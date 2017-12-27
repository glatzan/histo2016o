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
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.service.SampleService;
import org.histo.ui.ListChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CreateSlidesDialog extends AbstractDialog {

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
	private SampleService sampleService;

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
			// getting all slides to add
			List<StainingPrototype> slidesToAdd = stainingListChooser.stream().filter(p -> p.isChoosen())
					.map(p -> p.getListItem()).collect(Collectors.toList());
			if (!slidesToAdd.isEmpty()) {
				sampleService.createSlidesForSample(slidesToAdd, block, commentary, restaining);
				// updating statining list
			}

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}

		globalEditViewHandler.updateDataOfTask(true, false, true, true);
	}

	public boolean isStainingSelected() {
		return stainingListChooser.stream().anyMatch(p -> p.isChoosen());
	}
}
