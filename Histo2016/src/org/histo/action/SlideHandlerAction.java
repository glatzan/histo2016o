package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.StainingListAction;
import org.histo.dao.GenericDAO;
import org.histo.dao.SettingsDAO;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.ListChooser;
import org.histo.ui.StainingTableChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class SlideHandlerAction implements Serializable {

	private static final long serialVersionUID = -7212398949353596573L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	/**
	 * Temporary task object, for finalizing stainigs
	 */
	private Task temporaryTask;

	/**
	 * Used for
	 */
	private Sample tmpSample;

	/********************************************************
	 * Create new slide
	 ********************************************************/
	/**
	 * List for selecting staining, this list contains all stainigns not added
	 * in tmpSample
	 */
	private List<ListChooser<StainingPrototype>> stainingListChooser;

	/**
	 * Temporary Block for creating new slides
	 */
	private Block temporaryBlock;

	/**
	 * used for adding new staings to block
	 */
	private String slideCommentary;

	/**
	 * used for adding new staings to block
	 */
	private boolean slideRestaining;

	/********************************************************
	 * Create new slide
	 ********************************************************/

	/**
	 * This variable is used to save the selected action, which sho In dieser
	 * Variable wird die Aktion gespeichert, die in der Objektträgerliste auf
	 * alle ausgewählten elemente ausgeführt werden soll.
	 */
	private StainingListAction actionOnMany;

	/**
	 * Hides dialogs associated with the slideHandlerAction, resets all
	 * variables
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		setTemporaryBlock(null);
		setTemporaryTask(null);
		mainHandlerAction.hideDialog(dialog);
	}

	/********************************************************
	 * Add Slide from Gui
	 ********************************************************/
	/**
	 * Show a dialog for adding new slides to a block
	 * 
	 * @param sample
	 */
	public void prepareAddSlideDialog(Block blockToAddStaining) {
		setTemporaryBlock(blockToAddStaining);

		setSlideCommentary("");

		setSlideRestaining(blockToAddStaining.getParent().isReStainingPhase());

		setStainingListChooser(new ArrayList<ListChooser<StainingPrototype>>());

		List<StainingPrototype> allStainings = settingsDAO.getAllStainingPrototypes();

		for (StainingPrototype staining : allStainings) {
			getStainingListChooser().add(new ListChooser<StainingPrototype>(staining));
		}

		mainHandlerAction.showDialog(Dialog.SLIDE_CREATE);
	}

	public void addSlidesFromGui() {

		// überprüft ob eine neuer Objektträger erstellt werden soll, falls
		// nicht wird abgebrochen
		boolean slideChoosen = false;
		for (ListChooser<StainingPrototype> slide : getStainingListChooser()) {
			if (slide.isChoosen()) {
				slideChoosen = true;
				break;
			}
		}

		if (!slideChoosen) {
			hideDialog(Dialog.SLIDE_CREATE);
			return;
		}

		// fügt einen neune Objektträger hinzu
		for (ListChooser<StainingPrototype> slide : getStainingListChooser()) {
			if (slide.isChoosen()) {
				taskManipulationHandler.createSlide(slide.getListItem(), getTemporaryBlock(), getSlideCommentary(),
						isSlideRestaining());
			}
		}

		// if staining is needed set the staining flag of the task object to
		// true
		getTemporaryBlock().getTask().getStatus().updateStainingStatus();

		// updating statining list
		getTemporaryBlock().getTask().generateSlideGuiList();

		mainHandlerAction.saveDataChange(getTemporaryBlock(), "log.patient.task.sample.block.update",
				getTemporaryBlock().getBlockID());

		hideDialog(Dialog.SLIDE_CREATE);
	}

	/********************************************************
	 * Add Slide from Gui
	 ********************************************************/

	/********************************************************
	 * Many Staining Manipulation
	 ********************************************************/
	/**
	 * Toggelt den Status eines StainingTableChoosers und aller Kinder
	 * 
	 * @param chooser
	 */
	public void toggleChildrenChoosenFlag(StainingTableChooser chooser) {
		setChildrenAsChoosen(chooser, !chooser.isChoosen());
	}

	/**
	 * Setzt den Status eines StainingTableChoosers und aller Kindern
	 * 
	 * @param chooser
	 * @param choosen
	 */
	public void setChildrenAsChoosen(StainingTableChooser chooser, boolean choosen) {
		chooser.setChoosen(choosen);
		if (chooser.isSampleType() || chooser.isBlockType()) {
			for (StainingTableChooser tmp : chooser.getChildren()) {
				setChildrenAsChoosen(tmp, choosen);
			}
		}

		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Setzt den Status einer Liste von StainingTableChoosers und ihrer Kinder
	 * 
	 * @param choosers
	 * @param choosen
	 */
	public void setListAsChoosen(List<StainingTableChooser> choosers, boolean choosen) {
		logger.debug("Settings list choosen as: " + choosen);
		for (StainingTableChooser chooser : choosers) {
			if (chooser.isSampleType()) {
				setChildrenAsChoosen(chooser, choosen);
			}
		}
	}

	public void performActionOnMany(Task task) {
		performActionOnMany(task, getActionOnMany());
		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Fürt
	 * 
	 * @param list
	 * @param action
	 */
	public void performActionOnMany(Task task, StainingListAction action) {
		List<StainingTableChooser> list = task.getStainingTableRows();

		// mindestens ein Objektträger muss ausgewählt sein
		boolean atLeastOnechoosen = false;
		for (StainingTableChooser stainingTableChooser : list) {
			if (stainingTableChooser.isChoosen()) {
				atLeastOnechoosen = true;
				break;
			}
		}

		if (!atLeastOnechoosen)
			return;

		switch (getActionOnMany()) {
		case PERFORMED:
			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()
						&& !stainingTableChooser.getStaining().isStainingCompleted()) {
					Slide slide = stainingTableChooser.getStaining();
					slide.setStainingCompleted(true);

					mainHandlerAction.saveDataChange(slide, "log.patient.task.sample.blok.slide.stainingPerformed",
							String.valueOf(slide.getId()));
				}
			}
			// shows dialog for informing the user that all stainings are
			// performed
			showStainingPhaseLeave(task);

			break;
		case NOT_PERFORMED:
			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()
						&& stainingTableChooser.getStaining().isStainingCompleted()) {
					stainingTableChooser.getStaining().setStainingCompleted(false);

					Slide slide = stainingTableChooser.getStaining();
					slide.setStainingCompleted(false);

					mainHandlerAction.saveDataChange(slide, "log.patient.task.sample.blok.slide.stainingNotPerformed",
							String.valueOf(slide.getId()));
				}
			}

			showStainingPhaseLeave(task);

			break;
		case ARCHIVE:
			// TODO implement
			System.out.println("To impliment");
			break;
		case PRINT:
			mainHandlerAction.getSettings().getLabelPrinterManager()
					.loadPrinter(userHandlerAction.getCurrentUser().getPreferedLabelPritner());

			PrintTemplate[] arr = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON,
					new DocumentType[] { DocumentType.LABLE });

			if (arr.length == 0) {
				logger.debug("No Template found, returning.");
				return;
			}

			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {

					Slide slide = stainingTableChooser.getStaining();

					mainHandlerAction.getSettings().getLabelPrinterManager().print(arr[0], slide,
							mainHandlerAction.date(System.currentTimeMillis()));
				}
			}

			mainHandlerAction.getSettings().getLabelPrinterManager().flushPrints();

			break;
		default:
			break;
		}

		setActionOnMany(StainingListAction.NONE);

	}

	public void printLableForSlide(Slide slide) {

		mainHandlerAction.getSettings().getLabelPrinterManager()
				.loadPrinter(userHandlerAction.getCurrentUser().getPreferedLabelPritner());

		PrintTemplate[] arr = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON,
				new DocumentType[] { DocumentType.LABLE });

		if (arr.length == 0) {
			logger.debug("No Template found, returning.");
			return;
		}

		mainHandlerAction.getSettings().getLabelPrinterManager().print(arr[0], slide,
				mainHandlerAction.date(System.currentTimeMillis()));
		mainHandlerAction.getSettings().getLabelPrinterManager().flushPrints();

	}

	/********************************************************
	 * Many Staining Manipulation
	 ********************************************************/

	/********************************************************
	 * Default leave Staining Phase Dialog
	 ********************************************************/

	public void showStainingPhaseLeave(Task task) {
		showStainingPhaseLeave(task, false);
	}

	public void showStainingPhaseLeave(Task task, boolean setAll) {
		logger.trace("Method: showStainingPhaseLeave(Task task)");

		// setting all to performed
		if (setAll) {
			for (StainingTableChooser stainingTableChooser : task.getStainingTableRows()) {
				if (stainingTableChooser.isStainingType()
						&& !stainingTableChooser.getStaining().isStainingCompleted()) {
					Slide slide = stainingTableChooser.getStaining();
					slide.setStainingCompleted(true);

					mainHandlerAction.saveDataChange(slide, "log.patient.task.sample.blok.slide.stainingPerformed",
							String.valueOf(slide.getId()));
				}
			}
		}

		// if task has changed
		if (task.getStatus().updateStainingStatus()) {
			logger.trace("Status has changed!");
			if (task.getStatus().isStainingPerformed()) {
				// staining is now performed
				// setting time of completion

				setTemporaryTask(task);

				mainHandlerAction.saveDataChange(task, "log.patient.task.change.stainingPhase.end");

				// show dialog for notifying the user that the task will be
				// passed to diagnosis phase, and offering the option to hold
				// the task also in staining phase
				mainHandlerAction.showDialog(Dialog.STAINING_PHASE_LEAVE);
			} else {
				// there are new slides to stain, the stain-process was finished
				// before, so re-enter the staining phase
				mainHandlerAction.saveDataChange(task, "log.patient.task.change.stainingPhase.reentered");
			}
		}

	}

	/**
	 * Keeps the task in staining phase if phase is true. Hides the
	 * Dialog.STAINING_PHASE_LEAVE dialog.
	 * 
	 * @param phase
	 */
	public void hideStainingPhaseLeave(boolean stayInPhase) {
		logger.trace("Method: hideStainingPhaseLeave(boolean stayInPhase)");
		if (stayInPhase) {
			logger.debug("StayInPhase is true");
			getTemporaryTask().getStatus().updateStainingStatus(stayInPhase);
			mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.forced");
		}
		hideDialog(Dialog.STAINING_PHASE_LEAVE);
	}

	/********************************************************
	 * Default leave Staining Phase Dialog
	 ********************************************************/

	/********************************************************
	 * Dialog for forcing stay and leave of staining phase
	 ********************************************************/
	/**
	 * Shows the dialog for forcing the phase leave or entering
	 * 
	 * @param task
	 */
	public void showStaingPhaseForceDialog(Task task, boolean force) {
		setTemporaryTask(task);
		if (force)
			mainHandlerAction.showDialog(Dialog.STAINING_PHASE_FORCE_ENTER);
		else
			mainHandlerAction.showDialog(Dialog.STAINING_PHASE_FORCE_LEAVE);
	}

	/**
	 * Removes the task from the staining list, if task is not finalized enter
	 * diangosis phase
	 */
	public void forceLeaveStainingPhaseAndHideDialog() {
		// sets diagnosis phase if task is not finalized and no other phase is
		// active
		if (!getTemporaryTask().isFinalized() && !getTemporaryTask().getStatus().isStainingPhaseAndOtherPhase()) {
			logger.debug("Setting diagnosis phase to true");
			getTemporaryTask().setDiagnosisPhase(true);
		}

		temporaryTask.setStainingPhase(false);

		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.end");

		hideDialog(Dialog.STAINING_PHASE_FORCE_LEAVE);
	}

	/**
	 * Adds the task to the stating list, even if all stanings are completed
	 */
	public void forceEnterStainingPhaseAndHideDialog() {
		getTemporaryTask().setStainingPhase(true);
		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.forced");
		hideDialog(Dialog.STAINING_PHASE_FORCE_ENTER);
	}

	/********************************************************
	 * Dialog for forcing stay and leave of staining phase
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public Block getTemporaryBlock() {
		return temporaryBlock;
	}

	public void setTemporaryBlock(Block temporaryBlock) {
		this.temporaryBlock = temporaryBlock;
	}

	public List<ListChooser<StainingPrototype>> getStainingListChooser() {
		return stainingListChooser;
	}

	public void setStainingListChooser(List<ListChooser<StainingPrototype>> stainingListChooser) {
		this.stainingListChooser = stainingListChooser;
	}

	public Sample getTmpSample() {
		return tmpSample;
	}

	public void setTmpSample(Sample tmpSample) {
		this.tmpSample = tmpSample;
	}

	public StainingListAction getActionOnMany() {
		return actionOnMany;
	}

	public void setActionOnMany(StainingListAction actionOnMany) {
		this.actionOnMany = actionOnMany;
	}

	public Task getTemporaryTask() {
		return temporaryTask;
	}

	public void setTemporaryTask(Task temporaryTask) {
		this.temporaryTask = temporaryTask;
	}

	public String getSlideCommentary() {
		return slideCommentary;
	}

	public boolean isSlideRestaining() {
		return slideRestaining;
	}

	public void setSlideCommentary(String slideCommentary) {
		this.slideCommentary = slideCommentary;
	}

	public void setSlideRestaining(boolean slideRestaining) {
		this.slideRestaining = slideRestaining;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
