package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.StainingListAction;
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

	private static Logger logger = Logger.getRootLogger();

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private HelperDAO helperDAO;

	@Autowired
	private SettingsDAO settingsDAO;
	
	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	MainHandlerAction mainHandlerAction;

	@Autowired
	DiagnosisHandlerAction diagnosisHandlerAction;

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
	private List<ListChooser<StainingPrototype>> stainingListChooser;

	/**
	 * used for adding new staings to block
	 */
	private String tmpCommentary;

	/**
	 * 
	 */
	private boolean tmpRestaining;

	/**
	 * This variable is used to save the selected action, which sho In dieser
	 * Variable wird die Aktion gespeichert, die in der Objektträgerliste auf
	 * alle ausgewählten elemente ausgeführt werden soll.
	 */
	private StainingListAction actionOnMany;

	/**
	 * Show a dialog for adding new slides to a block
	 * 
	 * @param sample
	 */
	public void prepareAddSlideDialog(Block blockToAddStaining) {
		setTmpBlock(blockToAddStaining);

		setTmpCommentary(new String());

		setTmpRestaining(blockToAddStaining.getParent().isReStainingPhase());

		setStainingListChooser(new ArrayList<ListChooser<StainingPrototype>>());

		List<StainingPrototype> allStainings = settingsDAO.getAllStainingPrototypes();

		for (StainingPrototype staining : allStainings) {
			getStainingListChooser().add(new ListChooser<StainingPrototype>(staining));
		}

		mainHandlerAction.showDialog(Dialog.SLIDE_CREATE);
	}

	/**
	 * Hides the dialog for adding new slides
	 */
	public void hideAddSlideDialog() {
		mainHandlerAction.hideDialog(Dialog.SLIDE_CREATE);
	}

	public void addSelectedSlides(List<ListChooser<StainingPrototype>> slideList, Block block, String commentary,
			boolean reStaining) {

		// überprüft ob eine neuer Objektträger erstellt werden soll, falls
		// nicht wird abgebrochen
		boolean slideChoosen = false;
		for (ListChooser<StainingPrototype> slide : slideList) {
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
		for (ListChooser<StainingPrototype> slide : slideList) {
			if (slide.isChoosen()) {
				addStaining(slide.getListItem(), block, commentary, reStaining);
			}
		}

		// updating statining list
		TaskUtil.generateSlideGuiList(block.getParent().getParent());

		// if staining is needed set the staining flag of the task object to
		// true
		// TODO save indepenetly from patient
		block.getParent().getParent().updateStainingStatus();

		genericDAO.save(block.getPatient(), resourceBundle.get("log.patient.save"), block.getPatient());

		hideAddSlideDialog();

	}

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
						&& !stainingTableChooser.getStaining().isStainingPerformed()) {
					Slide slide = stainingTableChooser.getStaining();
					slide.setStainingPerformed(true);

					genericDAO.save(slide,
							resourceBundle.get("log.patient.task.sample.blok.slide.stainingPerformed",
									slide.getParent().getParent().getParent().getTaskID(),
									slide.getParent().getParent().getSampleID(), slide.getParent().getBlockID(),
									slide.getSlideID()),
							task.getPatient());

				}
			}
			// shows dialog for informing the user that all stainings are
			// performed
			if (task.updateStainingStatus()) {
				mainHandlerAction.showDialog(Dialog.STAINING_PERFORMED);
				genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());
			}

			break;
		case NOT_PERFORMED:
			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()
						&& stainingTableChooser.getStaining().isStainingPerformed()) {
					stainingTableChooser.getStaining().setStainingPerformed(false);

					Slide slide = stainingTableChooser.getStaining();
					slide.setStainingPerformed(false);

					genericDAO.save(slide,
							resourceBundle.get("log.patient.task.sample.blok.slide.stainingNotPerformed",
									slide.getParent().getParent().getParent().getTaskID(),
									slide.getParent().getParent().getSampleID(), slide.getParent().getBlockID(),
									slide.getSlideID()),
							task.getPatient());
				}
			}
			
			if (task.updateStainingStatus()) {
				mainHandlerAction.showDialog(Dialog.STAINING_PERFORMED);
				genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());
			}
			
			break;
		case ARCHIVE:
			// TODO implement
			System.out.println("To impliment");
			break;
		case PRINT:
			LabelPrinter[] test = LabelPrinter.factroy(HistoSettings.LABEL_PRINTER_JSON);

			for (StainingTableChooser stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {

					Slide slide = stainingTableChooser.getStaining();
					String taskID = slide.getParent().getParent().getParent().getTaskID();

					HashMap<String, String> args = new HashMap<String, String>();
					args.put("%slideNumber%", taskID + HistoUtil.fitString(slide.getUniqueIDinBlock(), 2, '0'));
					args.put("%slideName%", taskID + " " + slide.getSlideID());
					args.put("%slideText%", slide.getCommentary());
					args.put("%date%", mainHandlerAction.date(System.currentTimeMillis()));

					test[0].addTaskToBuffer(HistoUtil.replaceWildcardsInString(
							mainHandlerAction.getSettings().getDefaultSlideLableLayout(), args));
				}
			}

			if (test[0].isBufferNotEmpty()) {
				try {
					test[0].openConnection();
					test[0].flushBuffer();
					test[0].closeConnection();
					System.out.println(test[0].isBufferNotEmpty());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			break;
		default:
			break;
		}

		setActionOnMany(StainingListAction.NONE);

	}

	public void hideStainingsPerformedDialog() {
		mainHandlerAction.hideDialog(Dialog.STAINING_PERFORMED);
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
		Slide slide = TaskUtil.createNewStaining(block, prototype);

		logger.debug("New Slide created " + slide.getSlideID());

		if (commentary != null && !commentary.isEmpty())
			slide.setCommentary(commentary);

		slide.setReStaining(reStaining);

		genericDAO.save(slide, resourceBundle.get("log.patient.task.sample.blok.slide.new",
				slide.getParent().getParent().getParent().getTaskID(), slide.getParent().getParent().getSampleID(),
				slide.getParent().getBlockID(), slide.getSlideID()), slide.getPatient());

		// setting restaining flag of the diagnosis
//		if (reStaining) {
//			List<DiagnosisRevision> reports = slide.getParent().getParent().getParent().getReports();
//			if (diagnoses.size() > 0) {
//				Diagnosis diagnosis = diagnoses.get(diagnoses.size() - 1);
//				if (!diagnosis.isDiagnosisRevision()) {
//					diagnosis.setDiagnosisRevision(true);
//					diagnosisHandlerAction.diagnosisDataChanged(diagnosis,
//							"log.patient.task.sample.diagnosis.changed.followUP", diagnosis.isDiagnosisRevision());
//				}
//			}
//		}
		
		// TODO: rework
	}

	/********************************************************
	 * Staining Manipulation
	 ********************************************************/

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
