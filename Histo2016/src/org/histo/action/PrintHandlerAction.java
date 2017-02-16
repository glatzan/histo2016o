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
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.ContactChooser;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.PdfGenerator;
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

	/**
	 * The selected task for that a report should be generated
	 */
	private Task taskToPrint;

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
	 * class for creating pdfs
	 */
	private PdfGenerator pdfGenerator;

	/**
	 * List with all associated contacts
	 */
	private List<ContactChooser> contactChooser;



	public void initBean(Task task, PrintTemplate[] templates, PrintTemplate selectedTemplate) {

		setTaskToPrint(task);

		pdfGenerator = new PdfGenerator(mainHandlerAction, resourceBundle);

		setTemplateList(new ArrayList<PrintTemplate>(Arrays.asList(templates)));

		setTemplateTransformer(new DefaultTransformer<PrintTemplate>(getTemplateList()));

		// sets the selected template
		if (selectedTemplate == null && !getTemplateList().isEmpty())
			setSelectedTemplate(getTemplateList().get(0));
		else
			setSelectedTemplate(selectedTemplate);

		setSelectedPrinter(userHandlerAction.getCurrentUser().getPreferedPrinter());

		if (task.getContacts() != null)
			setContactChooser(ContactChooser.getContactChooserList(task.getContacts()));

		// // setting default external receiver to family physician
		// if (getExternalReportPhysicianType() == null)
		// setExternalReportPhysicianType(ContactRole.FAMILY_PHYSICIAN);
		//
		// // changing the time of signature if 0
		// if (getDateOfReport() == 0)
		// setDateOfReport(System.currentTimeMillis());
		//
		// // initializes teh task
		// taskDAO.initializeCouncilData(task);
		// taskDAO.initializeDiagnosisData(task);
		//
		taskDAO.initializeTaskData(task);
		//
		// // also initializing taskHandlerAction, generating lists to choos
		// // physicians from
		// taskHandlerAction.initBean();
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
		mainHandlerAction.showDialog(Dialog.PRINT_NEW);
	}

	/**
	 * Shows the print dialog an initializes all default values.
	 * 
	 * @param task
	 */
	public void showDefaultPrintDialog(Task task) {
		PrintTemplate[] templates = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON);
		PrintTemplate[] subSelect = PrintTemplate.getTemplatesByTypes(templates, new DocumentType[] {
				DocumentType.DIAGNOSIS_REPORT, DocumentType.U_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		initBean(task, templates, PrintTemplate.getDefaultTemplate(subSelect));

		onChangePrintTemplate();

		mainHandlerAction.showDialog(Dialog.PRINT_NEW);
	}

	/**
	 * Prepares the print dialog for printing a case conference request.
	 * 
	 * @param task
	 */
	public void showCouncilPrintDialog(Task task) {
		PrintTemplate[] templates = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON);
		PrintTemplate[] subSelect = PrintTemplate.getTemplatesByTypes(templates,
				new DocumentType[] { DocumentType.CASE_CONFERENCE });

		initBean(task, templates, PrintTemplate.getDefaultTemplate(subSelect));

		onChangePrintTemplate();
	}

	/**
	 * Hides the print dialog and clears the print data.
	 */
	public void hidePrintDialog() {
		mainHandlerAction.hideDialog(Dialog.PRINT_NEW);
		resetBean();
	}

	/**
	 * Renders the new template after a template was changed
	 */
	public void onChangePrintTemplate() {
		setTmpPdfContainer(pdfGenerator.generatePDFForReport(getTaskToPrint().getPatient(), getTaskToPrint(),
				getSelectedTemplate(), null));

		if (getTmpPdfContainer() == null) {
			setTmpPdfContainer(new PDFContainer(DocumentType.EMPTY, "", new byte[0]));
			setRenderPdf(false);
			logger.debug("No Pdf created, hiding pdf display");
		} else {
			logger.debug("Pdf created");
			setRenderPdf(true);
		}
	}

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

	public void saveGeneratedPdf(PDFContainer pdf) {
		logger.debug("Saving Pdf " + pdf.getName());
		genericDAO.save(pdf, resourceBundle.get("log.patient.task.pdf.created", pdf.getName()),
				getTaskToPrint().getPatient());

		getTaskToPrint().getAttachedPdfs().add(pdf);

		mainHandlerAction.saveDataChange(getTaskToPrint(), "log.patient.task.pdf.attached", pdf.getName());
	}

	public void onDownloadPdf() {
		if (getTmpPdfContainer().getId() == 0) {
			logger.debug("Pdf not saved jet, saving");
			saveGeneratedPdf(getTmpPdfContainer());
		}
	}

	public void onPrintPdf() {
		if (getTmpPdfContainer().getId() == 0) {
			logger.debug("Pdf not saved jet, saving");
			saveGeneratedPdf(getTmpPdfContainer());
		}

		mainHandlerAction.getSettings().getPrinterManager().print(selectedPrinter, getTmpPdfContainer());
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

	public List<ContactChooser> getContactChooser() {
		return contactChooser;
	}

	public void setContactChooser(List<ContactChooser> contactChooser) {
		this.contactChooser = contactChooser;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
