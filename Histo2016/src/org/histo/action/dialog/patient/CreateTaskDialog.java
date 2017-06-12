package org.histo.action.dialog.patient;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.MediaDialogHandler;
import org.histo.action.dialog.media.MediaDialog;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.InformedConsentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.TaskPriority;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.BioBank;
import org.histo.model.Council;
import org.histo.model.FavouriteList;
import org.histo.model.MaterialPreset;
import org.histo.model.PDFContainer;
import org.histo.model.Signature;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.TimeUtil;
import org.histo.util.printer.PrintTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class CreateTaskDialog extends AbstractDialog {

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private MediaDialogHandler mediaDialogHandler;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private SettingsHandler settingsHandler;

	@Autowired
	private PDFGeneratorHandler pDFGeneratorHandler;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private MediaDialog mediaDialog;

	private Patient patient;

	private List<MaterialPreset> materialList;

	private DefaultTransformer<MaterialPreset> materialListTransformer;

	private int sampleCount;

	private boolean autoNomenclatureChangedManually;

	private BioBank bioBank;

	private boolean moveInformedConsent;

	/**
	 * Initializes the bean and shows the createTaskDialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Patient patient) {
		initBean(patient);
		prepareDialog();
	}

	/**
	 * Initializes the bean
	 * 
	 * @param patient
	 */
	public void initBean(Patient patient) {
		try {
			setPatient(genericDAO.refresh(patient));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			setPatient(patientDao.getPatient(patient.getId(), true));
			worklistViewHandlerAction.replacePatientInCurrentWorklist(getPatient());
		}

		super.initBean(new Task(getPatient()), Dialog.TASK_CREATE);

		// setting material list
		setMaterialList(settingsDAO.getAllMaterialPresets());
		setMaterialListTransformer(new DefaultTransformer<>(getMaterialList()));

		getTask().setTaskID(getNewTaskID());
		getTask().setTaskPriority(TaskPriority.NONE);
		getTask().setDateOfReceipt(TimeUtil.setDayBeginning(System.currentTimeMillis()));
		getTask().setUseAutoNomenclature(true);

		// samplecount for new task
		setSampleCount(1);

		setAutoNomenclatureChangedManually(false);

		// resetting selected pdf container for informed consent upload
		mediaDialogHandler.setSelectedPdfContainer(null);

		// creates a new sample, is automatically added to the task
		new Sample(getTask(), !getMaterialList().isEmpty() ? getMaterialList().get(0) : null);

		// setting biobank
		setBioBank(new BioBank());
		getBioBank().setInformedConsentType(InformedConsentType.NONE);
		getBioBank().setTask(getTask());

		setMoveInformedConsent(false);
	}

	/**
	 * Updates the name and the amount of samples which should be created with
	 * the new task.
	 */
	public void updateDialog() {

		logger.debug("New Task: Updating sample tree");

		// changing autoNomeclature of samples, if no change was made manually
		if (getSampleCount() > 1 && !isAutoNomenclatureChangedManually()) {
			logger.debug("New Task: More then one sample, setting autonomeclature to true");
			getTask().setUseAutoNomenclature(true);
		} else if (getSampleCount() == 1 && !isAutoNomenclatureChangedManually()) {
			logger.debug("New Task: Only one sample, setting autonomeclature to false");
			getTask().setUseAutoNomenclature(false);
		}

		if (getSampleCount() >= 1) {
			if (getSampleCount() > task.getSamples().size()) {
				logger.debug("New Task: Samplecount > samples, adding new samples");
				// adding samples if count is bigger then the current sample
				// count
				while (getSampleCount() > task.getSamples().size())
					new Sample(task, !getMaterialList().isEmpty() ? getMaterialList().get(0) : null);
			} else if (getSampleCount() < task.getSamples().size()) {
				logger.debug("New Task: Samplecount > samples, removing sample");
				// removing samples if count is less then current sample count
				while (getSampleCount() < task.getSamples().size())
					task.getSamples().remove(task.getSamples().size() - 1);
			}
		}

		logger.debug("New Task: Updating sample names");

		// updates the name of all other samples
		for (Sample sample : task.getSamples()) {
			sample.updateNameOfSample(getTask().isUseAutoNomenclature(), true);
		}
	}

	/**
	 * Creates a new Task object and calls createBiobak at the end.
	 */
	public void createTask() {
		try {
			if (getPatient().getTasks() == null) {
				getPatient().setTasks(new ArrayList<>());
			}

			getPatient().getTasks().add(0, getTask());
			// sets the new task as the selected task

			// saving task
			patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.task.new", task.getTaskID());

			getTask().setDiagnosisContainer(new DiagnosisContainer(task));
			getTask().getDiagnosisContainer().setDiagnosisRevisions(new ArrayList<DiagnosisRevision>());

			// setting signature
			getTask().getDiagnosisContainer().setSignatureOne(new Signature());
			getTask().getDiagnosisContainer().setSignatureTwo(new Signature());

			getTask().setCaseHistory("");
			getTask().setWard("");

			getTask().setCouncils(new ArrayList<Council>());

			getTask().setFavouriteLists(new ArrayList<FavouriteList>());

			// saving diagnosis container
			patientDao.savePatientAssociatedDataFailSave(task.getDiagnosisContainer(),
					"log.patient.task.diagnosisContainer.new", task.getTaskID());

			for (Sample sample : getTask().getSamples()) {
				// set name of material for changing it manually
				sample.setMaterial(sample.getMaterilaPreset().getName());

				// saving samples
				patientDao.savePatientAssociatedDataFailSave(sample, "log.patient.task.sample.new",
						sample.getSampleID());
				// creating needed blocks
				// TODO: save for version conflict
				taskManipulationHandler.createNewBlock(sample, task.isUseAutoNomenclature());

				// saving the sample
				// saving samples
				patientDao.savePatientAssociatedDataFailSave(sample, "log.patient.task.sample.update",
						sample.getSampleID());
			}

			// creating standard diagnoses
			taskManipulationHandler.createDiagnosisRevision(getTask().getDiagnosisContainer(),
					DiagnosisRevisionType.DIAGNOSIS);

			// generating gui list
			getTask().generateSlideGuiList();

			// creating bioBank for Task
			bioBank.setAttachedPdfs(new ArrayList<PDFContainer>());
			patientDao.savePatientAssociatedDataFailSave(bioBank, getTask(), "log.patient.save");

			PDFContainer selectedPDF = mediaDialogHandler.getSelectedPdfContainer();
			if (selectedPDF != null) {
				// attaching pdf to biobank
				bioBank.getAttachedPdfs().add(selectedPDF);

				patientDao.savePatientAssociatedDataFailSave(bioBank, getTask(), "log.patient.bioBank.pdf.attached",
						selectedPDF.getName());

				// and task
				getTask().setAttachedPdfs(new ArrayList<PDFContainer>());
				getTask().getAttachedPdfs().add(selectedPDF);

				patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.pdf.attached");

				if (isMoveInformedConsent()) {
					patient.getAttachedPdfs().remove(selectedPDF);
					patientDao.savePatientAssociatedDataFailSave(getPatient(), "log.patient.pdf.removed",
							selectedPDF.getName());
				}
			}

			patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.task.edit", task.getTaskID());
			favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.StainingList);

			patientDao.savePatientAssociatedDataFailSave(getPatient(), "log.patient.save");

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Calls createTask and prints the Ureport form
	 */
	public void createTaskAndPrintUReport() {
		createTask();

		PrintTemplate[] subSelect = PrintTemplate.getTemplatesByTypes(new DocumentType[] { DocumentType.U_REPORT });

		if (subSelect.length == 0) {
			logger.error("New Task: No TemplateUtil for printing UReport found");
			return;
		}

		// printing u report
		PDFContainer newPdf = pDFGeneratorHandler.generateUReport(subSelect[0], task.getPatient(), task);
		settingsHandler.getSelectedPrinter().print(newPdf);
	}

	/**
	 * Is called if the user has manually change the checkbox, after that the
	 * autonomeclature settings aren't changed automatically
	 */
	public void manuallyChangeAutoNomenclature() {
		logger.debug("New Task: Autonomeclature change manually");
		setAutoNomenclatureChangedManually(true);
		updateDialog();
	}

	/**
	 * Returns the name for a new Task
	 * 
	 * @return
	 */
	public String getNewTaskID() {
		return Integer.toString(TimeUtil.getCurrentYear() - 2000)
				+ HistoUtil.fitString(taskDAO.countTasksOfCurrentYear(), 4, '0');
	}

	public void showMediaSelectDialog(PDFContainer pdfContainer) {
		// init dialog for patient and task
		mediaDialog.initBean(getPatient(), new HasDataList[] { getPatient() }, pdfContainer, true);

		// enabeling upload to task
		mediaDialog.enableUpload(new HasDataList[] { getPatient() },
				new DocumentType[] { DocumentType.BIOBANK_INFORMED_CONSENT });

		// setting info text
		mediaDialog
				.setActionDescription(resourceBundle.get("dialog.media.headline.info.biobank", getTask().getTaskID()));

		// show dialog
		mediaDialog.prepareDialog();
	}

	// ************************ Getter/Setter ************************
	public List<MaterialPreset> getMaterialList() {
		return materialList;
	}

	public void setMaterialList(List<MaterialPreset> materialList) {
		this.materialList = materialList;
	}

	public DefaultTransformer<MaterialPreset> getMaterialListTransformer() {
		return materialListTransformer;
	}

	public void setMaterialListTransformer(DefaultTransformer<MaterialPreset> materialListTransformer) {
		this.materialListTransformer = materialListTransformer;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	public boolean isAutoNomenclatureChangedManually() {
		return autoNomenclatureChangedManually;
	}

	public void setAutoNomenclatureChangedManually(boolean autoNomenclatureChangedManually) {
		this.autoNomenclatureChangedManually = autoNomenclatureChangedManually;
	}

	public BioBank getBioBank() {
		return bioBank;
	}

	public void setBioBank(BioBank bioBank) {
		this.bioBank = bioBank;
	}

	public boolean isMoveInformedConsent() {
		return moveInformedConsent;
	}

	public void setMoveInformedConsent(boolean moveInformedConsent) {
		this.moveInformedConsent = moveInformedConsent;
	}

}
