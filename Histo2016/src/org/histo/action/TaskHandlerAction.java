package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.dialog.MediaDialogHandler;
import org.histo.action.dialog.SettingsDialogHandler;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.Role;
import org.histo.config.enums.StaticList;
import org.histo.config.enums.TaskPriority;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.Council;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.IdManuallyAltered;
import org.histo.model.patient.Block;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.TimeUtil;
import org.histo.util.printer.PrintTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskHandlerAction implements Serializable {

	private static final long serialVersionUID = -1460063099758733063L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Lazy
	private WorklistHandlerAction worklistHandlerAction;


	@Autowired
	@Lazy
	private SettingsDialogHandler settingsDialogHandler;

	@Autowired
	@Lazy
	private MediaDialogHandler mediaDialogHandler;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;
	
	@Autowired
	private UtilDAO utilDAO;
	/********************************************************
	 * Task creation
	 ********************************************************/

	/**
	 * Transformer for selecting staininglist
	 */
	private StainingListTransformer materialListTransformer;

	/**
	 * Temporary task for creating samples
	 */
	private Task temporaryTask;

	/**
	 * sample count for temporary task, used by p:spinner in the createTask
	 * dialog
	 */
	private int temporaryTaskSampleCount;

	/**
	 * True if the user change the useAutoNomeclature setting manually
	 */
	private boolean autoNomenclatureChangedManually;

	/********************************************************
	 * Task creation
	 ********************************************************/

	/********************************************************
	 * Sample creation
	 ********************************************************/
	/**
	 * Used to save a sample while changing the material
	 */
	private Sample temporarySample;

	/**
	 * Used to save the material while creating a new sample / changing the
	 * material
	 */
	private MaterialPreset selectedMaterial;
	/********************************************************
	 * Sample creation
	 ********************************************************/

	/********************************************************
	 * Task
	 ********************************************************/
	/**
	 * List of all physicians known in the database
	 */
	private DefaultTransformer<Physician> allAvailablePhysiciansTransformer;

	/**
	 * Contains all available case histories
	 */
	private List<ListItem> caseHistoryList;

	/**
	 * Contains all available wards
	 */
	private List<ListItem> wardList;

	/********************************************************
	 * Task
	 ********************************************************/

	/********************************************************
	 * Delete
	 ********************************************************/
	/**
	 * Temporary save for a task tree entity (sample, slide, block)
	 */
	private DeleteAble taskTreeEntityToDelete;
	/********************************************************
	 * Delete
	 ********************************************************/

	/********************************************************
	 * DiagnosisRevision
	 ********************************************************/
	
	/**
	 * List of physicians which have the role signature
	 */
	private List<Physician> physiciansToSignList;
	
	/**
	 * Transfomer for physiciansToSign
	 */
	private DefaultTransformer<Physician> physiciansToSignListTransformer;
	
	/**
	 * Selected physician to sign the report
	 */
	private Physician signatureOne;

	/**
	 * Selected consultant to sign the report
	 */
	private Physician signatureTwo;

	/********************************************************
	 * DiagnosisRevision
	 ********************************************************/

	public void initBean() {
		// init materials in settingshandlerAction
		settingsDialogHandler.initMaterialPresets();
		setMaterialListTransformer(new StainingListTransformer(settingsDialogHandler.getAllAvailableMaterials()));

		
		setPhysiciansToSignList(physicianDAO.getPhysicians(ContactRole.SIGNATURE, false));
		setPhysiciansToSignListTransformer(new DefaultTransformer<Physician>(getPhysiciansToSignList()));
	}

	/********************************************************
	 * Task
	 ********************************************************/
	public void prepareTask(Task task) {
		initBean();

		task = genericDAO.refresh(task);

		// setting the report time to the current date
		if (task.isDiagnosisPhase()) {
			task.getDiagnosisContainer().setSignatureDate(TimeUtil.setDayBeginning(System.currentTimeMillis()));
			if (task.getDiagnosisContainer().getSignatureOne().getPhysician() == null
					|| task.getDiagnosisContainer().getSignatureTwo().getPhysician() == null) {
				// TODO set if physician to the left, if consultant to the right
			}
		}

		// loading lists
		setCaseHistoryList(settingsDAO.getAllStaticListItems(StaticList.CASE_HISTORY));
		setWardList(settingsDAO.getAllStaticListItems(StaticList.WARDS));

		setSignatureOne(task.getDiagnosisContainer().getSignatureOne().getPhysician());
		setSignatureTwo(task.getDiagnosisContainer().getSignatureTwo().getPhysician());
	}


	/********************************************************
	 * Task
	 ********************************************************/
	public void manuallyAltered(IdManuallyAltered idManuallyAltered, boolean altered){
		idManuallyAltered.setIdManuallyAltered(altered);
		
		genericDAO.saveDataRollbackSave(idManuallyAltered, "log.patient.task.idManuallyAltered");
		logger.debug("Manually altered " + altered);
	}
	/********************************************************
	 * Sample creation from Gui
	 ********************************************************/

	/**
	 * Displays a dialog for creating a new sample
	 */
	public void prepareNewSampleDialog(Task task) {

		settingsDialogHandler.initMaterialPresets();
		// checks if default statingsList is empty
		if (!settingsDialogHandler.getAllAvailableMaterials().isEmpty()) {
			// setSelectedMaterial(settingsDialogHandler.getAllAvailableMaterials().get(0));
			setMaterialListTransformer(new StainingListTransformer(settingsDialogHandler.getAllAvailableMaterials()));
		}
		setTemporaryTask(task);

		// more the one task = use autonomeclature
		if (task.getSamples().size() > 0)
			task.setUseAutoNomenclature(true);

		mainHandlerAction.showDialog(Dialog.SAMPLE_CREATE);
	}

	/**
	 * Hides the dialog for creating new samples
	 */
	public void hideNewSampleDialog() {
		setTemporaryTask(null);
		mainHandlerAction.hideDialog(Dialog.SAMPLE_CREATE);
	}

	/**
	 * Method used by the gui for creating a new sample
	 * 
	 * @param task
	 * @param material
	 */
	public void createNewSampleFromGui(Task task, MaterialPreset material) {
		taskManipulationHandler.createNewSample(task, material);

		// updating names
		task.updateAllNames();
		// checking if staining flag of the task object has to be false
		task.getStatus().updateStainingStatus();
		// generating gui list
		task.generateSlideGuiList();
		// saving patient

		hideNewSampleDialog();
	}

	/**
	 * Shows a dialog for changing the material of a sample
	 */
	public void prepareSelectMaterialDialog(Sample sample) {
		setTemporarySample(sample);
		setSelectedMaterial(sample.getMaterilaPreset());
		mainHandlerAction.showDialog(Dialog.SELECT_MATERIAL);
	}

	/**
	 * Changes the material of the sample to the given material.
	 * 
	 * @param sample
	 * @param materialPreset
	 */
	public void changeSelectedMaterial(Sample sample, MaterialPreset materialPreset) {
		sample.setMaterial(materialPreset.getName());
		sample.setMaterilaPreset(materialPreset);

		genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.material.update",
				sample.getParent().getTaskID(), sample.getSampleID(), materialPreset.getName()),
				sample.getParent().getPatient());

		hideSelectMaterialDialog();
	}

	/**
	 * Hides the change material dialog
	 */
	public void hideSelectMaterialDialog() {
		setTemporarySample(null);
		mainHandlerAction.hideDialog(Dialog.SELECT_MATERIAL);
	}

	/********************************************************
	 * Sample creation from Gui
	 ********************************************************/

	/********************************************************
	 * Create Block from Gui
	 ********************************************************/
	/**
	 * Method used by the gui for creating a new sample
	 * 
	 * @param task
	 * @param material
	 */
	public void createNewBlockFromGui(Sample sample) {
		taskManipulationHandler.createNewBlock(sample, false);

		// updates the name of all other samples
		for (Block block : sample.getBlocks()) {
			block.updateNameOfBlock(sample.getParent().isUseAutoNomenclature());
		}

		// checking if staining flag of the task object has to be false
		sample.getParent().getStatus().updateStainingStatus();
		// generating gui list
		sample.getParent().generateSlideGuiList();
		// saving patient
		genericDAO.save(sample.getPatient(), resourceBundle.get("log.patient.save"), sample.getPatient());
	}

	/********************************************************
	 * Create Block from Gui
	 ********************************************************/

	/********************************************************
	 * Task Data
	 ********************************************************/
	public void prepareDeleteTaskTreeEntityDialog(DeleteAble toDelete) {
		setTaskTreeEntityToDelete(toDelete);
		mainHandlerAction.showDialog(Dialog.DELETE_TREE_ENTITY);
	}

	public void deleteTaskTreeEntity(DeleteAble toDelete) {
		if (toDelete instanceof Slide) {
			Slide toDeleteSlide = (Slide) toDelete;

			logger.info("Deleting slide " + toDeleteSlide.getSlideID());

			Block parent = toDeleteSlide.getParent();

			parent.getSlides().remove(toDeleteSlide);

			parent.updateAllNames(parent.getParent().getParent().isUseAutoNomenclature());

			genericDAO.save(parent,
					resourceBundle.get("log.patient.task.sample.block.update",
							parent.getParent().getParent().getTaskID(), parent.getParent().getSampleID(),
							parent.getBlockID()),
					parent.getPatient());

			genericDAO.delete(toDeleteSlide,
					resourceBundle.get("log.patient.task.sample.block.slide.update",
							parent.getParent().getParent().getTaskID(), parent.getParent().getSampleID(),
							parent.getBlockID(), toDeleteSlide.getSlideID()),
					parent.getPatient());

			// checking if staining flag of the task object has to be false
			parent.getParent().getParent().getStatus().updateStainingStatus();
			// generating gui list
			parent.getParent().getParent().generateSlideGuiList();

		} else if (toDelete instanceof Block) {
			Block toDeleteBlock = (Block) toDelete;
			logger.info("Deleting block " + toDeleteBlock.getBlockID());

			Sample parent = toDeleteBlock.getParent();

			parent.getBlocks().remove(toDeleteBlock);

			parent.updateAllNames(parent.getParent().isUseAutoNomenclature());

			genericDAO.save(parent, resourceBundle.get("log.patient.task.sample.update", parent.getParent().getTaskID(),
					parent.getSampleID()), parent.getPatient());

			genericDAO.delete(
					toDeleteBlock, resourceBundle.get("log.patient.task.sample.block.delete",
							parent.getParent().getId(), parent.getSampleID(), toDeleteBlock.getBlockID()),
					toDeleteBlock.getPatient());

			// checking if staining flag of the task object has to be false
			parent.getParent().getStatus().updateStainingStatus();
			// generating gui list
			parent.getParent().generateSlideGuiList();

		} else if (toDelete instanceof Sample) {
			Sample toDeleteSample = (Sample) toDelete;
			logger.info("Deleting sample " + toDeleteSample.getSampleID());

			Task parent = toDeleteSample.getParent();

			parent.getSamples().remove(toDeleteSample);

			taskManipulationHandler.updateDiagnosisContainerToSampleCount(parent.getDiagnosisContainer(),
					parent.getSamples());

			parent.updateAllNames();

			genericDAO.save(parent, resourceBundle.get("log.patient.task.update", parent.getId()), parent.getPatient());

			genericDAO.delete(toDeleteSample,
					resourceBundle.get("log.patient.task.sample.delete", parent.getId(), toDeleteSample.getSampleID()),
					toDeleteSample.getParent().getPatient());

			// checking if staining flag of the task object has to be false
			parent.getStatus().updateStainingStatus();
			// generating gui list
			parent.generateSlideGuiList();
		}

		hideDeleteTaskTreeEntityDialog();
	}

	public void hideDeleteTaskTreeEntityDialog() {
		setTaskTreeEntityToDelete(null);
		mainHandlerAction.hideDialog(Dialog.DELETE_TREE_ENTITY);
	}

	/********************************************************
	 * Task Data
	 ********************************************************/

	/********************************************************
	 * Task Admin
	 ********************************************************/
	public void showAdministrateTask(Task task) {
		setTemporaryTask(task);
		mainHandlerAction.showDialog(Dialog.ADMINISTRATE_TASK);
	}

	public void saveAdministrateTask() {
		genericDAO.save(getTemporaryTask());
		hideAdministrateTask();
	}

	public void hideAdministrateTask() {
		genericDAO.refresh(getTemporaryTask());
		setTemporaryTask(null);
		mainHandlerAction.hideDialog(Dialog.ADMINISTRATE_TASK);
	}

	/********************************************************
	 * Task Admin
	 ********************************************************/

	/********************************************************
	 * Task editable
	 ********************************************************/
	public boolean isTaskEditable(Task task) {

		if (task == null)
			return false;

		// users and guest can't edit anything
		if (!userHandlerAction.currentUserHasRoleOrHigher(Role.MTA)) {
			logger.debug("Task not editable, user has no permission");
			return false;
		}

		// finalized
		if (task.isFinalized()) {
			logger.debug("Task not editable, is finalized");
			return false;
		}
		
		if(task.getStatus().isDiagnosisPerformed() && task.getStatus().isStainingPerformed())
			return false;

		// Blocked
		// TODO: Blocking

		logger.debug("Task is editable");
		return true;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public StainingListTransformer getMaterialListTransformer() {
		return materialListTransformer;
	}

	public void setMaterialListTransformer(StainingListTransformer materialListTransformer) {
		this.materialListTransformer = materialListTransformer;
	}

	public Task getTemporaryTask() {
		return temporaryTask;
	}

	public void setTemporaryTask(Task temporaryTask) {
		this.temporaryTask = temporaryTask;
	}

	public int getTemporaryTaskSampleCount() {
		return temporaryTaskSampleCount;
	}

	public void setTemporaryTaskSampleCount(int temporaryTaskSampleCount) {
		this.temporaryTaskSampleCount = temporaryTaskSampleCount;
	}

	public DefaultTransformer<Physician> getAllAvailablePhysiciansTransformer() {
		return allAvailablePhysiciansTransformer;
	}

	public void setAllAvailablePhysiciansTransformer(DefaultTransformer<Physician> allAvailablePhysiciansTransformer) {
		this.allAvailablePhysiciansTransformer = allAvailablePhysiciansTransformer;
	}

	public Physician getSignatureOne() {
		return signatureOne;
	}

	public Physician getSignatureTwo() {
		return signatureTwo;
	}

	public void setSignatureOne(Physician signatureOne) {
		this.signatureOne = signatureOne;
	}

	public void setSignatureTwo(Physician signatureTwo) {
		this.signatureTwo = signatureTwo;
	}

	public boolean isAutoNomenclatureChangedManually() {
		return autoNomenclatureChangedManually;
	}

	public void setAutoNomenclatureChangedManually(boolean autoNomenclatureChangedManually) {
		this.autoNomenclatureChangedManually = autoNomenclatureChangedManually;
	}

	public MaterialPreset getSelectedMaterial() {
		return selectedMaterial;
	}

	public void setSelectedMaterial(MaterialPreset selectedMaterial) {
		this.selectedMaterial = selectedMaterial;
	}

	public Sample getTemporarySample() {
		return temporarySample;
	}

	public void setTemporarySample(Sample temporarySample) {
		this.temporarySample = temporarySample;
	}

	public List<ListItem> getCaseHistoryList() {
		return caseHistoryList;
	}

	public void setCaseHistoryList(List<ListItem> caseHistoryList) {
		this.caseHistoryList = caseHistoryList;
	}

	public List<ListItem> getWardList() {
		return wardList;
	}

	public void setWardList(List<ListItem> wardList) {
		this.wardList = wardList;
	}

	public DeleteAble getTaskTreeEntityToDelete() {
		return taskTreeEntityToDelete;
	}

	public void setTaskTreeEntityToDelete(DeleteAble taskTreeEntityToDelete) {
		this.taskTreeEntityToDelete = taskTreeEntityToDelete;
	}

	public List<Physician> getPhysiciansToSignList() {
		return physiciansToSignList;
	}

	public DefaultTransformer<Physician> getPhysiciansToSignListTransformer() {
		return physiciansToSignListTransformer;
	}

	public void setPhysiciansToSignList(List<Physician> physiciansToSignList) {
		this.physiciansToSignList = physiciansToSignList;
	}

	public void setPhysiciansToSignListTransformer(DefaultTransformer<Physician> physiciansToSignListTransformer) {
		this.physiciansToSignListTransformer = physiciansToSignListTransformer;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
