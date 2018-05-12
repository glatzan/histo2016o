package org.histo.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.dialog.slide.CreateSlidesDialog.SlideSelectResult;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class SampleService {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DiagnosisService diagnosisService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	/**
	 * Creates an sample, udpates all names and checks the statining phase
	 * 
	 * @param task
	 * @param material
	 */
	public void createSampleForTask(Task task, MaterialPreset material) {
		createSample(task, material, true, false);
		// updating names
		task.getSamples().forEach(p -> p.updateAllNames());

		updateStaingPhase(task);

		genericDAO.savePatientData(task, "log.patient.task.update", task.getTaskID());
	}

	/**
	 * Creates a new sample and adds this sample to the given task. Creates a new
	 * diagnosis and a new block with slides as well.
	 * 
	 * @param task
	 */
	public void createSample(Task task, MaterialPreset material, boolean createBlock, boolean naming) {
		Sample sample = new Sample();
		sample.setCreationDate(System.currentTimeMillis());
		sample.setParent(task);
		sample.setMaterialPreset(material);
		sample.setMaterial(material == null ? "" : material.getName());
		task.getSamples().add(sample);

		logger.debug("Creating new sample " + sample.getSampleID());

		genericDAO.savePatientData(sample, "log.patient.task.sample.new", sample);

		// creating needed blocks
		if (createBlock)
			createBlock(sample, true, naming);

		if (naming)
			sample.updateAllNames();

		genericDAO.savePatientData(sample, "log.patient.task.sample.update", sample);

		// creating first default diagnosis
		diagnosisService.synchronizeDiagnosesAndSamples(task);
	}

	/**
	 * Deletes a sample and returns true is staining phase is completed
	 * 
	 * @param block
	 * @return
	 */
	public boolean deleteSample(Sample sampel) {
		logger.debug("Deleting sample (" + sampel.getId() + ")");
		Task parent = sampel.getParent();

		parent.getSamples().remove(sampel);

		parent.getSamples().forEach(p -> p.updateAllNames());

		genericDAO.savePatientData(parent, "log.patient.task.update", parent.toString());

		// creating first default diagnosis
		diagnosisService.synchronizeDiagnosesAndSamples(parent);

		genericDAO.deletePatientData(sampel, "log.patient.task.sample.delete", sampel.toString());

		return updateStaingPhase(parent);
	}

	/**
	 * Creates a block for a sample, check the staining phase, and updates all names
	 * 
	 * @param sample
	 */
	public void createBlockForSample(Sample sample) {
		// do not update
		createBlock(sample, true, false);

		sample.getBlocks().forEach(p -> p.updateAllNames());

		updateStaingPhase(sample.getTask());

		// saving patient
		genericDAO.savePatientData(sample, "log.patient.task.sample.update", sample.toString());

	}

	/**
	 * Creates a block for the given sample. Adds all slides from the material
	 * preset to the block.
	 * 
	 * @param sample
	 * @param material
	 */
	public Block createBlock(Sample sample) {
		return createBlock(sample, true, true);
	}

	/**
	 * Creates a block for the given sample. Adds all slides from the material
	 * preset to the block if createSlides is true.
	 * 
	 * @param sample
	 * @param material
	 */
	public Block createBlock(Sample sample, boolean createSlides, boolean naming) {
		Block block = new Block();
		block.setParent(sample);
		sample.getBlocks().add(block);

		genericDAO.savePatientData(block, "log.patient.task.sample.blok.new", block.getBlockID());

		logger.debug("Creating new block " + block.getBlockID());

		if (createSlides) {
			for (StainingPrototype proto : sample.getMaterialPreset().getStainingPrototypes()) {
				createSlide(proto, block, "", false, naming, false);
			}
		}

		if (naming)
			block.updateAllNames(sample.getTask().isUseAutoNomenclature(), false);

		genericDAO.savePatientData(block, "log.patient.task.sample.block.update", block.getBlockID());

		return block;
	}

	/**
	 * Deletes a block and returns true is staining phase is completed
	 * 
	 * @param block
	 * @return
	 */
	public boolean deleteBlock(Block block) {
		Sample parent = block.getParent();

		parent.getBlocks().remove(block);

		parent.getBlocks().forEach(p -> p.updateAllNames());

		genericDAO.savePatientData(parent, "log.patient.task.sample.update", parent.toString());

		genericDAO.deletePatientData(block, "log.patient.task.sample.block.delete", block.toString());

		return updateStaingPhase(parent.getTask());
	}

	/**
	 * Creates slides for a sample, also updates the phase settings of the given
	 * task.
	 * 
	 * @param slidesToCreate
	 * @param block
	 * @param commentary
	 * @param restaining
	 */
	public void createSlidesForSample(SlideSelectResult slideSelectResult) {
		createSlidesForSample(slideSelectResult.getPrototpyes(), slideSelectResult.getBlock(),
				slideSelectResult.getCommentary(), slideSelectResult.isRestaining(), slideSelectResult.isAsCompleted());
	}

	/**
	 * Creates slides for a sample, also updates the phase settings of the given
	 * task.
	 * 
	 * @param slidesToCreate
	 * @param block
	 * @param commentary
	 * @param restaining
	 */
	public void createSlidesForSample(List<StainingPrototype> slidesToCreate, Block block, String commentary,
			boolean restaining, boolean asCompleted) {

		if (slidesToCreate.size() == 0)
			return;

		// creating slides
		slidesToCreate.forEach(p -> createSlide(p, block, commentary, restaining, true, asCompleted));
		// updating staining phase
		updateStaingPhase(block.getTask());

	}

	/**
	 * Adds a new staining to a block. Sets the staining completion time to 0.
	 * 
	 * @param prototype
	 * @param block
	 */
	public void createSlide(StainingPrototype prototype, Block block) {
		createSlide(prototype, block, null, false, true, false);
	}

	/**
	 * Adds a new staining to a block.
	 * 
	 * @param prototype
	 * @param sample
	 * @param block
	 * @param commentary
	 * @param patientOfSample
	 */
	public void createSlide(StainingPrototype prototype, Block block, String commentary, boolean reStaining,
			boolean naming, boolean asCompleted) {
		logger.debug("Creating new slide " + prototype.getName());

		Slide slide = new Slide();

		slide.setCreationDate(System.currentTimeMillis());
		slide.setSlidePrototype(prototype);
		slide.setParent(block);

		// setting unique slide number
		slide.setUniqueIDinTask(block.getTask().getNextSlideNumber());

		block.getSlides().add(slide);

		if (naming)
			slide.updateNameOfSlide(block.getTask().isUseAutoNomenclature(), false);

		if (commentary != null && !commentary.isEmpty())
			slide.setCommentary(commentary);

		slide.setReStaining(reStaining);

		if(asCompleted) {
			slide.setCompletionDate(System.currentTimeMillis());
			slide.setStainingCompleted(true);
		}
		
		genericDAO.savePatientData(slide, "log.patient.task.sample.block.slide.new", slide.toString());
	}

	/**
	 * Deletes an slide an returns true if the staining phase is completed
	 * 
	 * @param slide
	 * @return
	 */
	public boolean deleteSlide(Slide slide) {
		Block parent = slide.getParent();

		parent.getSlides().remove(slide);

		parent.getSlides().forEach(p -> p.updateAllNames());

		genericDAO.savePatientData(parent, "log.patient.task.sample.block.update", parent.toString());

		genericDAO.deletePatientData(slide, "log.patient.task.sample.block.slide.delete", slide.toString());

		// checking if staining flag of the task object has to be false
		return updateStaingPhase(parent.getTask());
	}

	/**
	 * Updates the status of the stating phase, if the phase has ended, true will be
	 * returned.
	 * 
	 * @param task
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	public boolean updateStaingPhase(Task task) throws HistoDatabaseInconsistentVersionException {
		// removing from staining list and showing the dialog for ending
		// staining phase
		if (TaskStatus.checkIfStainingCompleted(task)) {
			logger.trace("Staining phase of task (" + task.getTaskID()
					+ ") completed removing from staing list, adding to diagnosisList");
			return true;
		} else {

			// reentering the staining phase, adding task to staining or
			// restaining list
			logger.trace("Enter staining phase, adding to staingin list");

			startStainingPhase(task);

			return false;
		}
	}

	/**
	 * Sets all slides of a task to staining completed/not completed
	 * 
	 * @param task
	 * @param completed
	 * @return
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	public boolean setStainingCompletedForSlides(Task task, boolean completed)
			throws HistoDatabaseInconsistentVersionException {

		boolean changed = false;

		for (Sample sample : task.getSamples()) {
			for (Block block : sample.getBlocks()) {
				if (setStainingCompletedForSlides(block.getSlides(), completed))
					changed = true;
			}
		}
		return changed;
	}

	/**
	 * Sets a lists of slides to completed/not completed
	 * 
	 * @param slides
	 * @param completed
	 * @return
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	public boolean setStainingCompletedForSlides(List<Slide> slides, boolean completed)
			throws HistoDatabaseInconsistentVersionException {

		boolean changed = false;

		for (Slide slide : slides) {
			if (setStainingCompletedForSlide(slide, completed))
				changed = true;
		}

		return changed;
	}

	/**
	 * Sets the stainin status of a slide as completed if not done jet
	 * 
	 * @param slide
	 * @param completed
	 * @return
	 * @throws HistoDatabaseInconsistentVersionException
	 */
	public boolean setStainingCompletedForSlide(Slide slide, boolean completed)
			throws HistoDatabaseInconsistentVersionException {

		if (slide.isStainingCompleted() != completed) {
			slide.setStainingCompleted(completed);
			slide.setCompletionDate(System.currentTimeMillis());

			genericDAO.savePatientData(slide, completed ? "log.patient.task.sample.blok.slide.stainingPerformed"
					: "log.patient.task.sample.blok.slide.stainingNotPerformed", slide.toString());
			return true;
		}

		return false;
	}

	/**
	 * Ends the staining phase, removes the task from the staining lists, sets
	 * staining completion date to current time
	 * 
	 * Error-Handling via global Error-Handler
	 * 
	 * @param task
	 */
	public void endStainingPhase(Task task, boolean removeFromList) {
		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					setStainingCompletedForSlides(task, true);

					task.setStainingCompletionDate(System.currentTimeMillis());

					genericDAO.savePatientData(task, "log.patient.task.phase.staining.end");

					if (removeFromList)
						favouriteListDAO.removeReattachedTaskFromList(task, PredefinedFavouriteList.StainingList,
								PredefinedFavouriteList.ReStainingList);
				}
			});
		} catch (Exception e) {
			throw new HistoDatabaseInconsistentVersionException(task);
		}
	}

	/**
	 * Start the staining phase, adds the task to the staining or restaining phase,
	 * set staining completion date to 0
	 * 
	 * Error-Handling via global Error-Handler
	 * 
	 * @param task
	 */
	public void startStainingPhase(Task task) {
		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					task.setStainingCompletionDate(0);

					genericDAO.savePatientData(task, "log.patient.task.phase.staining.enter");

					if (!task.isListedInFavouriteList(PredefinedFavouriteList.StainingList,
							PredefinedFavouriteList.ReStainingList)) {
						if (TaskStatus.checkIfReStainingFlag(task)) {
							favouriteListDAO.removeReattachedTaskFromList(task, PredefinedFavouriteList.StainingList);
							favouriteListDAO.addReattachedTaskToList(task, PredefinedFavouriteList.ReStainingList);
						} else
							favouriteListDAO.addReattachedTaskToList(task, PredefinedFavouriteList.StainingList);
					}
				}
			});
		} catch (Exception e) {
			throw new HistoDatabaseInconsistentVersionException(task);
		}
	}

	/**
	 * Changes the material of an sample
	 * 
	 * Error-Handling via global Error-Handler
	 * 
	 * @param sample
	 * @param materialPreset
	 */
	public void changeMaterialOfSample(Sample sample, MaterialPreset materialPreset) {
		sample.setMaterial(materialPreset.getName());
		sample.setMaterialPreset(materialPreset);

		genericDAO.savePatientData(sample, "log.patient.task.sample.material.update", materialPreset.toString());
	}
}