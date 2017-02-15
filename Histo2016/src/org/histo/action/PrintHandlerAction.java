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
import org.histo.config.enums.PrintDocumentTyp;
import org.histo.config.enums.PrintTab;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.ClinicPrinter;
import org.histo.model.transitory.json.PdfTemplate;
import org.histo.model.transitory.json.TexTemplate;
import org.histo.ui.ContactChooser;
import org.histo.ui.transformer.ClinicPrinterTransformer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.ui.transformer.PdfTemplateTransformer;
import org.histo.util.PdfGenerator;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Template;

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
	private List<TexTemplate> templateList;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<TexTemplate> templateTransformer;

	/**
	 * Selected template for printing
	 */
	private TexTemplate selectedTemplate;

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
	private ClinicPrinter selectedPrinter;

	/**
	 * class for creating pdfs
	 */
	private PdfGenerator pdfGenerator;

	/**
	 * List with all associated contacts
	 */
	private List<ContactChooser> contactChooser;

	/**
	 * The print tab to display (Print view or pdf view)
	 */
	private PrintTab printTab;

	// // loading templates if no are passed
	// if (templates != null)
	// setTemplates(templates);
	// else
	// setTemplates(PdfTemplate.getInternalReportsOnly(HistoSettings.PDF_TEMPLATE_JSON));

	public void initBean(Task task, TexTemplate[] templates, TexTemplate selectedTemplate) {

		setTaskToPrint(task);

		pdfGenerator = new PdfGenerator(mainHandlerAction, resourceBundle);

		setTemplateList(new ArrayList<TexTemplate>(Arrays.asList(templates)));

		setTemplateTransformer(new DefaultTransformer<TexTemplate>(getTemplateList()));

		// sets the selected template
		if (selectedTemplate == null && !getTemplateList().isEmpty())
			setSelectedTemplate(getTemplateList().get(0));
		else
			setSelectedTemplate(selectedTemplate);

		// settings printers
		mainHandlerAction.setClinicPrinterTransformer(
				new ClinicPrinterTransformer(mainHandlerAction.getSettings().getPrinter().getPrinters()));
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
		taskDAO.initializePdfData(task);
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
		mainHandlerAction.setClinicPrinterTransformer(null);
	}

	/**
	 * Shows the print dialog an initializes all default values.
	 * 
	 * @param task
	 */
	public void showDefaultPrintDialog(Task task) {
		TexTemplate[] templates = TexTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON);
		TexTemplate[] subSelect = TexTemplate.getTemplatesByTypes(templates,
				new PrintDocumentTyp[] { PrintDocumentTyp.DIAGNOSIS_REPORT, PrintDocumentTyp.U_REPORT,
						PrintDocumentTyp.DIAGNOSIS_REPORT_EXTERN });

		initBean(task, templates, TexTemplate.getDefaultTemplate(subSelect));

		onChangePrintTemplate();

		setPrintTab(PrintTab.PRINT_PDFs);
		
		mainHandlerAction.showDialog(Dialog.PRINT_NEW);
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
				getSelectedTemplate(),
				getTaskToPrint().getDiagnosisInfo().getSignatureOne().getPhysician().getPerson()));

		if (getTmpPdfContainer() == null) {
			setTmpPdfContainer(new PDFContainer("", "", new byte[0]));
			setRenderPdf(false);
			logger.debug("No Pdf created, hiding pdf display");
		} else {
			logger.debug("Pdf created");
			setRenderPdf(true);
		}
	}
	
	public void onChangeAttachedTemplate() {
		if (getTmpPdfContainer() == null || getTmpPdfContainer().getId() == 0) {
			if (!getTaskToPrint().getAttachedPdfs().isEmpty()) {
				setTmpPdfContainer(getTaskToPrint().getAttachedPdfs().get(0));
				setRenderPdf(true);
			} else {
				setRenderPdf(false);
				setTmpPdfContainer(new PDFContainer("", "", new byte[0]));
			}
		} else
			setRenderPdf(true);
	}
	
	public void onChangeTab(){
		if(getPrintTab() == PrintTab.PRINT_PDFs)
			onChangePrintTemplate();
		else
			onChangeAttachedTemplate();
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

		mainHandlerAction.getSettings().getPrinter().print(selectedPrinter, getTmpPdfContainer());
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

	public DefaultTransformer<TexTemplate> getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(DefaultTransformer<TexTemplate> templateTransformer) {
		this.templateTransformer = templateTransformer;
	}

	public List<TexTemplate> getTemplateList() {
		return templateList;
	}

	public void setTemplateList(List<TexTemplate> templateList) {
		this.templateList = templateList;
	}

	public TexTemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(TexTemplate selectedTemplate) {
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

	public ClinicPrinter getSelectedPrinter() {
		return selectedPrinter;
	}

	public void setSelectedPrinter(ClinicPrinter selectedPrinter) {
		this.selectedPrinter = selectedPrinter;
	}

	public List<ContactChooser> getContactChooser() {
		return contactChooser;
	}

	public void setContactChooser(List<ContactChooser> contactChooser) {
		this.contactChooser = contactChooser;
	}

	public PrintTab getPrintTab() {
		return printTab;
	}

	public void setPrintTab(PrintTab printTab) {
		this.printTab = printTab;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
