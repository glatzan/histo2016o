package org.histo.action.dialog;

import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.dao.PatientDao;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.util.printer.PrintTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")

public class PrintDialogHandler extends AbstractDialog {

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PDFGeneratorHandler pDFGeneratorHandler;

	/**
	 * Selected template for printing
	 */
	private PrintTemplate selectedTemplate;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer pdfContainer;

	/**
	 * The contact rendered, the first one will always be rendered, if not
	 * changed, no rendering necessary
	 */
	private Contact selectedContact;

	/**
	 * True if the pdf should be rendered
	 */
	private boolean renderPdf;

	/**
	 * Initializes the bean and shows the council dialog
	 * 
	 * @param task
	 */
	public void initAndPrepareBean(Task task) {
		initBean(task);
		prepareDialog();
	}

	public void initBean(Task task) {
		super.initBean(patientDao.savePatientAssociatedData(task), Dialog.PRINT);
	}

	public void onChangePrintTemplate() {

		switch (getSelectedTemplate().getDocumentTyp()) {
		case U_REPORT:
		case U_REPORT_EMTY:
			setPdfContainer(
					pDFGeneratorHandler.generateUReport(getSelectedTemplate(), getTask().getPatient(), getTask()));
			break;
		case DIAGNOSIS_REPORT:
			setPdfContainer(pDFGeneratorHandler.generateDiagnosisReport(getSelectedTemplate(), getTask().getPatient(),
					getTask(), getSelectedContact() == null ? new Person(resourceBundle.get("pdf.address.none"))
							: getSelectedContact().getPerson()));
			break;
		default:
			// always render the pdf with the fist contact chosen
			setPdfContainer(pDFGeneratorHandler.generatePDFForReport(getTask().getPatient(), getTask(),
					getSelectedTemplate(), getSelectedContact() == null ? null : getSelectedContact().getPerson()));
			break;
		}

		if (getPdfContainer() == null) {
			setPdfContainer(new PDFContainer(DocumentType.EMPTY, "", new byte[0]));
			setRenderPdf(false);
			logger.debug("No Pdf created, hiding pdf display");
		} else {
			logger.debug("Pdf created");
			setRenderPdf(true);
		}
	}

	// ************************ Getter/Setter ************************

	public PrintTemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(PrintTemplate selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public PDFContainer getPdfContainer() {
		return pdfContainer;
	}

	public void setPdfContainer(PDFContainer pdfContainer) {
		this.pdfContainer = pdfContainer;
	}

	public Contact getSelectedContact() {
		return selectedContact;
	}

	public void setSelectedContact(Contact selectedContact) {
		this.selectedContact = selectedContact;
	}

	public boolean isRenderPdf() {
		return renderPdf;
	}

	public void setRenderPdf(boolean renderPdf) {
		this.renderPdf = renderPdf;
	}
}
