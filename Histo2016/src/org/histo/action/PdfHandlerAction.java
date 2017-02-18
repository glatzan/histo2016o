package org.histo.action;

import java.io.ByteArrayInputStream;
import java.util.Date;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.util.PdfGenerator;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
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

	private UploadedFile file;

	public UploadedFile getFile() {
		return file;
	}

	public void setFile(UploadedFile file) {
		this.file = file;
	}

	public void test(){
		System.out.println(file);
	}
	/**
	 * The selected task for that a report should be generated
	 */
	private Task taskToPrint;


	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer tmpPdfContainer;

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

	/********************************************************
	 * Upload
	 ********************************************************/

	/**
	 * Uploaded file from upload dialog
	 */
	private PDFContainer uploadedFile;

	/**
	 * File Type of the uploaded file {@link PdfContainer}
	 */
	private String uploadedFileType;

	/********************************************************
	 * Upload
	 ********************************************************/

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
//		prepareForPdf(task, null, selectedTemplateName);
	}

	public void prepareForPdf(Task task, PrintTemplate[] templates, String selectedTemplateName) {
//		prepareBean(task, templates, selectedTemplateName);


		onChangeTemplate();
	}

	public void prepareForAttachedPdf(Task task, PDFContainer selectedPdfContainer) {
//		prepareBean(task, null, null);
//
//		setTmpPdfContainer(selectedPdfContainer);
//
//		if (selectedPdfContainer != null) {
//			setSelectedTemplate(PdfTemplate.getTemplateByType(getTemplates(), selectedPdfContainer.getType()));
//		} else
//			setSelectedTemplate(PdfTemplate.getDefaultTemplate(getTemplates()));
//
//
//		onChangeTemplate();
	}

	public void onChangeTemplate() {
//		if (getPrintTab() == PrintTab.PRINT_PDFs) {
//			setTmpPdfContainer((new PdfGenerator(mainHandlerAction, resourceBundle)).generatePdfForTemplate(
//					getTaskToPrint(), getSelectedTemplate(), getDateOfReport(), getExternalReportPhysicianType(),
//					getExternalPhysician(), getSignatureTmpPhysician()));
//
//			if (getTmpPdfContainer() == null) {
//				setTmpPdfContainer(new PDFContainer("", "", new byte[0]));
//				setRenderPDF(false);
//			} else
//				setRenderPDF(true);
//
//		} else {
//			if (getTmpPdfContainer() == null || getTmpPdfContainer().getId() == 0) {
//				if (!getTaskToPrint().getAttachedPdfs().isEmpty()) {
//					setTmpPdfContainer(getTaskToPrint().getAttachedPdfs().get(0));
//					setRenderPDF(true);
//				} else {
//					setRenderPDF(false);
//					setTmpPdfContainer(new PDFContainer("", "", new byte[0]));
//				}
//			} else
//				setRenderPDF(true);
//		}

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
	public void prepareBean(Task task, PrintTemplate[] templates, String selectedTemplateType) {

//		// loading templates if no are passed
//		if (templates != null)
//			setTemplates(templates);
//		else
//			setTemplates(PdfTemplate.getInternalReportsOnly(HistoSettings.PDF_TEMPLATE_JSON));
//
//		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));
//
//		setTaskToPrint(task);
//
//		// loads the default template if no type is passed
//		if (selectedTemplateType != null) {
//			setSelectedTemplate(PdfTemplate.getTemplateByType(getTemplates(), selectedTemplateType));
//		} else {
//			setSelectedTemplate(PdfTemplate.getDefaultTemplate(getTemplates()));
//		}
//
//		// setting default external receiver to family physician
//		if (getExternalReportPhysicianType() == null)
//			setExternalReportPhysicianType(ContactRole.FAMILY_PHYSICIAN);
//
//		// changing the time of signature if 0
//		if (getDateOfReport() == 0)
//			setDateOfReport(System.currentTimeMillis());
//
//		// initializes teh task
//		taskDAO.initializeCouncilData(task);
//		taskDAO.initializeDiagnosisData(task);
//
//		taskDAO.initializeTaskData(task);
//
//		// also initializing taskHandlerAction, generating lists to choos
//		// physicians from
//		taskHandlerAction.initBean();
	}

	/**
	 * Clears all values of the bean.
	 */
	public void clearData() {
//		setTaskToPrint(null);
//		setTemplates(null);
//		setSelectedTemplate(null);
//		setTemplateTransformer(null);
//		setTmpPdfContainer(null);
	}

	/********************************************************
	 * Upload
	 ********************************************************/
	public void preparePdfUploadDialog(Task task) {
		setUploadedFile(null);
		setTaskToPrint(task);
//		setUploadedFileType(PdfTemplate.OTHER);
		mainHandlerAction.showDialog(Dialog.UPLOAD);
	}

	public void saveUploadedPdfToTask(PDFContainer container, Task task) {
		container = getUploadedFile();
		if (container != null) {
			System.out.println(container.getId());
			taskDAO.initializeTaskData(task);

			genericDAO.save(container, resourceBundle.get("log.patient.task.pdf.uploded", container.getName()),
					task.getPatient());

			task.addReport(container);

			genericDAO.save(task,
					resourceBundle.get("log.patient.task.pdf.attached", task.getTaskID(), container.getName()),
					task.getPatient());

			mainHandlerAction.hideDialog(Dialog.UPLOAD);
		}
	}

	public void onChangeFileType() {
//		if (getUploadedFile() != null) {
//			getUploadedFile().setType(getUploadedFileType());
//
//			// generating a name
//			if (getUploadedFileType() != null && !getUploadedFileType().equals(PdfTemplate.OTHER)) {
//				String pdfName = "";
//				System.out.println(getUploadedFileType());
//				if (getUploadedFileType().equals(PdfTemplate.BIOBANK)) {
//					pdfName = resourceBundle.get("json.pdfTemplate.biobank") + "_"
//							+ getTaskToPrint().getPatient().getPiz() + ".pdf";
//				} else if (getUploadedFileType().equals(PdfTemplate.UREPORT)) {
//					pdfName = resourceBundle.get("json.pdfTemplate.council") + "_"
//							+ getTaskToPrint().getPatient().getPiz() + ".pdf";
//				}
//				getUploadedFile().setName(pdfName);
//			}
//		}
	}

	/**
	 * Handles the uploaded pdf orderLetter.
	 * 
	 * @param event
	 */
	public void uploadPdfForTask(FileUploadEvent event) {
//		PDFContainer requestReport = new PDFContainer();
//		// requestReport.setType("application/pdf");
//		requestReport.setData(event.getFile().getContents());
//		requestReport.setName(event.getFile().getFileName());
//
//		if (getUploadedFileType() != null || !getUploadedFileType().isEmpty()) {
//			requestReport.setType(getUploadedFileType());
//		}
//
//		setUploadedFile(requestReport);
//
//		onChangeFileType();

	}

	/********************************************************
	 * Upload
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

	public PDFContainer getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(PDFContainer uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public String getUploadedFileType() {
		return uploadedFileType;
	}

	public void setUploadedFileType(String uploadedFileType) {
		this.uploadedFileType = uploadedFileType;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
