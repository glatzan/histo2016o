package org.histo.action.dialog;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;


import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.ui.ContactChooser;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.printer.PrintTemplate;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class PrintDialogHandler extends AbstractDialog {

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PDFGeneratorHandler pDFGeneratorHandler;

	@Autowired
	private SettingsHandler settingsHandler;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * List of all templates for printing
	 */
	private List<PrintTemplate> templateList;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<PrintTemplate> templateTransformer;

	/**
	 * Selected template for printing
	 */
	private PrintTemplate selectedTemplate;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer pdfContainer;

	/**
	 * List with all associated contacts
	 */
	private List<ContactChooser> contactList;

	/**
	 * The associatedContact rendered, the first one will always be rendered, if not
	 * changed, no rendering necessary
	 */
	private AssociatedContact selectedContact;

	/**
	 * True if the pdf should be rendered
	 */
	private boolean renderPdf;

	/**
	 * Council to print
	 */
	private Council selectedCouncil;

	/**
	 * Initializes the bean and shows the council dialog
	 * 
	 * @param task
	 */
	public void initAndPrepareBeanForPrinting(Task task) {
		initBeanForPrinting(task);
		prepareDialog();
	}

	public void initBeanForPrinting(Task task) {
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.U_REPORT,
						DocumentType.U_REPORT_EMTY, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		initBean(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect));

		// contacts for printing
		setContactList(new ArrayList<ContactChooser>());

		// setting patient
		getContactList().add(new ContactChooser(task, task.getPatient().getPerson(), ContactRole.PATIENT));

		// setting other contacts (physicians)
		for (AssociatedContact associatedContact : task.getContacts()) {
			getContactList().add(new ContactChooser(associatedContact));
		}

		setSelectedContact(null);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initAndPrepareBeanForCouncil(Task task, Council council) {
		initBeanForCouncil(task, council);
		prepareDialog();
	}

	public void initBeanForCouncil(Task task, Council council) {
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.COUNCIL_REQUEST });

		initBean(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect));

		setSelectedCouncil(council);

		// contacts for printing
		setContactList(new ArrayList<ContactChooser>());

		// only one adress so set as chosen
		if (getSelectedCouncil().getCouncilPhysician() != null) {
			ContactChooser chosser = new ContactChooser(task, getSelectedCouncil().getCouncilPhysician().getPerson(),
					ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactList().add(chosser);

			// setting council physicians data as rendere associatedContact data
			setSelectedContact(new AssociatedContact(task, getSelectedCouncil().getCouncilPhysician().getPerson(),
					ContactRole.CASE_CONFERENCE));
		}

		onChangePrintTemplate();
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType) {
		initBeanForExternalDisplay(task, types, defaultType, null);
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType, AssociatedContact sendTo) {
		PrintTemplate[] subSelect = PrintTemplate.getTemplatesByTypes(types);
		initBeanForExternalDisplay(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect, defaultType), sendTo);
	}

	public void initBeanForExternalDisplay(Task task, PrintTemplate[] types, PrintTemplate defaultType,
			AssociatedContact sendTo) {

		initBean(task, types, defaultType);

		setContactList(new ArrayList<ContactChooser>());

		setSelectedContact(sendTo);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBean(Task task, PrintTemplate[] templates, PrintTemplate selectedTemplate) {
		// getting task datalist, if was altered a updated task will be returend
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.PRINT);

		if (templates != null) {
			setTemplateList(new ArrayList<PrintTemplate>(Arrays.asList(templates)));

			setTemplateTransformer(new DefaultTransformer<PrintTemplate>(getTemplateList()));

			// sets the selected template
			if (selectedTemplate == null && !getTemplateList().isEmpty())
				setSelectedTemplate(getTemplateList().get(0));
			else
				setSelectedTemplate(selectedTemplate);
		}
	}

	/**
	 * Updates the pdf content if a associatedContact was chosen for the first time
	 */
	public void onChooseContact() {
		List<ContactChooser> selectedContacts = getSelectedContactFromList();

		if (getSelectedContact() == null && !selectedContacts.isEmpty()) {
			setSelectedContact(selectedContacts.get(0).getContact());
			onChangePrintTemplate();
			RequestContext.getCurrentInstance().update("dialogContent");
		} else if (getSelectedContact() != null) {
			boolean found = false;
			for (ContactChooser contactChooser : selectedContacts) {
				if (contactChooser.getContact() == getSelectedContact()) {
					found = true;
					break;
				}
			}

			if (!found) {
				if (!selectedContacts.isEmpty()) {
					setSelectedContact(selectedContacts.get(0).getContact());
				} else
					setSelectedContact(null);
				onChangePrintTemplate();
				RequestContext.getCurrentInstance().update("dialogContent");
			}
		}
	}

	public void onChangePrintTemplate() {
		setPdfContainer(generatePDFFromTemplate());
		setRenderPdf(getPdfContainer() == null ? false : true);
	}

	private PDFContainer generatePDFFromTemplate() {
		PDFContainer result;
		switch (getSelectedTemplate().getDocumentTyp()) {
		case U_REPORT:
		case U_REPORT_EMTY:
			result = pDFGeneratorHandler.generateUReport(getSelectedTemplate(), getTask().getPatient(), getTask());
			break;
		case DIAGNOSIS_REPORT:
			result = pDFGeneratorHandler.generateDiagnosisReport(getSelectedTemplate(), getTask().getPatient(),
					getTask(), getSelectedContact() == null ? new Person(resourceBundle.get("pdf.address.none"))
							: getSelectedContact().getPerson());
			break;
		default:
			// always render the pdf with the fist associatedContact chosen
			result = pDFGeneratorHandler.generatePDFForReport(getTask().getPatient(), getTask(), getSelectedTemplate(),
					getSelectedContact() == null ? null : getSelectedContact().getPerson());
			break;
		}

		if (result == null) {
			result = new PDFContainer(DocumentType.EMPTY, "", new byte[0]);
			logger.debug("No Pdf created, hiding pdf display");
		}
		return result;
	}

	/**
	 * Gets the selected contacts an returns an list including them
	 * 
	 * @return
	 */
	private List<ContactChooser> getSelectedContactFromList() {
		ArrayList<ContactChooser> result = new ArrayList<ContactChooser>();
		for (ContactChooser contactChooser : getContactList()) {
			if (contactChooser.isSelected())
				result.add(contactChooser);
		}

		logger.debug("Return " + result.size() + " selected contatcs");
		return result;
	}

	/**
	 * Return the pdf as streamed content
	 * 
	 * @return
	 */
	public StreamedContent getPdfContent() {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream(getPdfContainer().getData()), "application/pdf",
					getPdfContainer().getName());
		}
	}

	public void onDownloadPdf() {
		if (getPdfContainer().getId() == 0) {
			logger.debug("Pdf not saved jet, saving");
			if (!getSelectedTemplate().isDoNotSave())
				savePdf(getTask(), getPdfContainer());
		}
	}

	public void onPrintNewPdf() {
		// no address was chosen, so the address will be "An den
		// weiterbehandelden Kollegen" this was generated and saved in
		// tmpPdfContainer
		if (getContactList().isEmpty()) {
			if (!getSelectedTemplate().isDoNotSave())
				savePdf(getTask(), getPdfContainer());
			settingsHandler.getSelectedPrinter().print(getPdfContainer());
		} else {
			boolean oneContactSelected = false;
			// addresses where chosen
			for (ContactChooser contactChooser : getContactList()) {
				if (contactChooser.isSelected()) {
					// address of the rendered pdf, not rendering twice
					if (contactChooser.getContact() == getSelectedContact()) {
						if (!getSelectedTemplate().isDoNotSave())
							savePdf(getTask(), getPdfContainer());
						for (int i = 0; i < contactChooser.getCopies(); i++) {
							settingsHandler.getSelectedPrinter().print(getPdfContainer());
						}
					} else {
						// setting other associatedContact then selected
						AssociatedContact tmp = getSelectedContact();
						setSelectedContact(contactChooser.getContact());
						// render all other pdfs
						PDFContainer otherAddress = generatePDFFromTemplate();
						for (int i = 0; i < contactChooser.getCopies(); i++) {
							settingsHandler.getSelectedPrinter().print(otherAddress);
						}
						// settings the old selected associatedContact as selected associatedContact
						setSelectedContact(tmp);
					}

					oneContactSelected = true;
				}

			}

			// printin if no container was selected, with the default address
			if (!oneContactSelected) {
				settingsHandler.getSelectedPrinter().print(getPdfContainer());
			}
		}

	}

	/**
	 * Saves a new pdf within the task
	 * 
	 * @param pdf
	 */
	public void savePdf(Task task, PDFContainer pdf) {

		try {
			if (pdf.getId() == 0) {
				logger.debug("Pdf not saved jet, saving" + pdf.getName());

				// saving new pdf and updating task
				patientDao.savePatientAssociatedDataFailSave(pdf, task, "log.patient.task.pdf.created", pdf.getName());

				task.getAttachedPdfs().add(pdf);

				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.pdf.attached", pdf.getName());
			} else {
				logger.debug("PDF allready saved, not saving. " + pdf.getName());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
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

	public AssociatedContact getSelectedContact() {
		return selectedContact;
	}

	public void setSelectedContact(AssociatedContact selectedContact) {
		this.selectedContact = selectedContact;
	}

	public boolean isRenderPdf() {
		return renderPdf;
	}

	public void setRenderPdf(boolean renderPdf) {
		this.renderPdf = renderPdf;
	}

	public List<PrintTemplate> getTemplateList() {
		return templateList;
	}

	public void setTemplateList(List<PrintTemplate> templateList) {
		this.templateList = templateList;
	}

	public DefaultTransformer<PrintTemplate> getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(DefaultTransformer<PrintTemplate> templateTransformer) {
		this.templateTransformer = templateTransformer;
	}

	public List<ContactChooser> getContactList() {
		return contactList;
	}

	public void setContactList(List<ContactChooser> contactList) {
		this.contactList = contactList;
	}

	public Council getSelectedCouncil() {
		return selectedCouncil;
	}

	public void setSelectedCouncil(Council selectedCouncil) {
		this.selectedCouncil = selectedCouncil;
	}

}
