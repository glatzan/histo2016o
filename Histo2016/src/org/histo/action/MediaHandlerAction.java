package org.histo.action;

import java.io.ByteArrayInputStream;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MediaHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private PatientHandlerAction patientHandlerAction;

	@Autowired
	private TaskHandlerAction taskHandlerAction;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private PatientDao patientDao;
	/********************************************************
	 * data upload
	 ********************************************************/
	/**
	 * Uploaded file
	 */
	private UploadedFile uploadedFile;

	/**
	 * Type of uploaded file
	 */
	private DocumentType uploadedFileType;

	/**
	 * Description of the uploaded file
	 */
	private String uploadedFileCommentary;

	/********************************************************
	 * data upload
	 ********************************************************/

	/********************************************************
	 * media display
	 ********************************************************/
	/**
	 * Used for showing pdf in a dialog
	 */
	private PDFContainer temporaryPdfContainer;

	/********************************************************
	 * media display
	 ********************************************************/

	/********************************************************
	 * PatientData Upload
	 ********************************************************/
	/**
	 * Shows the upload file dialog for a patient
	 */
	public void preparePatientDataUploadDialog(Patient patient) {
		patientDao.initializePatientPdfData(patient);
		patientHandlerAction.setTmpPatient(patient);
		setUploadedFileCommentary("");
		setUploadedFileType(DocumentType.OTHER);
		mainHandlerAction.showDialog(Dialog.UPLOAD_PATIENT);
	}

	/**
	 * Uploads a file to the patient file array
	 */
	public void uploadPatientData(Patient patient) {
		logger.debug("Uploadting new file to " + patient.getPerson().getFullName());
		if (getUploadedFile() != null) {
			PDFContainer upload = new PDFContainer();

			upload.setData(getUploadedFile().getContents());
			upload.setName(getUploadedFile().getFileName());
			upload.setType(getUploadedFileType());
			upload.setCommentary(getUploadedFileCommentary());

			// saving pdf
			genericDAO.save(upload, resourceBundle.get("log.patient.pdf.created", upload.getName()), patient);

			patient.getAttachedPdfs().add(upload);
			System.out.println(patient.getAttachedPdfs().size());
			// saving patient
			mainHandlerAction.saveDataChange(patient, "log.patient.pdf.attached", upload.getName());
		} else
			logger.debug("No file provided");

		hidePatientDataUploadDialog();

	}

	/**
	 * Hides the upload file dialog of the patient
	 */
	public void hidePatientDataUploadDialog() {
		mainHandlerAction.hideDialog(Dialog.UPLOAD_PATIENT);
		setUploadedFile(null);
		patientHandlerAction.setTmpPatient(null);
	}

	/********************************************************
	 * PatientData Upload
	 ********************************************************/
	/********************************************************
	 * Task Data Upload
	 ********************************************************/
	/**
	 * Shows a dialog for uploading files to a task
	 */
	public void prepareTaskDataUploadDialog(Task task) {
		taskDAO.initializeTaskData(task);
		taskHandlerAction.setTemporaryTask(task);
		setUploadedFileCommentary("");
		setUploadedFileType(DocumentType.OTHER);
		mainHandlerAction.showDialog(Dialog.UPLOAD_TASK);
	}

	/**
	 * Uploads a file to the selected task
	 */
	public void uploadTaskData(Task task) {
		if (getUploadedFile() != null) {
			PDFContainer upload = new PDFContainer();

			upload.setData(getUploadedFile().getContents());
			upload.setName(getUploadedFile().getFileName());
			upload.setType(getUploadedFileType());
			upload.setCommentary(getUploadedFileCommentary());

			// saving pdf
			genericDAO.save(upload, resourceBundle.get("log.patient.pdf.created", upload.getName()), task.getPatient());

			task.getAttachedPdfs().add(upload);

			// saving patient
			mainHandlerAction.saveDataChange(task, "log.patient.task.pdf.attached", upload.getName());
		}
		hideTaskDataUploadDialog();
	}

	/**
	 * Hides the dialog for uploading files to a task
	 */
	public void hideTaskDataUploadDialog() {
		mainHandlerAction.hideDialog(Dialog.UPLOAD_TASK);
		setUploadedFile(null);
	}

	/********************************************************
	 * Task Data Upload
	 ********************************************************/

	/********************************************************
	 * Task Media display
	 ********************************************************/
	public void prepareMediaDisplayDialog(Task task) {
		prepareMediaDisplayDialog(task, null);
	}

	public void prepareMediaDisplayDialog(Task task, PDFContainer mediaToDisplay) {
		mainHandlerAction.showDialog(Dialog.TASK_MEDIA_PREVIEW);
		taskHandlerAction.setTemporaryTask(task);
		taskDAO.initializeTaskData(task);

		if (mediaToDisplay == null && task.getAttachedPdfs().size() > 0)
			mediaToDisplay = task.getAttachedPdfs().get(0);

		setTemporaryPdfContainer(mediaToDisplay);
	}

	public StreamedContent getPdfContent() {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE || getTemporaryPdfContainer() == null) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream(getTemporaryPdfContainer().getData()),
					"application/pdf", getTemporaryPdfContainer().getName());
		}
	}

	public void hideMediaDisplayDialog() {
		mainHandlerAction.hideDialog(Dialog.TASK_MEDIA_PREVIEW);
		taskHandlerAction.setTemporaryTask(null);
		setTemporaryPdfContainer(null);
	}

	/********************************************************
	 * Task Media display
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public DocumentType getUploadedFileType() {
		return uploadedFileType;
	}

	public String getUploadedFileCommentary() {
		return uploadedFileCommentary;
	}

	public PDFContainer getTemporaryPdfContainer() {
		return temporaryPdfContainer;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public void setUploadedFileType(DocumentType uploadedFileType) {
		this.uploadedFileType = uploadedFileType;
	}

	public void setUploadedFileCommentary(String uploadedFileCommentary) {
		this.uploadedFileCommentary = uploadedFileCommentary;
	}

	public void setTemporaryPdfContainer(PDFContainer temporaryPdfContainer) {
		this.temporaryPdfContainer = temporaryPdfContainer;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
