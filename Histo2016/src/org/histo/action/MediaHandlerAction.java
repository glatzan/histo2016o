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
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Lazy
	private PrintHandlerAction printHandlerAction;

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
	 * Temporary task
	 */
	private HasDataList temporaryDataList;

	/**
	 * Used for showing pdf in a dialog
	 */
	private PDFContainer temporaryPdfContainer;

	/**
	 * Selected container, will not be set to null after dialog was closed
	 */
	private PDFContainer selectedPdfContainer;

	/********************************************************
	 * media display
	 ********************************************************/

	/********************************************************
	 * Data Upload
	 ********************************************************/

	/**
	 * Shows a dialog for uploading files to a task
	 */
	public void prepareUploadDialog(HasDataList dataList) {
		patientDao.initializeDataList(dataList);
		setTemporaryDataList(dataList);
		setUploadedFileCommentary("");
		setUploadedFileType(DocumentType.OTHER);
		mainHandlerAction.showDialog(Dialog.UPLOAD);
	}

	/**
	 * Uploads a file to the selected task
	 */
	public void uploadData(HasDataList dataList) {
		if (getUploadedFile() != null) {
			PDFContainer upload = new PDFContainer();

			upload.setData(getUploadedFile().getContents());
			upload.setName(getUploadedFile().getFileName());
			upload.setType(getUploadedFileType());
			upload.setCommentary(getUploadedFileCommentary());

			// saving pdf
			genericDAO.save(upload, resourceBundle.get("log.patient.pdf.created", upload.getName()),
					dataList.getPatient());

			dataList.getAttachedPdfs().add(upload);

			String logRef = "";

			if (dataList instanceof Patient) {
				logRef = "log.patient.pdf.attached";
			} else {
				logRef = "log.patient.task.pdf.attached";
			}

			// saving list
			mainHandlerAction.saveDataChange(dataList, logRef, upload.getName());
		}
		hideDataUploadDialog();
	}

	/**
	 * Hides the dialog for uploading files to a task
	 */
	public void hideDataUploadDialog() {
		mainHandlerAction.hideDialog(Dialog.UPLOAD);
		setUploadedFile(null);
	}

	/********************************************************
	 * Data Upload
	 ********************************************************/

	/********************************************************
	 * Single Media display from extern
	 ********************************************************/
	public void perpareBeanForExternalForSinglView(PDFContainer mediaToDisplay) {
		logger.trace("Prepare PDF generation form external bean");
		// init bean
		setTemporaryPdfContainer(mediaToDisplay);
	}

	/********************************************************
	 * Single Media display from extern
	 ********************************************************/
	
	/********************************************************
	 * Media display
	 ********************************************************/
	public void prepareMediaDisplayDialog(HasDataList dataList) {
		prepareMediaDisplayDialog(dataList, null);
	}

	public void prepareMediaDisplayDialog(HasDataList dataList, PDFContainer mediaToDisplay) {
		mainHandlerAction.showDialog(Dialog.MEDIA_PREVIEW);
		patientDao.initializeDataList(dataList);
		setTemporaryDataList(dataList);
		
		// setting default printer
		printHandlerAction.setSelectedPrinter(userHandlerAction.getCurrentUser().getPreferedPrinter());

		if (mediaToDisplay == null && dataList.getAttachedPdfs().size() > 0)
			mediaToDisplay = dataList.getAttachedPdfs().get(0);

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
		mainHandlerAction.hideDialog(Dialog.MEDIA_PREVIEW);
		setTemporaryPdfContainer(null);
		setTemporaryDataList(null);
	}

	/********************************************************
	 * Media display
	 ********************************************************/

	/********************************************************
	 * Media select display
	 ********************************************************/
	public void prepareMediaDisplaySelectDialog(HasDataList dataList) {
		prepareMediaDisplaySelectDialog(dataList, null);
	}

	public void prepareMediaDisplaySelectDialog(HasDataList dataList, PDFContainer mediaToDisplay) {
		mainHandlerAction.showDialog(Dialog.MEDIA_SELECT);
		patientDao.initializeDataList(dataList);
		setTemporaryDataList(dataList);

		if (mediaToDisplay == null && dataList.getAttachedPdfs().size() > 0)
			mediaToDisplay = dataList.getAttachedPdfs().get(0);

		setTemporaryPdfContainer(mediaToDisplay);
	}

	public void selectMedia(PDFContainer mediaToDisplay) {
		setSelectedPdfContainer(mediaToDisplay);
		hideMediaDisplaySelectDialog();
	}

	public void hideMediaDisplaySelectDialog() {
		mainHandlerAction.hideDialog(Dialog.MEDIA_SELECT);
		setTemporaryPdfContainer(null);
		setTemporaryDataList(null);
	}

	/********************************************************
	 * Media select display
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

	public HasDataList getTemporaryDataList() {
		return temporaryDataList;
	}

	public void setTemporaryDataList(HasDataList temporaryDataList) {
		this.temporaryDataList = temporaryDataList;
	}

	public PDFContainer getSelectedPdfContainer() {
		return selectedPdfContainer;
	}

	public void setSelectedPdfContainer(PDFContainer selectedPdfContainer) {
		this.selectedPdfContainer = selectedPdfContainer;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
