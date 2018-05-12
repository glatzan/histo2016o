package org.histo.action.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.diagnosis.AddDiangosisReviosionDialog;
import org.histo.action.dialog.slide.CreateSlidesDialog.SlideSelectResult;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.StainingListAction;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.SlideLable;
import org.histo.ui.StainingTableChooser;
import org.histo.ui.task.TaskStatus;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sun.net.www.content.image.gif;

@Controller
@Scope("session")
@Getter
@Setter
public class ReceiptlogViewHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private AddDiangosisReviosionDialog addDiangosisReviosionDialog;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private GlobalEditViewHandler globalEditViewHandler;
	
	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	@Transient
	private ArrayList<StainingTableChooser<?>> stainingTableRows;

	/**
	 * This variable is used to save the selected action, which should be executed
	 * upon all selected slides
	 */
	private StainingListAction actionOnMany;

	/**
	 * Is used for selecting a chooser from the generated list (generated by task).
	 * It is used to edit the names of the entities by an overlaypannel
	 */
	private StainingTableChooser<?> selectedStainingTableChooser;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReceiptlogViewHandlerAction for task");
		// generating guilist for display
		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Updates the flat task/sample/block/slide list
	 * 
	 * @param task
	 * @param showArchived
	 */
	public void updateSlideGuiList(Task task, boolean showArchived) {
		setStainingTableRows(StainingTableChooser.factory(task, showArchived));
	}

	/**
	 * Executes an action on all selected slides
	 * 
	 * @param task
	 */
	public void performActionOnManyTaskChildren(Task task) {
		performActionOnManyTaskChildren(task, getActionOnMany());
	}

	/**
	 * Executes an action on all selected slides
	 * 
	 * @param list
	 * @param action
	 */
	public void performActionOnManyTaskChildren(Task task, StainingListAction action) {
		try {
			List<StainingTableChooser<?>> list = getStainingTableRows();

			// at least one thing has to bee selected
			boolean atLeastOnechoosen = false;
			for (StainingTableChooser<?> stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen()) {
					atLeastOnechoosen = true;
					break;
				}
			}

			if (!atLeastOnechoosen) {
				logger.debug("Nothing selected, do not performe any action");
				return;
			}

			switch (getActionOnMany()) {
			case PERFORMED:
				logger.debug("Setting staining status of selected slides to perforemd!");

				sampleService
						.setStainingCompletedForSlides(list.stream().filter(p -> p.isChoosen() && p.isStainingType())
								.map(p -> (Slide) p.getEntity()).collect(Collectors.toList()), true);

				// shows dialog for informing the user that all stainings are
				// performed
				if (sampleService.updateStaingPhase(task))
					dialogHandlerAction.getStainingPhaseExitDialog().initAndPrepareBean(task);

				break;
			case NOT_PERFORMED:
				logger.debug("Setting staining status of selected slides to not perforemd!");

				sampleService
						.setStainingCompletedForSlides(list.stream().filter(p -> p.isChoosen() && p.isStainingType())
								.map(p -> (Slide) p.getEntity()).collect(Collectors.toList()), false);

				if (sampleService.updateStaingPhase(task))
					dialogHandlerAction.getStainingPhaseExitDialog().initAndPrepareBean(task);

				break;
			case ARCHIVE:
				// TODO implement
				System.out.println("To impliment");
				break;
			case PRINT:

				SlideLable slideLabel = DocumentTemplate
						.getTemplateByID(globalSettings.getDefaultDocuments().getSlideLabelDocument());

				if (slideLabel == null) {
					logger.debug("No template found for printing, returning!");
					return;
				}

				logger.debug("Printing labes for selected slides");

				List<SlideLable> toPrint = new ArrayList<SlideLable>();

				for (StainingTableChooser<?> stainingTableChooser : list) {
					if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {

						Slide slide = (Slide) stainingTableChooser.getEntity();

						SlideLable tmp = (SlideLable) slideLabel.clone();
						tmp.initData(task, slide, new Date(System.currentTimeMillis()));
						tmp.fillTemplate();
						toPrint.add(tmp);
					}
				}

				if (toPrint.size() != 0) {
					try {
						userHandlerAction.getSelectedLabelPrinter().print(toPrint);
					} catch (CustomUserNotificationExcepetion e) {
						// handling offline error
						mainHandlerAction.sendGrowlMessages(e);
					}
				}

				break;
			default:
				break;
			}
		} catch (HistoDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replaceSelectedTask();
		}

		setActionOnMany(StainingListAction.NONE);

	}

	/**
	 * Toggles the status of a StainingTableChooser object and all chides.
	 * 
	 * @param chooser
	 */
	public void toggleChildrenChoosenFlag(StainingTableChooser<?> chooser) {
		setChildrenAsChoosen(chooser, !chooser.isChoosen());
	}

	/**
	 * Sets all children of a StainingTableChoosers to chosen/unchosen
	 * 
	 * @param chooser
	 * @param choosen
	 */
	public void setChildrenAsChoosen(StainingTableChooser<?> chooser, boolean chosen) {
		chooser.setChoosen(chosen);
		if (chooser.isSampleType() || chooser.isBlockType()) {
			for (StainingTableChooser<?> tmp : chooser.getChildren()) {
				setChildrenAsChoosen(tmp, chosen);
			}
		}

		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Sets a lists of StainingTableChoosers to chosen/unchosen Setzt den Status
	 * einer Liste von StainingTableChoosers und ihrer Kinder
	 * 
	 * @param choosers
	 * @param choosen
	 */
	public void setListAsChoosen(List<StainingTableChooser<?>> choosers, boolean chosen) {
		for (StainingTableChooser<?> chooser : choosers) {
			if (chooser.isSampleType()) {
				setChildrenAsChoosen(chooser, chosen);
			}
		}
	}

	/**
	 * Prints a lable for the choosen slide.
	 * 
	 * @param slide
	 */
	public void printLableForSlide(Slide slide) {

		SlideLable slideLabel = DocumentTemplate
				.getTemplateByID(globalSettings.getDefaultDocuments().getSlideLabelDocument());

		if (slideLabel == null) {
			logger.debug("No template found for printing, returning!");
			return;
		}

		slideLabel.initData(slide.getTask(), slide, new Date(System.currentTimeMillis()));
		slideLabel.fillTemplate();

		userHandlerAction.getSelectedLabelPrinter().print(slideLabel);

	}

	/**
	 * Sets a slide as stating status completed
	 * 
	 * @param slide
	 */
	public void setSlideAsCompleted(Slide slide) {
		try {
			sampleService.setStainingCompletedForSlide(slide, !slide.isStainingCompleted());

			if (sampleService.updateStaingPhase(slide.getTask()))
				dialogHandlerAction.getStainingPhaseExitDialog().initAndPrepareBean(slide.getTask());

		} catch (HistoDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replaceSelectedTask();
		}
	}

	/**
	 * Saves the manually altered flag, if the sample/block/ or slide id was
	 * manually altered.
	 * 
	 * @param idManuallyAltered
	 * @param altered
	 */
	public void onEntityIDAlteredOverlayClose(StainingTableChooser<?> chooser) {

		// checking if something was altered, if not do nothing
		if (chooser != null && chooser.isIdChanged()) {
			try {

				chooser.getEntity().setIdManuallyAltered(true);

				chooser.getEntity().updateAllNames(chooser.getEntity().getTask().isUseAutoNomenclature(), false);

				// TODO update childrens names
				genericDAO.savePatientData(chooser.getEntity(), "log.patient.task.idManuallyAltered",
						chooser.getEntity().toString());
				
				logger.debug("Text changed and saved!");
			} catch (HistoDatabaseInconsistentVersionException e) {
				// catching database version inconsistencies
				worklistViewHandlerAction.replaceSelectedTask();
			}
			chooser.setIdChanged(false);
		}
	}

	/**
	 * Creates slides if dialog returns the selected slides
	 * 
	 * @param event
	 */
	public void onSelectStainingDialogReturn(SelectEvent event) {
		logger.debug("On select staining dialog return ");

		if (event.getObject() != null && event.getObject() instanceof SlideSelectResult) {
			sampleService.createSlidesForSample((SlideSelectResult) event.getObject());
		}
		globalEditViewHandler.updateDataOfTask(true, false, true, true);
	}
}
