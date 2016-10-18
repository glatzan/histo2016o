package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PdfTemplate;
import org.histo.config.enums.TaskPriority;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.Siganture;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.util.ArchivAble;
import org.histo.model.util.TaskTree;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.ResourceBundle;
import org.histo.util.SlideUtil;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskHandlerAction implements Serializable {

	private static final long serialVersionUID = -1460063099758733063L;

	@Autowired
	private HelperDAO helperDAO;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private GenericDAO genericDAO;

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

	private HashMap<String, String> selectableWards;

	/********************************************************
	 * Task creation
	 ********************************************************/
	/**
	 * all staininglists, default not initialized
	 */
	private List<MaterialPreset> allAvailableMaterials;

	/**
	 * Transformer for selecting staininglist
	 */
	private StainingListTransformer materialListTransformer;

	/**
	 * selected stainingList for sample
	 */
	private MaterialPreset selectedMaterial;

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
	 * List of all physicians
	 */
	private List<Physician> physiciansToSignReport;

	/**
	 * Transformer for selecting a physician for sigin the report
	 */
	private DefaultTransformer<Physician> physiciansToSignReportTransformer;
	/********************************************************
	 * Task creation
	 ********************************************************/

	/********************************************************
	 * Archive able
	 ********************************************************/

	/**
	 * Object to archive
	 */
	private ArchivAble toArchive;

	/**
	 * the toArchive object will be archived if true
	 */
	private boolean archived;

	/********************************************************
	 * Archive able
	 ********************************************************/

	/********************************************************
	 * Task
	 ********************************************************/
	public void prepareForTask() {
		setAllAvailableMaterials(helperDAO.getAllStainingLists());
		setMaterialListTransformer(new StainingListTransformer(getAllAvailableMaterials()));

		setPhysiciansToSignReport(physicianDAO.getPhysicians(ContactRole.values(), false));
		setPhysiciansToSignReportTransformer(new DefaultTransformer<>(getPhysiciansToSignReport()));

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

	/**
	 * Displays a dialog for creating a new task
	 */
	public void prepareNewTaskDialog() {
		prepareForTask();

		setTemporaryTask(new Task());
		getTemporaryTask().setTaskPriority(TaskPriority.LOW);
		setTemporaryTaskSampleCount(1);
		Sample tmp = new Sample(getTemporaryTask());

		// checks if default statingsList is empty
		if (!getAllAvailableMaterials().isEmpty()) {
			tmp.setMaterilaPreset(getAllAvailableMaterials().get(0));
		}

		mainHandlerAction.showDialog(Dialog.TASK_CREATE);
	}

	/**
	 * Method is called if user adds or removes a sample within the task
	 * creation process. Adds or removes a new Material for the new Sample.
	 */
	public void updateNewTaskDilaog(Task task) {
		if (temporaryTaskSampleCount >= 1) {
			if (temporaryTaskSampleCount > task.getSamples().size())
				while (temporaryTaskSampleCount > task.getSamples().size()) {
					Sample tmp = new Sample(task);
					tmp.setMaterilaPreset(getAllAvailableMaterials().get(0));
				}
			else if (temporaryTaskSampleCount < task.getSamples().size())
				while (temporaryTaskSampleCount < task.getSamples().size()) {
					task.getSamples().remove(task.getSamples().size() - 1);
				}
		}
	}

	/**
	 * Creates a new task for the given Patient
	 * 
	 * @param patient
	 */
	public void createNewTask(Patient patient, Task phantomTask) {
		if (patient.getTasks() == null) {
			patient.setTasks(new ArrayList<>());
		}

		Task task = TaskUtil.createNewTask(phantomTask, patient, taskDAO.countSamplesOfCurrentYear());

		patient.getTasks().add(0, task);
		// sets the new task as the selected task
		patient.setSelectedTask(task);

		if (task.getReport(PdfTemplate.UREPROT) != null) {
			genericDAO.save(task.getReport(PdfTemplate.UREPROT), resourceBundle.get("log.patient.task.upload.orderList",
					task.getTaskID(), task.getReport(PdfTemplate.UREPROT).getName()), patient);
		}

		// saving report to datanase
		genericDAO.save(task.getReport(), resourceBundle.get("log.patient.task.report.new", task.getTaskID()),
				task.getPatient());

		genericDAO.save(task, resourceBundle.get("log.patient.task.new", task.getTaskID()), patient);

		for (Sample sample : task.getSamples()) {
			sample.setMaterial(sample.getMaterilaPreset().getName());

			genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.new", task.getTaskID(),
					sample.getSampleID(), sample.getMaterial()), task.getPatient());

			// creating first default diagnosis
			diagnosisHandlerAction.createDiagnosis(sample, DiagnosisType.DIAGNOSIS);
			// creating needed blocks
			createNewBlock(sample);
		}

		// checking if staining flag of the task object has to be false
		SlideUtil.checkIfAllSlidesAreStained(task);
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
		setAllAvailableMaterials(helperDAO.getAllStainingLists());
		// checks if default statingsList is empty
		if (!getAllAvailableMaterials().isEmpty()) {
			setSelectedMaterial(getAllAvailableMaterials().get(0));
			setMaterialListTransformer(new StainingListTransformer(getAllAvailableMaterials()));
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
		SlideUtil.checkIfAllSlidesAreStained(task);
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
		Sample sample = new Sample(task, material);

		genericDAO.save(sample, resourceBundle.get("log.patient.task.sample.new", task.getTaskID(),
				sample.getSampleID(), material.getName()), task.getPatient());

		// creating first default diagnosis
		diagnosisHandlerAction.createDiagnosis(sample, DiagnosisType.DIAGNOSIS);
		// creating needed blocks
		createNewBlock(sample);
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
		createNewBlock(sample);

		// checking if staining flag of the task object has to be false
		SlideUtil.checkIfAllSlidesAreStained(sample.getParent());
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
	public void createNewBlock(Sample sample) {
		Block block = TaskUtil.createNewBlock(sample);

		genericDAO.save(block, resourceBundle.get("log.patient.task.sample.blok.new",
				block.getParent().getParent().getTaskID(), block.getParent().getSampleID(), block.getBlockID()),
				sample.getPatient());

		for (StainingPrototype proto : sample.getMaterilaPreset().getStainingPrototypes()) {
			slideHandlerAction.addStaining(proto, block);
		}

	}

	/********************************************************
	 * Block
	 ********************************************************/

	/********************************************************
	 * Archive
	 ********************************************************/

	/**
	 * Shows a Dialog for deleting (archiving) the sample/task/bock/image
	 * 
	 * @param sample
	 * @param archived
	 */
	public void prepareArchiveObject(TaskTree<?> archive, boolean archived) {
		setArchived(archived);
		setToArchive(archive);
		// if no dialog is provieded the object will be archived immediately
		if (archive.getArchiveDialog() == null)
			archiveObject(archive, archived);
		else
			mainHandlerAction.showDialog(archive.getArchiveDialog());
	}

	/**
	 * Archives a Object implementing TaskTree.
	 * 
	 * @param task
	 * @param archiveAble
	 * @param archived
	 */
	public void archiveObject(TaskTree<?> archive, boolean archived) {

		archive.setArchived(archived);

		String logString = "log.error";

		if (archive instanceof Slide)
			logString = resourceBundle.get("log.patient.task.sample.blok.slide.archived",
					((Slide) archive).getParent().getParent().getParent().getTaskID(),
					((Slide) archive).getParent().getParent().getSampleID(), ((Slide) archive).getParent().getBlockID(),
					((Slide) archive).getSlideID());
		else if (archive instanceof Diagnosis)
			logString = resourceBundle.get("log.patient.task.sample.diagnosis.archived",
					((Diagnosis) archive).getParent().getParent().getTaskID(),
					((Diagnosis) archive).getParent().getSampleID(), ((Diagnosis) archive).getName());
		else if (archive instanceof Block)
			logString = resourceBundle.get("log.patient.task.sample.blok.archived",
					((Block) archive).getParent().getParent().getTaskID(), ((Block) archive).getParent().getSampleID(),
					((Block) archive).getBlockID());
		else if (archive instanceof Sample)
			logString = resourceBundle.get("log.patient.task.sample.archived",
					((Sample) archive).getParent().getTaskID(), ((Sample) archive).getSampleID());
		else if (archive instanceof Task)
			logString = resourceBundle.get("log.patient.task.archived", ((Task) archive).getTaskID());

		genericDAO.save(archive, logString, archive.getPatient());

		// update the gui list for displaying in the receiptlog
		TaskUtil.generateSlideGuiList(archive.getPatient().getSelectedTask());

		hideArchiveObjectDialog();
	}

	/**
	 * Hides the Dialog for achieving an object
	 */
	public void hideArchiveObjectDialog() {
		mainHandlerAction.showDialog(getToArchive().getArchiveDialog());
	}

	/********************************************************
	 * Archive
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
	 * Getter/Setter
	 ********************************************************/
	public List<MaterialPreset> getAllAvailableMaterials() {
		return allAvailableMaterials;
	}

	public void setAllAvailableMaterials(List<MaterialPreset> allAvailableMaterials) {
		this.allAvailableMaterials = allAvailableMaterials;
	}

	public MaterialPreset getSelectedMaterial() {
		return selectedMaterial;
	}

	public void setSelectedMaterial(MaterialPreset selectedMaterial) {
		this.selectedMaterial = selectedMaterial;
	}

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

	public ArchivAble getToArchive() {
		return toArchive;
	}

	public void setToArchive(ArchivAble toArchive) {
		this.toArchive = toArchive;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public int getTemporaryTaskSampleCount() {
		return temporaryTaskSampleCount;
	}

	public void setTemporaryTaskSampleCount(int temporaryTaskSampleCount) {
		this.temporaryTaskSampleCount = temporaryTaskSampleCount;
	}

	public List<Physician> getPhysiciansToSignReport() {
		return physiciansToSignReport;
	}

	public void setPhysiciansToSignReport(List<Physician> physiciansToSignReport) {
		this.physiciansToSignReport = physiciansToSignReport;
	}

	public DefaultTransformer<Physician> getPhysiciansToSignReportTransformer() {
		return physiciansToSignReportTransformer;
	}

	public void setPhysiciansToSignReportTransformer(DefaultTransformer<Physician> physiciansToSignReportTransformer) {
		this.physiciansToSignReportTransformer = physiciansToSignReportTransformer;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
