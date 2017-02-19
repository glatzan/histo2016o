package org.histo.action;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.ContactChooser;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.PdfGenerator;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class PrintHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private TaskHandlerAction taskHandlerAction;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PatientDao patientDao;
	
	/**
	 * class for creating pdfs
	 */
	@Autowired
	private PdfGenerator pdfGenerator;

	/**
	 * The selected task for that a report should be generated
	 */
	private Task taskToPrint;

	/**
	 * The selected council to print
	 */
	private Council concilToPrint;

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
	 * True if the pdf should be rendered
	 */
	private boolean renderPdf;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer tmpPdfContainer;

	/**
	 * Selected pritner to print the document
	 */
	private String selectedPrinter;

	/**
	 * List with all associated contacts
	 */
	private List<ContactChooser> contactChoosers;

	/**
	 * The contact rendered, the first one will always be rendered, if not
	 * changed, no rendering necessary
	 */
	private Contact contactRendered;

	/**
	 * Init of the bean, for printing with gui
	 * 
	 * @param task
	 * @param templates
	 * @param selectedTemplate
	 */
	public void initBean(Task task, PrintTemplate[] templates, PrintTemplate selectedTemplate) {

		setTaskToPrint(task);

		patientDao.initializeDataList(task);

		setSelectedPrinter(userHandlerAction.getCurrentUser().getPreferedPrinter());

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

	public void resetBean() {
		setTaskToPrint(null);
		setTemplateList(null);
		setSelectedTemplate(null);
		setTemplateTransformer(null);
		setTmpPdfContainer(null);
	}

	/**
	 * Showing only the dialog, no init will be done
	 */
	public void showPrintDialog() {
		mainHandlerAction.showDialog(Dialog.PRINT);
	}

	/**
	 * Shows the print dialog an initializes all default values.
	 * 
	 * @param task
	 */
	public void showDefaultPrintDialog(Task task) {
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.U_REPORT,
						DocumentType.U_REPORT_EMTY, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		initBean(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect));

		// contacts for printing
		setContactChoosers(new ArrayList<ContactChooser>());

		// setting patient
		getContactChoosers().add(new ContactChooser(task.getPatient().getPerson(), ContactRole.PATIENT));

		// setting other contacts (physicians)
		for (Contact contact : task.getContacts()) {
			getContactChoosers().add(new ContactChooser(contact));
		}

		setContactRendered(null);

		// rendering the template
		onChangePrintTemplate();

		mainHandlerAction.showDialog(Dialog.PRINT);
	}

	/**
	 * Prepares the print dialog for printing a case conference request.
	 * 
	 * @param task
	 */
	public void showCouncilPrintDialog(Task task, Council council) {
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.CASE_CONFERENCE });

		initBean(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect));

		// contacts for printing
		setContactChoosers(new ArrayList<ContactChooser>());

		setConcilToPrint(council);

		// only one adress so set as chosen
		if (council.getCouncilPhysician() != null) {
			ContactChooser chosser = new ContactChooser(council.getCouncilPhysician().getPerson(),
					ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactChoosers().add(chosser);

			// setting council physicians data as rendere contact data
			setContactRendered(new Contact(council.getCouncilPhysician().getPerson(), ContactRole.CASE_CONFERENCE));
		}

		onChangePrintTemplate();
	}

	/**
	 * Hides the print dialog and clears the print data.
	 */
	public void hidePrintDialog() {
		mainHandlerAction.hideDialog(Dialog.PRINT);
		resetBean();
	}

	/**
	 * Updates the pdf content if a contact was chosen for the first time
	 */
	public void onChooseContact() {
		List<ContactChooser> selectedContacts = getSelectedContactChooser();

		if (getContactRendered() == null && !selectedContacts.isEmpty()) {
			setContactRendered(selectedContacts.get(0).getContact());
			onChangePrintTemplate();
			RequestContext.getCurrentInstance().update("dialogContent");
		} else if (getContactRendered() != null) {
			boolean found = false;
			for (ContactChooser contactChooser : selectedContacts) {
				if (contactChooser.getContact() == getContactRendered()) {
					found = true;
					break;
				}
			}

			if (!found) {
				if (!selectedContacts.isEmpty()) {
					setContactRendered(selectedContacts.get(0).getContact());
				} else
					setContactRendered(null);
				onChangePrintTemplate();
				RequestContext.getCurrentInstance().update("dialogContent");
			}
		}
	}

	/**
	 * Renders the new template after a template was changed
	 */
	public void onChangePrintTemplate() {
		// always render the pdf with the fist contact chosen
		setTmpPdfContainer(pdfGenerator.generatePDFForReport(getTaskToPrint().getPatient(), getTaskToPrint(),
				getSelectedTemplate(), getContactRendered() == null ? null : getContactRendered().getPerson()));

		if (getTmpPdfContainer() == null) {
			setTmpPdfContainer(new PDFContainer(DocumentType.EMPTY, "", new byte[0]));
			setRenderPdf(false);
			logger.debug("No Pdf created, hiding pdf display");
		} else {
			logger.debug("Pdf created");
			setRenderPdf(true);
		}
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
			return new DefaultStreamedContent(new ByteArrayInputStream(getTmpPdfContainer().getData()),
					"application/pdf", getTmpPdfContainer().getName());
		}
	}

	/**
	 * Saves a new pdf within the task
	 * 
	 * @param pdf
	 */
	public void saveGeneratedPdf(PDFContainer pdf) {
		if (pdf.getId() == 0) {
			logger.debug("Pdf not saved jet, saving" + pdf.getName());

			genericDAO.save(pdf, resourceBundle.get("log.patient.task.pdf.created", pdf.getName()),
					getTaskToPrint().getPatient());

			getTaskToPrint().getAttachedPdfs().add(pdf);

			mainHandlerAction.saveDataChange(getTaskToPrint(), "log.patient.task.pdf.attached", pdf.getName());
		} else {
			logger.debug("PDF allready saved, not saving. " + pdf.getName());
		}
	}

	public void onDownloadPdf() {
		if (getTmpPdfContainer().getId() == 0) {
			logger.debug("Pdf not saved jet, saving");
			if (!getSelectedTemplate().isDoNotSave())
				saveGeneratedPdf(getTmpPdfContainer());
		}
	}

	public void onPrintNewPdf() {
		onPrintNewPdf(getSelectedContactChooser(), getTmpPdfContainer(), getContactRendered(),
				getSelectedTemplate().isDoNotSave());
	}

	public void onPrintNewPdf(List<ContactChooser> list, PDFContainer preview, Contact contactRenderedInPrevew,
			boolean doNotSave) {
		mainHandlerAction.getSettings().getPrinterManager().loadPrinter(selectedPrinter);
		// no address was chosen, so the address will be "An den
		// weiterbehandelden Kollegen" this was generated and saved in
		// tmpPdfContainer
		if (list.isEmpty()) {
			if (!doNotSave)
				saveGeneratedPdf(preview);
			mainHandlerAction.getSettings().getPrinterManager().print(preview);
		} else {
			// addresses where chosen
			for (ContactChooser contactChooser : list) {
				// address of the rendered pdf, not rendering twice
				if (contactChooser.getContact() == contactRenderedInPrevew) {
					if (!doNotSave)
						saveGeneratedPdf(preview);
					for (int i = 0; i < contactChooser.getCopies(); i++) {
						mainHandlerAction.getSettings().getPrinterManager().print(preview);
					}
				} else {
					// render all other pdfs
					PDFContainer otherAddress = pdfGenerator.generatePDFForReport(getTaskToPrint().getPatient(),
							getTaskToPrint(), getSelectedTemplate(), contactChooser.getContact().getPerson());
					for (int i = 0; i < contactChooser.getCopies(); i++) {
						mainHandlerAction.getSettings().getPrinterManager().print(otherAddress);
					}
				}

			}
		}

	}

	/**
	 * External printing, no bean initialization necessary. File is not saved.
	 * Printer has to be set
	 * 
	 * @param template
	 */
	public void printPdfFromExternalBean(PrintTemplate template) {
		PDFContainer newPdf = pdfGenerator.generatePDFForReport(getTaskToPrint().getPatient(), getTaskToPrint(),
				template);
		printPdfFromExternalBean(newPdf);
	}

	/**
	 * External printing, no bean initialization necessary. File is not saved.
	 * Printer has to be set
	 * 
	 * @param pdf
	 * @param saveIfNew
	 */
	public void printPdfFromExternalBean(PDFContainer pdf) {
		mainHandlerAction.getSettings().getPrinterManager().loadPrinter(selectedPrinter);
		mainHandlerAction.getSettings().getPrinterManager().print(pdf);
	}

	/**
	 * Gets the selected contacts an returns an list including them
	 * 
	 * @return
	 */
	private List<ContactChooser> getSelectedContactChooser() {
		ArrayList<ContactChooser> result = new ArrayList<ContactChooser>();
		for (ContactChooser contactChooser : getContactChoosers()) {
			if (contactChooser.isSelected())
				result.add(contactChooser);
		}

		logger.debug("Return " + result.size() + " selected contatcs");
		return result;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public Task getTaskToPrint() {
		return taskToPrint;
	}

	public void setTaskToPrint(Task taskToPrint) {
		this.taskToPrint = taskToPrint;
	}

	public DefaultTransformer<PrintTemplate> getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(DefaultTransformer<PrintTemplate> templateTransformer) {
		this.templateTransformer = templateTransformer;
	}

	public List<PrintTemplate> getTemplateList() {
		return templateList;
	}

	public void setTemplateList(List<PrintTemplate> templateList) {
		this.templateList = templateList;
	}

	public PrintTemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(PrintTemplate selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public PDFContainer getTmpPdfContainer() {
		return tmpPdfContainer;
	}

	public void setTmpPdfContainer(PDFContainer tmpPdfContainer) {
		this.tmpPdfContainer = tmpPdfContainer;
	}

	public boolean isRenderPdf() {
		return renderPdf;
	}

	public void setRenderPdf(boolean renderPdf) {
		this.renderPdf = renderPdf;
	}

	public String getSelectedPrinter() {
		return selectedPrinter;
	}

	public void setSelectedPrinter(String selectedPrinter) {
		this.selectedPrinter = selectedPrinter;
	}

	public List<ContactChooser> getContactChoosers() {
		return contactChoosers;
	}

	public void setContactChoosers(List<ContactChooser> contactChoosers) {
		this.contactChoosers = contactChoosers;
	}

	public Council getConcilToPrint() {
		return concilToPrint;
	}

	public void setConcilToPrint(Council concilToPrint) {
		this.concilToPrint = concilToPrint;
	}

	public Contact getContactRendered() {
		return contactRendered;
	}

	public void setContactRendered(Contact contactRendered) {
		this.contactRendered = contactRendered;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
