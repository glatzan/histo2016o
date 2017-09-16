package org.histo.action.dialog.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.InformedConsentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.TaskPriority;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomNotUniqueReqest;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
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
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateUReport;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.PDFGenerator;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CreateTaskDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

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
			setPatient(genericDAO.reattach(patient));
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			setPatient(patientDao.getPatient(patient.getId(), true));
			worklistViewHandlerAction.replacePatientInCurrentWorklist(getPatient());
		}

		super.initBean(new Task(getPatient()), Dialog.TASK_CREATE, true);

		// setting material list
		setMaterialList(utilDAO.getAllMaterialPresets(true));
		setMaterialListTransformer(new DefaultTransformer<>(getMaterialList()));

		getTask().setTaskID(getNewTaskID());
		getTask().setTaskPriority(TaskPriority.NONE);
		getTask().setDateOfReceipt(TimeUtil.setDayBeginning(System.currentTimeMillis()));
		getTask().setUseAutoNomenclature(true);

		// samplecount for new task
		setSampleCount(1);

		setAutoNomenclatureChangedManually(false);

		// creates a new sample, is automatically added to the task
		new Sample(getTask(), !getMaterialList().isEmpty() ? getMaterialList().get(0) : null);

		// setting biobank
		setBioBank(new BioBank());
		getBioBank().setInformedConsentType(InformedConsentType.NONE);
		getBioBank().setTask(getTask());

		setMoveInformedConsent(false);
		System.out.println(getNewTaskID());
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
	 * 
	 * @throws CustomNotUniqueReqest
	 */
	public void createTask() {
		uniqueRequestID.checkUniqueRequestID(true);

		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					genericDAO.reattach(getPatient());

					if (getPatient().getTasks() == null) {
						getPatient().setTasks(new ArrayList<>());
					}

					getPatient().getTasks().add(0, getTask());
					// sets the new task as the selected task

					getTask().setParent(getPatient());
					getTask().setCaseHistory("");
					getTask().setWard("");

					// renewing taskID, if somebody has created an other task
					// meanwhile
					getTask().setTaskID(getNewTaskID());

					getTask().setCouncils(new ArrayList<Council>());

					getTask().setFavouriteLists(new ArrayList<FavouriteList>());

					// saving task
					genericDAO.savePatientData(getTask(), "log.patient.task.new", getTask().getTaskID());

					DiagnosisContainer diagnosisContainer = new DiagnosisContainer(getTask());
					getTask().setDiagnosisContainer(diagnosisContainer);
					diagnosisContainer.setDiagnosisRevisions(new ArrayList<DiagnosisRevision>());

					// setting signature
					diagnosisContainer.setSignatureOne(new Signature());
					diagnosisContainer.setSignatureTwo(new Signature());

					// saving diagnosis container
					genericDAO.savePatientData(diagnosisContainer, "log.patient.task.diagnosisContainer.new",
							getTask().getTaskID());

					for (Sample sample : getTask().getSamples()) {
						// set name of material for changing it manually
						sample.setMaterial(sample.getMaterilaPreset().getName());

						// saving samples
						genericDAO.savePatientData(sample, "log.patient.task.sample.new", sample.getSampleID());
						// creating needed blocks
						taskManipulationHandler.createNewBlock(sample, task.isUseAutoNomenclature());

					}

					// creating standard diagnoses
					taskManipulationHandler.createDiagnosisRevision(getTask().getDiagnosisContainer(),
							DiagnosisRevisionType.DIAGNOSIS);

					// generating gui list
					getTask().generateSlideGuiList();

					// creating bioBank for Task
					bioBank.setAttachedPdfs(new ArrayList<PDFContainer>());

					PDFContainer selectedPDF = dialogHandlerAction.getMediaDialog().getSelectedPdfContainer();

					if (selectedPDF != null) {
						// attaching pdf to biobank
						bioBank.getAttachedPdfs().add(selectedPDF);

						genericDAO.savePatientData(bioBank, getTask(), "log.patient.bioBank.pdf.attached",
								selectedPDF.getName());

						// and task
						getTask().setAttachedPdfs(new ArrayList<PDFContainer>());
						getTask().getAttachedPdfs().add(selectedPDF);

						genericDAO.savePatientData(getTask(), "log.patient.pdf.attached");

						if (isMoveInformedConsent()) {
							patient.getAttachedPdfs().remove(selectedPDF);
							genericDAO.savePatientData(getPatient(), "log.patient.pdf.removed", selectedPDF.getName());
						}
					} else {
						genericDAO.savePatientData(bioBank, getTask(), "log.patient.bioBank.save");
					}

					genericDAO.savePatientData(getTask(), "log.patient.task.update", task.getTaskID());

					FavouriteList f = favouriteListDAO.getFavouriteList(PredefinedFavouriteList.StainingList.getId(),
							true);

					favouriteListDAO.addTaskToList(getTask(), f);

					genericDAO.save(task.getPatient());
					System.out.println("asd");
				}
			});

			genericDAO.lockParent(task);

		} catch (Exception e) {
			getPatient().getTasks().remove(0);
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Calls createTask and prints the Ureport form
	 * 
	 * @throws CustomNotUniqueReqest
	 */
	public void createTaskAndPrintUReport() {
		createTask();

		DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.U_REPORT);

		if (subSelect.length == 0) {
			logger.error("New Task: No TemplateUtil for printing UReport found");
			return;
		}

		// printing u report

		((TemplateUReport) subSelect[0]).initData(task.getPatient(), getTask());
		PDFContainer newPdf = ((TemplateUReport) subSelect[0]).generatePDF(new PDFGenerator());

		userHandlerAction.getSelectedPrinter().print(newPdf);
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
		try {
			// generating new task id
			Task task = taskDAO.getTaskWithLastID();
			String currentYear = Integer.toString(TimeUtil.getCurrentYear() - 2000);

			// task is within the current year
			if (task.getTaskID().startsWith(currentYear)) {
				// getting counter
				String count = task.getTaskID().substring(2, 6);
				// increment counter
				int counterAsInt = Integer.valueOf(count) + 1;
				return currentYear + HistoUtil.fitString(counterAsInt, 4, '0');
			} else {
				throw new NoResultException();
			}
		} catch (NoResultException e) {
			// first task ever, or first task of year , year + 0001
			return Integer.toString(TimeUtil.getCurrentYear() - 2000) + HistoUtil.fitString(1, 4, '0');
		}

	}

	public void showMediaSelectDialog(PDFContainer pdfContainer) {
		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(getPatient(), new HasDataList[] { getPatient() }, pdfContainer,
				true);

		// enabeling upload to task
		dialogHandlerAction.getMediaDialog().enableUpload(new HasDataList[] { getPatient() },
				new DocumentType[] { DocumentType.BIOBANK_INFORMED_CONSENT });

		// setting info text
		dialogHandlerAction.getMediaDialog()
				.setActionDescription(resourceBundle.get("dialog.media.headline.info.biobank", getTask().getTaskID()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}

	public void onDatabaseVersionConflict() {
		worklistViewHandlerAction.replacePatientInCurrentWorklist(getTask().getParent().getId());
		super.onDatabaseVersionConflict();
	}

}
