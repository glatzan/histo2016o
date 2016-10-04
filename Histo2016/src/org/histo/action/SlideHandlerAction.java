package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingListChooser;
import org.histo.ui.StainingTableChooser;
import org.histo.util.SlideUtil;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class SlideHandlerAction implements Serializable {

	private static final long serialVersionUID = -7212398949353596573L;

	public final static byte STAININGLIST_ACTION_NONE = 0;
	public final static byte STAININGLIST_ACTION_PERFORMED = 1;
	public final static byte STAININGLIST_ACTION_NOT_PERFORMED = 2;
	public final static byte STAININGLIST_ACTION_PRINT = 3;
	public final static byte STAININGLIST_ACTION_ARCHIVE = 4;

	@Autowired
	private GenericDAO genericDAO;
	
	@Autowired
	private HelperDAO helperDAO;
	
	@Autowired
	private HelperHandlerAction helper;

	/**
	 * Temporäres Blockobjekt, wird verwendet um neue Objektträger zu erstellen.
	 */
	private Block tmpBlock;

	/**
	 * Used for
	 */
	private Sample tmpSample;

	/**
	 * List for selecting staining, this list contains all stainigns not added
	 * in tmpSample
	 */
	private List<StainingListChooser> stainingListChooser;

	/**
	 * used for adding new staings to block
	 */
	private String tmpCommentary;

	/**
	 * 
	 */
	private boolean tmpRestaining;

	/**
	 * In dieser Variable wird die Aktion gespeichert, die in der
	 * Objektträgerliste auf alle ausgewählten elemente ausgeführt werden soll.
	 */
	private byte actionOnMany;

	/********************************************************
	 * Add Slide to Block
	 ********************************************************/
	/**
	 * Zeigt einen Dialog um einen neunen Objektträger für eine Block zu
	 * erstellen
	 * 
	 * @param sample
	 */
	public void prepareAddSlideDialog(Block blockToAddStaining) {
		setTmpBlock(blockToAddStaining);

		setTmpCommentary(new String());

		setTmpRestaining(blockToAddStaining.getParent().isReStainingPhase());

		setStainingListChooser(new ArrayList<StainingListChooser>());

		List<StainingPrototype> allStainings = helperDAO.getAllStainings();

		for (StainingPrototype staining : allStainings) {
			getStainingListChooser().add(new StainingListChooser(staining));
		}

		// log.info("Opening select staining dialog");
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_ADD_SLIDE), false, false, true);
	}

	public void addSelectedSlides(List<StainingListChooser> slideList, Block block, String commentary,
			boolean reStaining) {

		// überprüft ob eine neuer Objektträger erstellt werden soll, falls
		// nicht wird abgebrochen
		boolean slideChoosen = false;
		for (StainingListChooser slide : slideList) {
			if (slide.isChoosen()) {
				slideChoosen = true;
				break;
			}
		}

		if (!slideChoosen) {
			hideAddSlideDialog();
			return;
		}

		// fügt einen neune Objektträger hinzu
		for (StainingListChooser slide : slideList) {
			if (slide.isChoosen()) {
				addStaining(slide.getStainingPrototype(), block, commentary, reStaining);
			}
		}

		hideAddSlideDialog();

	}

	/**
	 * Schließt den addSlide Dialog
	 */
	public void hideAddSlideDialog() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_ADD_SLIDE));
	}

	/********************************************************
	 * Add Staining to Block
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

		setActionOnMany(STAININGLIST_ACTION_NONE);

	}

	/**
	 * Setzt den Status einer Liste von StainingTableChoosers und ihrer Kinder
	 * 
	 * @param choosers
	 * @param choosen
	 */
	public void setListAsChoosen(List<StainingTableChooser> choosers, boolean choosen) {
		for (StainingTableChooser chooser : choosers) {
			if (chooser.isSampleType()) {
				setChildrenAsChoosen(chooser, choosen);
			}
		}
	}

	public void performActionOnMany(Task task) {
		performActionOnMany(task, getActionOnMany());
		setActionOnMany(STAININGLIST_ACTION_NONE);
	}

	/**
	 * Fürt
	 * 
	 * @param list
	 * @param action
	 */
	public void performActionOnMany(Task task, byte action) {
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
		case STAININGLIST_ACTION_PERFORMED:
			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {
					stainingTableChooser.getStaining().setStainingPerformed(true);
				}
			}
			// zeige Dialog das Alle Färbungen erledigt wurden.
			if (updateSlideTree(task)) {
				helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_STAINING_PERFORMED));
			}

			break;
		case STAININGLIST_ACTION_NOT_PERFORMED:
			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {
					stainingTableChooser.getStaining().setStainingPerformed(false);
				}
			}
			break;
		case STAININGLIST_ACTION_ARCHIVE:
			// TODO implement
			System.out.println("To impliment");
			break;
		default:
			break;
		}

	}

	/**
	 * Geht eine Task durch und schaut ob alle färbungen vollstädnig sind
	 * 
	 * @param sample
	 */
	public boolean updateSlideTree(Task task) {
		boolean allPerformed = true;
		for (Sample sample : task.getSamples()) {
			// übersprinen, wenn sample archiviert wurde
			if (sample.isArchived())
				continue;

			// nur als completed markieren wenn mindestens eine Färbung
			// vorhanden ist
			if (sample.isStainingPerformed() && SlideUtil.checkIfAtLeastOnSlide(sample))
				sample.setReStainingPhase(true);
			else {
				allPerformed = false;
			}

			// log.info("Updating stainings, set stainingCompleted to " +
			// task.isStainingCompleted());
		}

		genericDAO.save(task);
		if (allPerformed)
			task.setStainingCompleted(true);
		else
			task.setStainingCompleted(false);
		return allPerformed;
	}

	public void hideStainingsPerformedDialog() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_STAINING_PERFORMED));
	}

	/********************************************************
	 * Many Staining Manipulation
	 ********************************************************/

	/********************************************************
	 * Staining Manipulation
	 ********************************************************/
	/**
	 * Adds a new staining to a block. Needs the sample an the patient for
	 * logging. Commentary will be null.
	 * 
	 * @param prototype
	 * @param sample
	 * @param block
	 * @param patientOfSample
	 */
	public void addStaining(StainingPrototype prototype, Block block) {
		addStaining(prototype, block, null, false);
	}

	/**
	 * Adds a new staining to a block. Needs the sample an the patient for
	 * logging. Commentary the given string.
	 * 
	 * @param prototype
	 * @param sample
	 * @param block
	 * @param commentary
	 * @param patientOfSample
	 */
	public void addStaining(StainingPrototype prototype, Block block, String commentary, boolean reStaining) {
		Slide staining = TaskUtil.createNewStaining(block, prototype);
		if (commentary != null && !commentary.isEmpty())
			staining.setCommentary(commentary);

		staining.setReStaining(reStaining);

		genericDAO.save(block.getParent().getParent());

		// log.info("Neue Färbung erstellt, ID:" + staining.getStainingID() + "
		// - " + staining.getStainingType().getName(), block.getPatient());

		// Updating Gui
		block.getParent().getParent().setStainingCompleted(false);
		block.getParent().getParent().generateStainingGuiList();
	}

	/********************************************************
	 * Staining Manipulation
	 ********************************************************/

	/**
	 * Saves all stainigs, if all stainigs are marked as performed
	 * prepareStainingsPerformed method is called
	 * 
	 * @param sample
	 */
	public void saveStainings(Task task) {
		updateSlideTree(task);
		genericDAO.save(task);
	}

	/******************************************************** Diagnosis ********************************************************/

	// public void updateSample(Sample sample) {
	// boolean completed = true;
	//
	// for (Diagnosis diagnosis2 : sample.getDiagnoses()) {
	// if (!diagnosis2.isFinalized())
	// completed = false;
	// }
	//
	// if (completed) {
	// sample.setDiagnosisCompletionDate(System.currentTimeMillis());
	// sample.setDiagnosisCompleted(true);
	// } else {
	// sample.setDiagnosisCompleted(false);
	// sample.setDiagnosisCompletionDate(System.currentTimeMillis());
	// }
	// }

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public String getTmpCommentary() {
		return tmpCommentary;
	}

	public void setTmpCommentary(String tmpCommentary) {
		this.tmpCommentary = tmpCommentary;
	}

	public Block getTmpBlock() {
		return tmpBlock;
	}

	public void setTmpBlock(Block tmpBlock) {
		this.tmpBlock = tmpBlock;
	}

	public List<StainingListChooser> getStainingListChooser() {
		return stainingListChooser;
	}

	public void setStainingListChooser(List<StainingListChooser> stainingListChooser) {
		this.stainingListChooser = stainingListChooser;
	}

	public Sample getTmpSample() {
		return tmpSample;
	}

	public void setTmpSample(Sample tmpSample) {
		this.tmpSample = tmpSample;
	}

	public byte getActionOnMany() {
		return actionOnMany;
	}

	public void setActionOnMany(byte actionOnMany) {
		this.actionOnMany = actionOnMany;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public boolean isTmpRestaining() {
		return tmpRestaining;
	}

	public void setTmpRestaining(boolean tmpRestaining) {
		this.tmpRestaining = tmpRestaining;
	}
}
