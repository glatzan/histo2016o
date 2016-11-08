package org.histo.action;

import java.io.ByteArrayInputStream;
import java.util.Date;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PrintTab;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.interfaces.DynamicHandler;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PdfTemplate;
import org.histo.ui.transformer.PdfTemplateTransformer;
import org.histo.util.PdfGenerator;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * @formatter:off I_Name I_Birthday I_Insurance I_PIZ_CODE I_PIZ I_Date
 *
 *                I_SAMPLES I_EDATE I_TASK_NUMBER_CODE I_TASK_NUMBER I_EYE
 *                I_HISTORY C_INSURANCE_NORMAL C_INSURANCE_PRIVATE I_WARD
 *                C_MALIGN
 *
 *                I_RESIDENT_DOCTOR_FAX I_SURGEON
 *
 *                I_DIAGNOSIS_EXTENDED I_DIAGNOSIS
 *
 *                I_SIGANTURE_DATE
 *
 *                I_PHYSICIAN I_PHYSICIAN_ROLE I_CONSULTANT I_CONSULTANT_ROLE
 * @formatter:on
 * @author andi
 *
 */
@Controller
@Scope("session")
public class PdfHandlerAction {

	@Autowired
	private GenericDAO genericDAO;

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
	 * List with all templates available for printing.
	 */
	private PdfTemplate[] templates;

	/**
	 * Selected template for printing
	 */
	private PdfTemplate selectedTemplate;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private PdfTemplateTransformer templateTransformer;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer tmpPdfContainer;

	/**
	 * The printtab to diasplay (Print view or pdf view)
	 */
	private PrintTab printTab;

	/**
	 * True if pdf should be rendere in the gui
	 */
	private boolean renderPDF = false;

	/**
	 * If an external report should be printed the adress of the receiver is
	 * determined using this variable. It can be {@link ContactRole},
	 * FAMILY_PHYSICIAN, PRIVATE_PHYSICIAN, and OTHER for a list to choose from.
	 */
	private ContactRole externalReportPhysicianType;

	/**
	 * If externalReportPhysicianType is set to OTHER the selected physician is
	 * stored in this variable.
	 */
	private Physician externalPhysician;

	/**
	 * Physician to sign the internal_short, external and external short report.
	 */
	private Physician signatureTmpPhysician;

	/**
	 * The date of the signature and the report
	 */
	private long dateOfReport;

	// TODO implement
	private String printer;

	// TODO implement
	private int copies;

	/**
	 * Shows the print dialog. Does not initializes the bean!
	 */
	public void showPrintDialog() {
		onChangeTemplate();
		mainHandlerAction.showDialog(Dialog.PRINT);
	}

	/**
	 * Shows the print dialog an initializes all default values.
	 * 
	 * @param task
	 */
	public void showPrintDialog(Task task) {
		prepareForPdf(task);
		mainHandlerAction.showDialog(Dialog.PRINT);
	}

	/**
	 * Hides the print dialog and clears the print data.
	 */
	public void hidePrintDialog() {
		mainHandlerAction.hideDialog(Dialog.PRINT);
		clearData();
	}

	public void prepareForPdf(Task task) {
		prepareForPdf(task, null);
	}

	public void prepareForPdf(Task task, String selectedTemplateName) {
		prepareForPdf(task, null, selectedTemplateName);
	}

	public void prepareForPdf(Task task, PdfTemplate[] templates, String selectedTemplateName) {
		prepareBean(task, templates, selectedTemplateName);

		setPrintTab(PrintTab.PRINT_PDFs);

		onChangeTemplate();
	}

	public void prepareForAttachedPdf(Task task, PDFContainer selectedPdfContainer) {
		prepareBean(task, null, null);

		setTmpPdfContainer(selectedPdfContainer);

		if (selectedPdfContainer != null) {
			setSelectedTemplate(PdfTemplate.getTemplateByType(getTemplates(), selectedPdfContainer.getType()));
		} else
			setSelectedTemplate(PdfTemplate.getDefaultTemplate(getTemplates()));

		setPrintTab(PrintTab.ATTACHED_PDFs);

		onChangeTemplate();
	}

	public void onChangeTemplate() {
		if (getPrintTab() == PrintTab.PRINT_PDFs) {
			System.out.println(getTaskToPrint() + " -- ");
			setTmpPdfContainer((new PdfGenerator(mainHandlerAction, resourceBundle)).generatePdfForTemplate(
					getTaskToPrint(), getSelectedTemplate(), getDateOfReport(), getExternalReportPhysicianType(),
					getExternalPhysician(), getSignatureTmpPhysician()));

			if (getTmpPdfContainer() == null) {
				setTmpPdfContainer(new PDFContainer("", "", new byte[0]));
				setRenderPDF(true);
			} else
				setRenderPDF(true);

		} else {
			if (getTmpPdfContainer() == null || getTmpPdfContainer().getId() == 0) {
				if (!getTaskToPrint().getAttachedPdfs().isEmpty()) {
					setTmpPdfContainer(getTaskToPrint().getAttachedPdfs().get(0));
					setRenderPDF(true);
				} else {
					setRenderPDF(false);
					setTmpPdfContainer(new PDFContainer("", "", new byte[0]));
				}
			} else
				setRenderPDF(true);
		}

	}

	/**
	 * Hides the print Dialog an switches to the Council Dialog. Workaround
	 * because direct hiding and then showing an other dialog not working.
	 */
	public void switchToCouncilDialog() {
		taskHandlerAction.prepareCouncilDialog(getTaskToPrint(), false);
		hidePrintDialog();
		mainHandlerAction.setQueueDialog("#headerForm\\\\:councilBtnShowOnly");
	}

	/**
	 * Handles the uploaded pdf orderLetter.
	 * 
	 * @param event
	 */
	public void handleTaskRequestReport(FileUploadEvent event) {
		PDFContainer requestReport = new PDFContainer(PdfTemplate.UREPORT);
		// requestReport.setType("application/pdf");
		requestReport.setData(event.getFile().getContents());
		requestReport.setName(event.getFile().getFileName());
		if (taskHandlerAction.getTemporaryTask() != null)
			taskHandlerAction.getTemporaryTask().addReport(requestReport);
	}

	public void onPrintPDF() {
		if (getTmpPdfContainer().getId() == 0) {
			genericDAO.save(getTmpPdfContainer(),
					resourceBundle.get("log.patient.task.pdf.created", getTmpPdfContainer().getName()),
					getTaskToPrint().getPatient());
			getTaskToPrint().getAttachedPdfs().add(getTmpPdfContainer());
			genericDAO.save(getTaskToPrint(), resourceBundle.get("log.patient.task.pdf.attached",
					getTaskToPrint().getTaskID(), getTmpPdfContainer().getName()), getTaskToPrint().getPatient());
		} else
			System.out.println("downloading only");
	}

	/**
	 * Prepares the bean for printing and generating pdfs.
	 * 
	 * @param task
	 * @param templates
	 * @param selectedTemplateType
	 */
	public void prepareBean(Task task, PdfTemplate[] templates, String selectedTemplateType) {

		// loading templates if no are passed
		if (templates != null)
			setTemplates(templates);
		else
			setTemplates(PdfTemplate.getInternalReportsOnly(HistoSettings.PDF_TEMPLATE_JSON));

		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));

		setTaskToPrint(task);

		// loads the default template if no type is passed
		if (selectedTemplateType != null) {
			setSelectedTemplate(PdfTemplate.getTemplateByType(getTemplates(), selectedTemplateType));
		} else {
			setSelectedTemplate(PdfTemplate.getDefaultTemplate(getTemplates()));
		}

		// setting default external receiver to family physician
		if (getExternalReportPhysicianType() == null)
			setExternalReportPhysicianType(ContactRole.FAMILY_PHYSICIAN);

		// changing the time of signature if 0
		if (getDateOfReport() == 0)
			setDateOfReport(System.currentTimeMillis());

		// initializes teh task
		taskDAO.initializeCouncilData(task);
		taskDAO.initializeReportData(task);

		taskDAO.initializePdfData(task);

		// also initializing taskHandlerAction, generating lists to choos
		// physicians from
		taskHandlerAction.prepareBean();
	}

	/**
	 * Clears all values of the bean.
	 */
	public void clearData() {
		setTaskToPrint(null);
		setTemplates(null);
		setSelectedTemplate(null);
		setTemplateTransformer(null);
		setTmpPdfContainer(null);
	}

	/********************************************************
	 * DynamicHandler Interface
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public Task getTaskToPrint() {
		return taskToPrint;
	}

	public void setTaskToPrint(Task taskToPrint) {
		this.taskToPrint = taskToPrint;
	}

	public PdfTemplate[] getTemplates() {
		return templates;
	}

	public void setTemplates(PdfTemplate[] templates) {
		this.templates = templates;
	}

	public PdfTemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(PdfTemplate selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public PdfTemplateTransformer getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(PdfTemplateTransformer templateTransformer) {
		this.templateTransformer = templateTransformer;
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

	public String getPrinter() {
		return printer;
	}

	public void setPrinter(String printer) {
		this.printer = printer;
	}

	public int getCopies() {
		return copies;
	}

	public void setCopies(int copies) {
		this.copies = copies;
	}

	public PrintTab getPrintTab() {
		return printTab;
	}

	public void setPrintTab(PrintTab printTab) {
		this.printTab = printTab;
	}

	public boolean isRenderPDF() {
		return renderPDF;
	}

	public void setRenderPDF(boolean renderPDF) {
		this.renderPDF = renderPDF;
	}

	public ContactRole getExternalReportPhysicianType() {
		return externalReportPhysicianType;
	}

	public Physician getExternalPhysician() {
		return externalPhysician;
	}

	public void setExternalReportPhysicianType(ContactRole externalReportPhysicianType) {
		this.externalReportPhysicianType = externalReportPhysicianType;
	}

	public void setExternalPhysician(Physician externalPhysician) {
		this.externalPhysician = externalPhysician;
	}

	public PDFContainer getTmpPdfContainer() {
		return tmpPdfContainer;
	}

	public void setTmpPdfContainer(PDFContainer tmpPdfContainer) {
		this.tmpPdfContainer = tmpPdfContainer;
	}

	public Physician getSignatureTmpPhysician() {
		return signatureTmpPhysician;
	}

	public void setSignatureTmpPhysician(Physician signatureTmpPhysician) {
		this.signatureTmpPhysician = signatureTmpPhysician;
	}

	public long getDateOfReport() {
		return dateOfReport;
	}

	public void setDateOfReport(long dateOfReport) {
		this.dateOfReport = dateOfReport;
	}

	public Date getDateOfReportAsDate() {
		return new Date(dateOfReport);
	}

	public void setDateOfReportAsDate(Date dateOfReportAsDate) {
		this.dateOfReport = dateOfReportAsDate.getTime();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
