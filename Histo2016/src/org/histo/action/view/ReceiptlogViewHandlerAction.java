package org.histo.action.view;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;

import org.histo.action.dialog.SettingsDialogHandler;
import org.histo.action.dialog.slide.StainingPhaseLeaveDialogHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.handler.SlideManipulationHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.config.HistoSettings;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.StainingListAction;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.model.interfaces.IdManuallyAltered;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingTableChooser;
import org.histo.util.printer.PrintTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class ReceiptlogViewHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Lazy
	private PatientDao patientDao;

	@Autowired
	@Lazy
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private SettingsHandler settingsHandler;

	@Autowired
	@Lazy
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	@Lazy
	private SlideManipulationHandler slideManipulationHandler;

	@Autowired
	@Lazy
	private StainingPhaseLeaveDialogHandler stainingPhaseLeaveDialogHandler;

	@Autowired
	@Lazy
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Lazy
	private SettingsDialogHandler settingsDialogHandler;

	@Autowired
	@Lazy
	private GenericDAO genericDAO;

	@Autowired
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * This variable is used to save the selected action, which should be
	 * executed upon all selected slides
	 */
	private StainingListAction actionOnMany;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReceiptlogViewHandlerAction for task");
		// generating guilist for display
		task.generateSlideGuiList();

		// Setzte action to none
		setActionOnMany(StainingListAction.NONE);

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
			List<StainingTableChooser> list = task.getStainingTableRows();

			// at least one thing has to bee selected
			boolean atLeastOnechoosen = false;
			for (StainingTableChooser stainingTableChooser : list) {
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

				PrintTemplate[] arr = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON,
						new DocumentType[] { DocumentType.LABLE });

				if (arr.length == 0) {
					logger.debug("No template found for printing, returning!");
					return;
				}

				logger.debug("Printing labes for selected slides");

				for (StainingTableChooser stainingTableChooser : list) {
					if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {

						Slide slide = stainingTableChooser.getStaining();
						settingsHandler.getSelectedLabelPrinter().print(arr[0], slide,
								mainHandlerAction.date(System.currentTimeMillis()));
					}
				}

				settingsHandler.getSelectedLabelPrinter().flushPrints();

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
				task.setStainingCompletionDate(System.currentTimeMillis());
				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.change.stainingPhase.end");

				// removing from staining or restaing list

				favouriteListDAO.removeTaskFromList(task, new PredefinedFavouriteList[] {
						PredefinedFavouriteList.StainingList, PredefinedFavouriteList.ReStainingList });

				favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.DiagnosisList);

				stainingPhaseLeaveDialogHandler.initAndPrepareBean(task);
			} else {
				// reentering the staining phase, adding task to staining or
				// restaining list
				logger.trace("Enter staining phase, adding to staingin list");

				task.setStainingCompletionDate(0);
				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.change.stainingPhase.reentered");

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
	public void toggleChildrenChoosenFlag(StainingTableChooser chooser) {
		setChildrenAsChoosen(chooser, !chooser.isChoosen());
	}

	/**
	 * Sets all children of a StainingTableChoosers to chosen/unchosen
	 * 
	 * @param chooser
	 * @param choosen
	 */
	public void setChildrenAsChoosen(StainingTableChooser chooser, boolean chosen) {
		chooser.setChoosen(chosen);
		if (chooser.isSampleType() || chooser.isBlockType()) {
			for (StainingTableChooser tmp : chooser.getChildren()) {
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
	public void setListAsChoosen(List<StainingTableChooser> choosers, boolean chosen) {
		for (StainingTableChooser chooser : choosers) {
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

		PrintTemplate[] arr = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON,
				new DocumentType[] { DocumentType.LABLE });

		if (arr.length == 0) {
			logger.debug("No template found for lable printn!");
			return;
		}

		settingsHandler.getSelectedLabelPrinter().print(arr[0], slide,
				mainHandlerAction.date(System.currentTimeMillis()));
		settingsHandler.getSelectedLabelPrinter().flushPrints();

	}


	/**
	 * Saves the manually altered flag, if the sample/block/ or slide id was
	 * manually altered.
	 * 
	 * @param idManuallyAltered
	 * @param altered
	 */
	public void entityIDmanuallyAltered(IdManuallyAltered idManuallyAltered, boolean altered) {
		try {
			
			idManuallyAltered.setIdManuallyAltered(altered);
			
			idManuallyAltered.updateAllNames(idManuallyAltered.getTask().isUseAutoNomenclature(), false);
			
			//TODO update childrens names
			patientDao.savePatientAssociatedDataFailSave(idManuallyAltered, "log.patient.task.idManuallyAltered",
					idManuallyAltered.toString());
			
			
		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected();
		}
	}
	
	// ************************ Getter/Setter ************************
	public StainingListAction getActionOnMany() {
		return actionOnMany;
	}

	public void setActionOnMany(StainingListAction actionOnMany) {
		this.actionOnMany = actionOnMany;
	}

}
