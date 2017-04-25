package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.StainingListAction;
import org.histo.config.enums.StainingStatus;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.SettingsDAO;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.LabelPrinter;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.ListChooser;
import org.histo.ui.StainingTableChooser;
import org.histo.util.HistoUtil;
import org.histo.util.SlideUtil;
import org.histo.util.TaskUtil;
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
		getTemporaryBlock().getTask().hasStatingStatusChanged();

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
			showStainingPhaseEndAutoDialog(task);

			break;
		case NOT_PERFORMED:
			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()
						&& stainingTableChooser.getStaining().isStainingCompleted()) {
					stainingTableChooser.getStaining().setStainingCompleted(false);

					Slide slide = stainingTableChooser.getStaining();
					slide.setStainingCompleted(false);

					genericDAO.save(slide,
							resourceBundle.get("log.patient.task.sample.blok.slide.stainingNotPerformed",
									slide.getParent().getParent().getParent().getTaskID(),
									slide.getParent().getParent().getSampleID(), slide.getParent().getBlockID(),
									slide.getSlideID()),
							task.getPatient());
				}
			}

			showStainingPhaseEndAutoDialog(task);

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
	 * Staining Phase Dialog Auto
	 ********************************************************/
	/**
	 * Checks if all staings are completed an shows a dialog informing the user about this fact and offering the opportunity to keep the task in the staining phase
	 * @formatter:off
	 * Option one -> Task in staining phase, staining completed -> End? (dialog, showEndStaingPhaseDialog)
	 * Option two -> Task in staining phase, staining is about to be completed -> Shift to diagnosis (dialog)
	 * Option three -> Task in staining phase, staining not completed -> Force to diagnosis (dialog, showForceDiagnosisPhaseDialog)
	 * Option four -> Task is not in staining phase, new slide -> staining phase (no dialog)
	 * @formatter:on
	 * @param task
	 */
	public void showStainingPhaseEndAutoDialog(Task task) {
		logger.trace("Method: showStainingPhaseEndAutoDialog(Task task)");
		// if task has changed
		if (task.hasStatingStatusChanged()) {
			if (task.getStainingStatus() == StainingStatus.PERFORMED) {
				// staining is now performed
				// setting time of completion

				setTemporaryTask(task);

				// show dialog for notifying the user that the task will be
				// passed to diagnosis phase, and offering the option to hold
				// the task also in staining phase
				mainHandlerAction.showDialog(Dialog.STAINING_PHASE_END_AUTO);

				mainHandlerAction.saveDataChange(task, "log.patient.task.change.stainingPhase.end");
			} else {
				// there are new slides to stain, the stain-process was finished
				// before, so re-enter the staining phase
				mainHandlerAction.saveDataChange(task, "log.patient.task.change.stainingPhase.reentered");
			}
		}

	}

	/**
	 * Keeps the task in staining phase if phase is true. Hides the
	 * Dialog.STAINING_PHASE_END_AUTO dialog.
	 * 
	 * @param phase
	 */
	public void stayInStainingPhase(boolean phase) {
		getTemporaryTask().setStainingPhase(phase);
		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.forced");
		hideDialog(Dialog.STAINING_PHASE_END_AUTO);
	}

	/********************************************************
	 * Staining Phase Dialog Auto
	 ********************************************************/

	/********************************************************
	 * Staining Phase Dialog Manual
	 ********************************************************/
	/**
	 * Shows a dialog for ending the staining phase manually, if no stainig task
	 * is left and the user had kept the task in staining phase
	 * 
	 * @param task
	 */
	public void showStaingPhaseEndManualDialog(Task task) {
		// if task was hold in staining phase but the staining had been
		// performed, show dialog to end staining phase
		mainHandlerAction.showDialog(Dialog.STAINING_PHASE_END_MANUAL);
		setTemporaryTask(task);
	}

	/**
	 * Removes the task from the staining phase, enables diagnosis phase if
	 * diagnosis was not done jet.
	 */
	public void removeFromStainingPhase() {
		temporaryTask.setStainingPhase(false);

		// if the diagnoses process of the task has not been finished, set to
		// diagnosis phase
		if (!getTemporaryTask().isFinalized() && getTemporaryTask().getDiagnosisCompletionDate() == 0) {
			logger.debug("Setting diagnosis phase to true");
			getTemporaryTask().setDiagnosisPhase(true);
		}

		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.end");

		hideDialog(Dialog.STAINING_PHASE_END_MANUAL);
	}

	/********************************************************
	 * Staining Phase Dialog Manual
	 ********************************************************/

	/********************************************************
	 * Force Staining phase
	 ********************************************************/
	public void showForceStainingPhaseDialog(Task task) {
		mainHandlerAction.showDialog(Dialog.STAINING_PHASE_FORCED);
		setTemporaryTask(task);
	}

	public void forceStainingPhase() {
		getTemporaryTask().setStainingPhase(true);
		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.forced");
		hideDialog(Dialog.STAINING_PHASE_FORCED);
	}

	/********************************************************
	 * Force Staining phase
	 ********************************************************/

	/********************************************************
	 * Force Diagnosis Phase Dialog From Staining
	 ********************************************************/
	/**
	 * Shows a dialog for shifting the task to diagnosis phase even if not all
	 * staining tasks are completed.
	 * 
	 * @param task
	 */
	public void showForceDiagnosisPhaseDialog(Task task) {
		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_PHASE_FORCED);
		setTemporaryTask(task);
	}

	/**
	 * Shifts the task to diagnosis phase, leave the staining phase as is
	 */
	public void forceDiagnosisPhase() {
		getTemporaryTask().setDiagnosisPhase(true);
		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.diagnosisPhase.forced");
		hideDialog(Dialog.DIAGNOSIS_PHASE_FORCED);
	}

	/********************************************************
	 * Force Diagnosis Phase Dialog From Staining
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
