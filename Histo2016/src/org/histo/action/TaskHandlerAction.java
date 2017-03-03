package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.StaticList;
import org.histo.config.enums.TaskPriority;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Council;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.patient.Block;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskHandlerAction implements Serializable {

	private static final long serialVersionUID = -1460063099758733063L;

	private static Logger logger = Logger.getLogger(TaskHandlerAction.class);

	@Autowired
	private HelperDAO helperDAO;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private PatientDao patientDao;
	
	@Autowired
	private DiagnosisHandlerAction diagnosisHandlerAction;

	@Autowired
	private SlideHandlerAction slideHandlerAction;

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
	private PrintHandlerAction printHandlerAction;

	@Autowired
	@Lazy
	private SettingsHandlerAction settingsHandlerAction;

	@Autowired
	@Lazy
	private MediaHandlerAction mediaHandlerAction;
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
	 * Council
	 ********************************************************/

	/**
	 * Temporary object for council dialog
	 */
	private Council temporaryCouncil;

	/**
	 * Converter for selecting councils
	 */
	private DefaultTransformer<Council> councilConverter;
	/********************************************************
	 * Council
	 ********************************************************/

	/********************************************************
	 * DiagnosisRevision
	 ********************************************************/
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
		settingsHandlerAction.initMaterialPresets();
		setMaterialListTransformer(new StainingListTransformer(settingsHandlerAction.getAllAvailableMaterials()));

		settingsHandlerAction.setPhysicianList(physicianDAO.getPhysicians(ContactRole.values(), false));
		setAllAvailablePhysiciansTransformer(
				new DefaultTransformer<Physician>(settingsHandlerAction.getPhysicianList()));
	}

	/********************************************************
	 * Task
	 ********************************************************/
	public void prepareTask(Task task) {
		initBean();

		taskDAO.initializeDiagnosisData(task);

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

	/**
	 * Displays a dialog for creating a new task
	 */
	public void prepareNewTaskDialog() {
		initBean();

		setTemporaryTask(new Task(worklistHandlerAction.getSelectedPatient()));
		getTemporaryTask().setTaskID(Integer.toString(TimeUtil.getCurrentYear() - 2000)
				+ HistoUtil.fitString(taskDAO.countSamplesOfCurrentYear(), 4, '0'));
		getTemporaryTask().setTaskPriority(TaskPriority.NONE);
		getTemporaryTask().setDateOfReceipt(TimeUtil.setDayBeginning(System.currentTimeMillis()));
		setTemporaryTaskSampleCount(1);

		getTemporaryTask().setUseAutoNomenclature(true);
		setAutoNomenclatureChangedManually(false);

		// resetting selected pdf container for informed consent upload
		mediaHandlerAction.setSelectedPdfContainer(null);
		
		// creates a new sample
		new Sample(getTemporaryTask(), !settingsHandlerAction.getAllAvailableMaterials().isEmpty()
				? settingsHandlerAction.getAllAvailableMaterials().get(0) : null);

		mainHandlerAction.showDialog(Dialog.TASK_CREATE);
	}

	/**
	 * Method is called if user adds or removes a sample within the task
	 * creation process. Adds or removes a new Material for the new Sample.
	 */
	public void updateNewTaskDilaog(Task task) {
		logger.debug("Updating sample tree");
		// changing autoNomeclature of samples, if no change was made manually
		if (temporaryTaskSampleCount > 1 && !isAutoNomenclatureChangedManually()) {
			logger.debug("Setting autonomeclature to true");
			getTemporaryTask().setUseAutoNomenclature(true);
		} else if (temporaryTaskSampleCount == 1 && !isAutoNomenclatureChangedManually()) {
			logger.debug("Setting autonomeclature to false");
			getTemporaryTask().setUseAutoNomenclature(false);
		}

		if (temporaryTaskSampleCount >= 1) {
			if (temporaryTaskSampleCount > task.getSamples().size()) {
				logger.debug("Adding new samples");
				// adding samples if count is bigger then the current sample
				// count
				while (temporaryTaskSampleCount > task.getSamples().size())
					new Sample(task, !settingsHandlerAction.getAllAvailableMaterials().isEmpty()
							? settingsHandlerAction.getAllAvailableMaterials().get(0) : null);
			} else if (temporaryTaskSampleCount < task.getSamples().size()) {
				logger.debug("Removing samples");
				// removing samples if count is less then current sample count
				while (temporaryTaskSampleCount < task.getSamples().size())
					task.getSamples().remove(task.getSamples().size() - 1);
			}
		}

		logger.debug("Updating sample names");
		// updates the name of all other samples
		for (Sample sample : task.getSamples()) {
			sample.updateNameOfSample(getTemporaryTask().isUseAutoNomenclature());
		}
	}

	public void manuallyChangeAutoNomenclature() {
		logger.debug("Autonomeclature change manually");
		setAutoNomenclatureChangedManually(true);
		updateNewTaskDilaog(getTemporaryTask());
	}

	/**
	 * Creates a new task for the given Patient
	 * 
	 * @param patient
	 */
	public Task createNewTask(Patient patient, Task task) {
		if (patient.getTasks() == null) {
			patient.setTasks(new ArrayList<>());
		}

		patient.getTasks().add(0, task);
		// sets the new task as the selected task
		patient.setSelectedTask(task);

		genericDAO.save(task, resourceBundle.get("log.patient.task.new", task.getTaskID()), patient);

		task.setDiagnosisContainer(new DiagnosisContainer(task));
		task.getDiagnosisContainer().setDiagnosisRevisions(new ArrayList<DiagnosisRevision>());

		// setting signature
		task.getDiagnosisContainer().setSignatureOne(new Signature());
		task.getDiagnosisContainer().setSignatureTwo(new Signature());

		task.setCaseHistory("");
		task.setWard("");

		task.setCouncils(new ArrayList<Council>());

		genericDAO.save(task.getDiagnosisContainer(),
				resourceBundle.get("log.patient.task.diagnosisContainer.new", task.getTaskID()), patient);

		for (Sample sample : task.getSamples()) {
			// set name of material for changing it manually
			sample.setMaterial(sample.getMaterilaPreset().getName());

			genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.new", task.getTaskID(),
					sample.getSampleID(), sample.getMaterial()), task.getPatient());

			// creating needed blocks
			createNewBlock(sample, false);
		}

		// standard diagnose erstellen
		diagnosisHandlerAction.createDiagnosisRevision(task.getDiagnosisContainer(), DiagnosisRevisionType.DIAGNOSIS);

		// checking if staining flag of the task object has to be false
		task.updateStainingStatus();
		// generating gui list
		task.generateSlideGuiList();
		// saving patient

		genericDAO.save(task.getPatient(), resourceBundle.get("log.patient.save"), task.getPatient());

		mainHandlerAction.hideDialog(Dialog.TASK_CREATE);
		
		return task;
	}
	
	public void createNewTaskAndPrintUReport(Patient patient, Task task) {
		createNewTask(patient,task);
		
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.U_REPORT });
		
		if(subSelect.length == 0){
			logger.debug("No Template for UReport found");
			return;
		}
		
		printHandlerAction.printPdfFromExternalBean(subSelect[0]);
	}

	/********************************************************
	 * Task
	 ********************************************************/

	/********************************************************
	 * Sample
	 ********************************************************/

	/**
	 * Displays a dialog for creating a new sample
	 */
	public void prepareNewSampleDialog(Task task) {

		settingsHandlerAction.initMaterialPresets();
		// checks if default statingsList is empty
		if (!settingsHandlerAction.getAllAvailableMaterials().isEmpty()) {
			// setSelectedMaterial(settingsHandlerAction.getAllAvailableMaterials().get(0));
			setMaterialListTransformer(new StainingListTransformer(settingsHandlerAction.getAllAvailableMaterials()));
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
		createNewSample(task, material);

		// updating names
		task.updateAllNames();
		// checking if staining flag of the task object has to be false
		task.updateStainingStatus();
		// generating gui list
		task.generateSlideGuiList();
		// saving patient

		hideNewSampleDialog();
	}

	/**
	 * Creates a new sample and adds this sample to the given task. Creates a
	 * new diagnosis and a new block with slides as well.
	 * 
	 * @param task
	 */
	public void createNewSample(Task task, MaterialPreset material) {
		Sample sample = new Sample(task, material);

		genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.new", task.getTaskID(),
				sample.getSampleID(), material.getName()), task.getPatient());

		// creating needed blocks
		createNewBlock(sample, false);

		genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.update", sample.getParent().getTaskID(),
				sample.getSampleID()), sample.getPatient());

		// creating first default diagnosis
		diagnosisHandlerAction.updateDiagnosisContainerToSampleCount(task.getDiagnosisContainer(), task.getSamples());
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
	 * Sample
	 ********************************************************/

	/********************************************************
	 * Block
	 ********************************************************/

	/**
	 * Method used by the gui for creating a new sample
	 * 
	 * @param task
	 * @param material
	 */
	public void createNewBlockFromGui(Sample sample) {
		createNewBlock(sample, false);

		// updates the name of all other samples
		for (Block block : sample.getBlocks()) {
			block.updateNameOfBlock(sample.getParent().isUseAutoNomenclature());
		}

		// checking if staining flag of the task object has to be false
		sample.getParent().updateStainingStatus();
		// generating gui list
		sample.getParent().generateSlideGuiList();
		// saving patient
		genericDAO.save(sample.getPatient(), resourceBundle.get("log.patient.save"), sample.getPatient());
	}

	/**
	 * Creates a new block for the given sample. Adds all slides from the
	 * material preset to the block.
	 * 
	 * @param sample
	 * @param material
	 */
	public Block createNewBlock(Sample sample, boolean useAutoNomenclature) {
		Block block = new Block();
		block.setBlockID(useAutoNomenclature ? TaskUtil.getCharNumber(sample.getBlocks().size()) : "");
		block.setParent(sample);
		sample.getBlocks().add(block);

		genericDAO.save(block, resourceBundle.get("log.patient.task.sample.blok.new",
				block.getParent().getParent().getTaskID(), block.getParent().getSampleID(), block.getBlockID()),
				sample.getPatient());

		logger.debug("Creating new block " + block.getBlockID());

		for (StainingPrototype proto : sample.getMaterilaPreset().getStainingPrototypes()) {
			slideHandlerAction.createSlide(proto, block);
		}

		genericDAO.save(
				block, resourceBundle.get("log.patient.task.sample.block.update",
						block.getParent().getParent().getTaskID(), block.getParent().getSampleID(), block.getBlockID()),
				sample.getPatient());

		return block;
	}

	/********************************************************
	 * Block
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
			parent.getParent().getParent().updateStainingStatus();
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
			parent.getParent().updateStainingStatus();
			// generating gui list
			parent.getParent().generateSlideGuiList();

		} else if (toDelete instanceof Sample) {
			Sample toDeleteSample = (Sample) toDelete;
			logger.info("Deleting sample " + toDeleteSample.getSampleID());

			Task parent = toDeleteSample.getParent();

			parent.getSamples().remove(toDeleteSample);

			diagnosisHandlerAction.updateDiagnosisContainerToSampleCount(parent.getDiagnosisContainer(), parent.getSamples());

			parent.updateAllNames();

			genericDAO.save(parent, resourceBundle.get("log.patient.task.update", parent.getId()), parent.getPatient());

			genericDAO.delete(toDeleteSample,
					resourceBundle.get("log.patient.task.sample.delete", parent.getId(), toDeleteSample.getSampleID()),
					toDeleteSample.getParent().getPatient());

			// checking if staining flag of the task object has to be false
			parent.updateStainingStatus();
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
	 * Council
	 ********************************************************/
	public void prepareCouncilDialog(Task task) {
		prepareCouncilDialog(task, true);
	}

	/**
	 * Prepares the council dialog
	 * 
	 * @param task
	 * @param show
	 */
	public void prepareCouncilDialog(Task task, boolean show) {
		setTemporaryTask(task);

		patientDao.initializeDataList(task);

		taskDAO.initializeCouncilData(task);
		
		// setting council as default
		if (task.getCouncils().size() == 0) {
			logger.debug("Creating new");
			setTemporaryCouncil(new Council());
			getTemporaryCouncil().setPhysicianRequestingCouncil(userHandlerAction.getCurrentUser().getPhysician());
		} else {
			// selected council is need for selectlist, temporary council is for
			// editing (new council can't be in task list)
			setTemporaryCouncil(task.getCouncils().get(0));
		}

		setCouncilConverter(new DefaultTransformer<Council>(task.getCouncils()));
		setAllAvailablePhysiciansTransformer(
				new DefaultTransformer<Physician>(settingsHandlerAction.getPhysicianList()));

		if (show)
			mainHandlerAction.showDialog(Dialog.COUNCIL);
	}

	public void addNewCouncil(Task task) {
		Council newCouncil = new Council();
		saveCouncilData(task, newCouncil);
		setTemporaryCouncil(newCouncil);
	}

	/**
	 * Saves a council, if not present the new council will be added to the
	 * council list
	 * 
	 * @param council
	 */
	public void saveCouncilData(Task task, Council council) {
		// new
		if (council.getId() == 0) {
			council.setDateOfRequest(System.currentTimeMillis());
			logger.debug("Creating new council");
			genericDAO.save(council, resourceBundle.get("log.patient.task.council.create", "TODO"), task.getPatient());

			System.out.println(council.getId() + ".-----------------------------------");
			task.getCouncils().add(council);

			mainHandlerAction.saveDataChange(task, "log.patient.task.council.attached",
					String.valueOf(council.getId()));
		} else {
			logger.debug("Saving council");
			// only saving
			genericDAO.save(council, resourceBundle.get("log.patient.task.council.update",
					String.valueOf(council.getId()), task.getTaskID()), task.getPatient());
		}
	}

	/**
	 * Hieds the council dialog
	 */
	public void hideCouncilDialog() {
		setTemporaryTask(null);
		setTemporaryCouncil(null);
		mainHandlerAction.hideDialog(Dialog.COUNCIL);
	}

	/**
	 * Hides the council dialog and opens the print dialog
	 * 
	 * @param print
	 */
	public void hideCouncilDialogAndPrintReport(Task task, Council council) {
		saveCouncilData(task, council);
		printHandlerAction.showCouncilPrintDialog(task, council);
		
		hideCouncilDialog();
		// workaround for showing and hiding two dialogues
		mainHandlerAction.setQueueDialog("#headerForm\\\\:printBtnShowOnly");

	}

	/********************************************************
	 * Council
	 ********************************************************/

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

	public DefaultTransformer<Council> getCouncilConverter() {
		return councilConverter;
	}

	public void setCouncilConverter(DefaultTransformer<Council> councilConverter) {
		this.councilConverter = councilConverter;
	}

	public Council getTemporaryCouncil() {
		return temporaryCouncil;
	}

	public void setTemporaryCouncil(Council temporaryCouncil) {
		this.temporaryCouncil = temporaryCouncil;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
