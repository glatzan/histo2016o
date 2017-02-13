package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.TaskPriority;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Council;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.LabelPrinter;
import org.histo.model.transitory.json.PdfTemplate;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.SlideUtil;
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
	private PdfHandlerAction pdfHandlerAction;

	@Autowired
	@Lazy
	private WorklistHandlerAction worklistHandlerAction;

	@Autowired
	private SettingsHandlerAction settingsHandlerAction;

	private HashMap<String, String> selectableWards;

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
	 * If true the program will name all samples, and blocks
	 */
	private boolean useAutoNomenclature;

	/**
	 * True if the user change the useAutoNomeclature setting manually
	 */
	private boolean autoNomenclatureChangedManually;

	/********************************************************
	 * Task creation
	 ********************************************************/

	/********************************************************
	 * Task
	 ********************************************************/
	/**
	 * List of all physicians known in the database
	 */
	private List<Physician> allAvailablePhysicians;

	/**
	 * List of all physicians known in the database
	 */
	private DefaultTransformer<Physician> allAvailablePhysiciansTransformer;
	/********************************************************
	 * Task
	 ********************************************************/

	/********************************************************
	 * Council
	 ********************************************************/

	/**
	 * Temporary object for council dialog
	 */
	private Council tmpCouncil;

	/********************************************************
	 * Council
	 ********************************************************/

	/********************************************************
	 * DiagnosisRevision
	 ********************************************************/
	/**
	 * Selected physician to sign the report
	 */
	private Physician physicianToSign;

	/**
	 * Selected consultant to sign the report
	 */
	private Physician consultantToSign;

	/********************************************************
	 * DiagnosisRevision
	 ********************************************************/

	/********************************************************
	 * Task
	 ********************************************************/

	public void initBean() {
		// init materials in settingshandlerAction
		settingsHandlerAction.initMaterialPresets();
		setMaterialListTransformer(new StainingListTransformer(settingsHandlerAction.getAllAvailableMaterials()));

		setAllAvailablePhysicians(physicianDAO.getPhysicians(ContactRole.values(), false));
		setAllAvailablePhysiciansTransformer(new DefaultTransformer<Physician>(getAllAvailablePhysicians()));

		// initis all wards
		if (selectableWards == null) {
			selectableWards = new HashMap<String, String>();
			selectableWards.put("none", resourceBundle.get("#{msg['body.receiptlog.ward.select']}"));
			selectableWards.put("ambulant", resourceBundle.get("#{msg['body.receiptlog.ward.ambulant']}"));
			selectableWards.put("impatient", resourceBundle.get("#{msg['body.receiptlog.ward.inpatient']}"));
			selectableWards.put("private", resourceBundle.get("#{msg['body.receiptlog.ward.ambulant.private']}"));
			selectableWards.put("impatient-private",
					resourceBundle.get("#{msg['body.receiptlog.ward.inpatient.private']}"));
			selectableWards.put("ims", resourceBundle.get("#{msg['body.receiptlog.ward.ims']}"));
			selectableWards.put("extern", resourceBundle.get("#{msg['body.receiptlog.ward.extern']}"));
			selectableWards.put("extern-private", resourceBundle.get("#{msg['body.receiptlog.ward.exern.private']}"));
			selectableWards.put("fda", resourceBundle.get("#{msg['body.receiptlog.ward.fda']}"));
			selectableWards.put("manz", resourceBundle.get("#{msg['body.receiptlog.ward.manz']}"));
			selectableWards.put("beck", resourceBundle.get("#{msg['body.receiptlog.ward.beck']}"));
			selectableWards.put("axenfeld", resourceBundle.get("#{msg['body.receiptlog.ward.axenfeld']}"));
		}
	}

	public void prepareTask(Task task) {
		initBean();

		taskDAO.initializeReportData(task);

		// setting the report time to the current date
		if (!task.isDiagnosisCompleted()) {
			task.setSignatureDate(TimeUtil.setDayBeginning(System.currentTimeMillis()));
		}

		setPhysicianToSign(task.getPhysicianSignature().getPhysician());
		setConsultantToSign(task.getConsultantSignature().getPhysician());
	}

	/**
	 * Displays a dialog for creating a new task
	 */
	public void prepareNewTaskDialog() {
		initBean();

		setTemporaryTask(new Task(worklistHandlerAction.getSelectedPatient()));
		getTemporaryTask().setTaskID(Integer.toString(TimeUtil.getCurrentYear() - 2000)
				+ HistoUtil.fitString(taskDAO.countSamplesOfCurrentYear(), 4, '0'));
		getTemporaryTask().setReports(new ArrayList<DiagnosisRevision>());
		getTemporaryTask().setTaskPriority(TaskPriority.NONE);
		getTemporaryTask().setDateOfReceipt(TimeUtil.setDayBeginning(System.currentTimeMillis()));
		setTemporaryTaskSampleCount(1);

		setUseAutoNomenclature(false);
		setAutoNomenclatureChangedManually(false);

		// creates a new sample
		new Sample(getTemporaryTask(), !settingsHandlerAction.getAllAvailableMaterials().isEmpty()
				? settingsHandlerAction.getAllAvailableMaterials().get(0) : null, isUseAutoNomenclature());

		mainHandlerAction.showDialog(Dialog.TASK_CREATE);
	}

	/**
	 * Method is called if user adds or removes a sample within the task
	 * creation process. Adds or removes a new Material for the new Sample.
	 */
	public void updateNewTaskDilaog(Task task, boolean useAutoNomenclature) {
		logger.debug("Updating sample tree");
		// changing autoNomeclature of samples, if no change was made manually
		if (temporaryTaskSampleCount > 1 && !isAutoNomenclatureChangedManually()) {
			logger.debug("Setting autonomeclature to true");
			setUseAutoNomenclature(true);
		} else if (temporaryTaskSampleCount == 1 && !isAutoNomenclatureChangedManually()) {
			logger.debug("Setting autonomeclature to false");
			setUseAutoNomenclature(false);
		}

		if (temporaryTaskSampleCount >= 1) {
			if (temporaryTaskSampleCount > task.getSamples().size()) {
				logger.debug("Adding new samples");
				// adding samples if count is bigger then the current sample
				// count
				while (temporaryTaskSampleCount > task.getSamples().size())
					new Sample(task,
							!settingsHandlerAction.getAllAvailableMaterials().isEmpty()
									? settingsHandlerAction.getAllAvailableMaterials().get(0) : null,
							useAutoNomenclature);
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
			sample.updateNameOfSample(isUseAutoNomenclature());
		}
	}

	public void manuallyChangeAutoNomenclature() {
		logger.debug("Autonomeclature change manually");
		setAutoNomenclatureChangedManually(true);
		updateNewTaskDilaog(getTemporaryTask(), isUseAutoNomenclature());
	}

	/**
	 * Creates a new task for the given Patient
	 * 
	 * @param patient
	 */
	public void createNewTask(Patient patient, Task task, boolean useAutoNomenclature) {
		if (patient.getTasks() == null) {
			patient.setTasks(new ArrayList<>());
		}

		patient.getTasks().add(0, task);
		// sets the new task as the selected task
		patient.setSelectedTask(task);

		// saving report to datanase
		genericDAO.save(task.getReports(), resourceBundle.get("log.patient.task.report.new", task.getTaskID()),
				task.getPatient());

		genericDAO.save(task, resourceBundle.get("log.patient.task.new", task.getTaskID()), patient);

		for (Sample sample : task.getSamples()) {
			sample.setMaterial(sample.getMaterilaPreset().getName());

			genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.new", task.getTaskID(),
					sample.getSampleID(), sample.getMaterial()), task.getPatient());

			// creating first default diagnosis
			diagnosisHandlerAction.createDiagnosis(sample, DiagnosisRevisionType.DIAGNOSIS);
			// creating needed blocks
			createNewBlock(sample, false);
		}

		// checking if staining flag of the task object has to be false
		task.updateStainingStatus();
		// generating gui list
		TaskUtil.generateSlideGuiList(task);
		// saving patient
		genericDAO.save(task.getPatient(), resourceBundle.get("log.patient.save"), task.getPatient());

		mainHandlerAction.hideDialog(Dialog.TASK_CREATE);
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
		// checks if default statingsList is empty
		if (!settingsHandlerAction.getAllAvailableMaterials().isEmpty()) {
			// setSelectedMaterial(settingsHandlerAction.getAllAvailableMaterials().get(0));
			setMaterialListTransformer(new StainingListTransformer(settingsHandlerAction.getAllAvailableMaterials()));
		}

		setTemporaryTask(task);

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

		// checking if staining flag of the task object has to be false
		task.updateStainingStatus();
		// generating gui list
		TaskUtil.generateSlideGuiList(task);
		// saving patient
		genericDAO.save(task.getPatient(), resourceBundle.get("log.patient.save"), task.getPatient());

		hideNewSampleDialog();
	}

	/**
	 * Creates a new sample and adds this sample to the given task. Creates a
	 * new diagnosis and a new block with slides as well.
	 * 
	 * @param task
	 */
	public void createNewSample(Task task, MaterialPreset material) {
		Sample sample = new Sample(task, material, false);

		genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.new", task.getTaskID(),
				sample.getSampleID(), material.getName()), task.getPatient());

		// creating first default diagnosis
		diagnosisHandlerAction.createDiagnosis(sample, DiagnosisRevisionType.DIAGNOSIS);
		// creating needed blocks
		createNewBlock(sample, false);
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

		// checking if staining flag of the task object has to be false
		sample.getParent().updateStainingStatus();
		// generating gui list
		TaskUtil.generateSlideGuiList(sample.getParent());
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
	public void createNewBlock(Sample sample, boolean useAutoNomenclature) {
		Block block = TaskUtil.createNewBlock(sample);

		logger.debug("Creating new block " + block.getBlockID());

		genericDAO.save(block, resourceBundle.get("log.patient.task.sample.blok.new",
				block.getParent().getParent().getTaskID(), block.getParent().getSampleID(), block.getBlockID()),
				sample.getPatient());

		logger.debug("Block saved, adding default slides");

		for (StainingPrototype proto : sample.getMaterilaPreset().getStainingPrototypes()) {
			slideHandlerAction.addStaining(proto, block);
		}

	}

	/********************************************************
	 * Block
	 ********************************************************/

	/********************************************************
	 * Task Data
	 ********************************************************/
	/**
	 * Method is called if the user changes task data.
	 * 
	 * @param task
	 */
	public void taskDataChanged(Task task) {
		taskDataChanged(task, null);
	}

	/**
	 * Method is called if the user changes task data. A detail resources string
	 * can be passed. This string can contain placeholder which will be replaced
	 * by the additional parameters.
	 * 
	 * @param task
	 * @param detailedInfoResourcesKey
	 * @param detailedInfoParams
	 */
	public void taskDataChanged(Task task, String detailedInfoResourcesKey, Object... detailedInfoParams) {
		String detailedInfoString = "";

		if (detailedInfoResourcesKey != null)
			detailedInfoString = resourceBundle.get(detailedInfoResourcesKey, detailedInfoParams);

		genericDAO.save(task, resourceBundle.get("log.patient.task.dataChange", task.getTaskID(), detailedInfoString),
				task.getParent());
		System.out.println("saving data");
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

	public void prepareCouncilDialog(Task task, boolean show) {
		setTemporaryTask(task);

		taskDAO.initializeCouncilData(task);

		// if (task.getCouncil() == null) {
		// // only saving if the diagnosis process has not been finished jet
		// if (task.isDiagnosisCompleted()) {
		// setTmpCouncil(new Council());
		// } else {
		// setTmpCouncil(new Council());
		// task.setCouncil(getTmpCouncil());
		// // setting current user als requesting physician
		// task.getCouncil().setPhysicianRequestingCouncil(userHandlerAction.getCurrentUser().getPhysician());
		//
		// genericDAO.save(task.getCouncil(),
		// resourceBundle.get("log.patient.task.council.new", task.getTaskID()),
		// task.getPatient());
		// }
		// } else
		// setTmpCouncil(task.getCouncil());
		//
		// updateCouncilDialog();
		//
		// if (show)
		// TODO: rework
		mainHandlerAction.showDialog(Dialog.COUNCIL);
	}

	public void updateCouncilDialog() {
		setAllAvailablePhysicians(physicianDAO.getPhysicians(ContactRole.values(), false));
		setAllAvailablePhysiciansTransformer(new DefaultTransformer<Physician>(getAllAvailablePhysicians()));
	}

	public void hideCouncilDialog() {
		hideCouncilDialogAndPrintReport(false);
	}

	public void hideCouncilDialogAndPrintReport(boolean print) {
		if (print) {
			pdfHandlerAction.prepareForPdf(getTemporaryTask(), PdfTemplate.COUNCIL);
			// workaround for showing and hiding two dialogues
			mainHandlerAction.setQueueDialog("#headerForm\\\\:printBtnShowOnly");
		}

		setTmpCouncil(null);
		setTemporaryTask(null);
		mainHandlerAction.hideDialog(Dialog.COUNCIL);
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

	public HashMap<String, String> getSelectableWards() {
		return selectableWards;
	}

	public void setSelectableWards(HashMap<String, String> selectableWards) {
		this.selectableWards = selectableWards;
	}

	public int getTemporaryTaskSampleCount() {
		return temporaryTaskSampleCount;
	}

	public void setTemporaryTaskSampleCount(int temporaryTaskSampleCount) {
		this.temporaryTaskSampleCount = temporaryTaskSampleCount;
	}

	public List<Physician> getAllAvailablePhysicians() {
		return allAvailablePhysicians;
	}

	public void setAllAvailablePhysicians(List<Physician> allAvailablePhysicians) {
		this.allAvailablePhysicians = allAvailablePhysicians;
	}

	public DefaultTransformer<Physician> getAllAvailablePhysiciansTransformer() {
		return allAvailablePhysiciansTransformer;
	}

	public void setAllAvailablePhysiciansTransformer(DefaultTransformer<Physician> allAvailablePhysiciansTransformer) {
		this.allAvailablePhysiciansTransformer = allAvailablePhysiciansTransformer;
	}

	public Council getTmpCouncil() {
		return tmpCouncil;
	}

	public void setTmpCouncil(Council tmpCouncil) {
		this.tmpCouncil = tmpCouncil;
	}

	public Physician getPhysicianToSign() {
		return physicianToSign;
	}

	public Physician getConsultantToSign() {
		return consultantToSign;
	}

	public void setPhysicianToSign(Physician physicianToSign) {
		this.physicianToSign = physicianToSign;
	}

	public void setConsultantToSign(Physician consultantToSign) {
		this.consultantToSign = consultantToSign;
	}

	public boolean isUseAutoNomenclature() {
		return useAutoNomenclature;
	}

	public void setUseAutoNomenclature(boolean useAutoNomenclature) {
		this.useAutoNomenclature = useAutoNomenclature;
	}

	public boolean isAutoNomenclatureChangedManually() {
		return autoNomenclatureChangedManually;
	}

	public void setAutoNomenclatureChangedManually(boolean autoNomenclatureChangedManually) {
		this.autoNomenclatureChangedManually = autoNomenclatureChangedManually;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
