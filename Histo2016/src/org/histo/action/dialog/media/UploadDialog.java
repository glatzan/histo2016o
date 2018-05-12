package org.histo.action.dialog.media;

import org.apache.log4j.Logger;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.UtilDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.util.dataList.HasDataList;
import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UploadDialog extends AbstractDialog {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private UtilDAO utilDAO;

	/**
	 * Uploaded file
	 */
	private UploadedFile uploadedFile;

	/**
	 * Description of the uploaded file
	 */
	private String uploadedFileCommentary;

	/**
	 * Datalists to upload pfds to
	 */
	private HasDataList[] dataLists;

	/**
	 * Type of uploaded file
	 */
	private DocumentType fileType;

	/**
	 * Type of uploaded file
	 */
	private DocumentType[] availableFileTypes;

	/**
	 * Associated Patient with datalists
	 */
	private Patient patient;

	public void initAndPrepareBean(HasDataList dataList, Patient patient, DocumentType[] availableFileTypes) {
		if (initBean(dataList, patient, availableFileTypes))
			prepareDialog();
	}

	public void initAndPrepareBean(HasDataList[] dataList, Patient patient, DocumentType[] availableFileTypes) {
		if (initBean(dataList, patient, availableFileTypes))
			prepareDialog();
	}

	public void initAndPrepareBean(HasDataList[] dataList, Patient patient, DocumentType[] availableFileTypes,
			DocumentType selectedFileType) {
		if (initBean(dataList, patient, availableFileTypes, selectedFileType))
			prepareDialog();
	}

	public boolean initBean(HasDataList dataList, Patient patient, DocumentType[] availableFileTypes) {
		return initBean(new HasDataList[] { dataList }, patient, availableFileTypes);
	}

	public boolean initBean(HasDataList[] dataList, Patient patient, DocumentType[] availableFileTypes) {
		return initBean(dataList, patient, availableFileTypes,
				availableFileTypes.length > 0 ? availableFileTypes[0] : null);
	}

	public boolean initBean(HasDataList[] dataList, Patient patient, DocumentType[] availableFileTypes,
			DocumentType selectedFileType) {
		try {
			for (HasDataList hasDataList : dataList) {
				utilDAO.initializeDataList(hasDataList);
			}
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
			return false;
		}
		
		setPatient(patient);
		setDataLists(dataList);
		setUploadedFileCommentary("");
		setAvailableFileTypes(availableFileTypes);
		setFileType(fileType);
		setFileType(selectedFileType);
		setUploadedFile(null);

		super.initBean(null, Dialog.UPLOAD);

		return true;
	}

	/**
	 * Uploads a file to the selected task
	 */
	public void uploadData() {
		try {
			if (getUploadedFile() != null && getUploadedFile().getSize() > 0) {
				PDFContainer upload = new PDFContainer();

				upload.setData(getUploadedFile().getContents());
				upload.setName(getUploadedFile().getFileName());
				upload.setType(getFileType());
				upload.setCommentary(getUploadedFileCommentary());

				// saving pdf

				genericDAO.savePatientData(upload, getPatient(), "log.patient.pdf.created",
						new Object[] { upload.getName().toString(), getPatient().toString() });

				for (HasDataList hasDataList : dataLists) {
					hasDataList.getAttachedPdfs().add(upload);

					// saving list
					genericDAO.savePatientData(hasDataList, getPatient(), "log.patient.pdf.attached",
							upload.getName());
				}

			}
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public String getUploadedFileCommentary() {
		return uploadedFileCommentary;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public void setUploadedFileCommentary(String uploadedFileCommentary) {
		this.uploadedFileCommentary = uploadedFileCommentary;
	}

	public DocumentType getFileType() {
		return fileType;
	}

	public void setFileType(DocumentType fileType) {
		this.fileType = fileType;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public HasDataList[] getDataLists() {
		return dataLists;
	}

	public void setDataLists(HasDataList[] dataLists) {
		this.dataLists = dataLists;
	}

	public DocumentType[] getAvailableFileTypes() {
		return availableFileTypes;
	}

	public void setAvailableFileTypes(DocumentType[] availableFileTypes) {
		this.availableFileTypes = availableFileTypes;
	}
}
