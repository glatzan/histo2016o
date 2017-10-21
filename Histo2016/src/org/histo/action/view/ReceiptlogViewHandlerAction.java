package org.histo.action.view;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.SettingsDialogHandler;
import org.histo.action.dialog.slide.StainingPhaseExitDialog;
import org.histo.action.handler.SlideManipulationHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.StainingListAction;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.ListItem;
import org.histo.model.interfaces.IdManuallyAltered;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateSlideLable;
import org.histo.ui.StainingTableChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

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
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SlideManipulationHandler slideManipulationHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsDialogHandler settingsDialogHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * This variable is used to save the selected action, which should be
	 * executed upon all selected slides
	 */
	private StainingListAction actionOnMany;

	/**
	 * Contains all available case histories
	 */
	private List<ListItem> slideCommentary;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReceiptlogViewHandlerAction for task");
		// generating guilist for display
		task.generateSlideGuiList();

		// Setzte action to none
		setActionOnMany(StainingListAction.NONE);

		if (getSlideCommentary() == null)
			setSlideCommentary(utilDAO.getAllStaticListItems(ListItem.StaticList.SLIDES));
	}

	public List<String> getTest(String query) {
		return slideCommentary.stream().map(p -> p.getValue()).collect(Collectors.toList());
	}

	public void performActionOnMany(Task task) {
		performActionOnMany(task, getActionOnMany());
		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Executes an action on all selected slides
	 * 
	 * @param list
	 * @param action
	 */
	public void performActionOnMany(Task task, StainingListAction action) {
		try {
			List<StainingTableChooser<?>> list = task.getStainingTableRows();

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

				boolean changed = slideManipulationHandler.setStainingCompletedForSelectedSlides(list, true);

				// shows dialog for informing the user that all stainings are
				// performed
				checkStainingPhase(task, changed);

				break;
			case NOT_PERFORMED:
				logger.debug("Setting staining status of selected slides to not perforemd!");

				changed = slideManipulationHandler.setStainingCompletedForSelectedSlides(list, false);

				checkStainingPhase(task, changed);

				break;
			case ARCHIVE:
				// TODO implement
				System.out.println("To impliment");
				break;
			case PRINT:

				DocumentTemplate[] arr = DocumentTemplate.getTemplates(DocumentType.LABLE);

				if (arr.length == 0 || !(arr[0] instanceof TemplateSlideLable)) {
					logger.debug("No template found for printing, returning!");
					return;
				}

				TemplateSlideLable printTemplate = (TemplateSlideLable) arr[0];

				printTemplate.prepareTemplate();

				logger.debug("Printing labes for selected slides");

				List<TemplateSlideLable> toPrint = new ArrayList<TemplateSlideLable>();

				for (StainingTableChooser<?> stainingTableChooser : list) {
					if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {

						Slide slide = (Slide) stainingTableChooser.getEntity();

						TemplateSlideLable tmp = (TemplateSlideLable) printTemplate.clone();
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
		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
		}

		setActionOnMany(StainingListAction.NONE);

	}

	/**
	 * Used from external button to end staing phase
	 * 
	 * @param task
	 */
	public void performStainingsAndCheckStainingPhase(Task task) {
		try {
			boolean changed = slideManipulationHandler.setStainingCompletedForAllSlides(task, true);
			checkStainingPhase(task, changed);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
		}
	}

	/**
	 * If all staings are completed an dialog will be shown, to end staing phase
	 * 
	 * @param task
	 * @param changed
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void checkStainingPhase(Task task, boolean changed) throws CustomDatabaseInconsistentVersionException {
		if (changed) {
			// removing from staining list and showing the dialog for ending
			// staining phase
			if (taskStatusHandler.isStainingCompleted(task)) {
				logger.trace("Staining phase completed removing from staing list, adding to diagnosisList");
				dialogHandlerAction.getStainingPhaseExitDialog().initAndPrepareBean(task);
			} else {
				// reentering the staining phase, adding task to staining or
				// restaining list
				logger.trace("Enter staining phase, adding to staingin list");

				task.setStainingCompletionDate(0);
				genericDAO.savePatientData(task, "log.patient.task.change.stainingPhase.reentered");

				if (taskStatusHandler.isReStainingFlag(task)) {
					logger.debug("Adding to restaining list, if not in list");
					favouriteListDAO.removeTaskFromList(task, PredefinedFavouriteList.StainingList);
					favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.ReStainingList);
				} else {
					logger.debug("Adding to staining list, if not in list");
					favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.StainingList);
				}
			}
		}
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

		DocumentTemplate[] arr = DocumentTemplate.getTemplates(DocumentType.LABLE);

		if (arr.length == 0 || !(arr[0] instanceof TemplateSlideLable)) {
			logger.debug("No template found for lable printn!");
			return;
		}

		TemplateSlideLable printTemplate = (TemplateSlideLable) arr[0];
		printTemplate.prepareTemplate();
		printTemplate.initData(slide.getTask(), slide, new Date(System.currentTimeMillis()));
		printTemplate.fillTemplate();

		userHandlerAction.getSelectedLabelPrinter().print(printTemplate);

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
		if (chooser.isIdChanged()) {
			try {

				chooser.getEntity().setIdManuallyAltered(true);

				chooser.getEntity().updateAllNames(chooser.getEntity().getTask().isUseAutoNomenclature(), false);

				// TODO update childrens names
				genericDAO.savePatientData(chooser.getEntity(), "log.patient.task.idManuallyAltered",
						chooser.getEntity().toString());

			} catch (CustomDatabaseInconsistentVersionException e) {
				// catching database version inconsistencies
				worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
			}
			chooser.setIdChanged(false);
		}
	}
}
