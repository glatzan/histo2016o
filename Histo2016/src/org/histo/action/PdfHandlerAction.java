package org.histo.action;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.config.HistoSettings;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PdfTemplate;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.PdfTemplateTransformer;
import org.histo.util.FileUtil;
import org.histo.util.PdfUtil;
import org.histo.util.ResourceBundle;
import org.histo.util.TimeUtil;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.Barcode;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.BarcodeEAN;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

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

	private PdfTemplate[] templates;

	private PdfTemplate selectedTemplate;

	private PdfTemplateTransformer templateTransformer;

	private Task taskToPrint;

	private StreamedContent pdfContent;

	private String printer;

	private int copies;

	/**
	 * Handles the uploaded pdf orderLetter.
	 * 
	 * @param event
	 */
	public void handleTaskRequestReport(FileUploadEvent event) {
		PDFContainer requestReport = new PDFContainer(PdfTemplate.UREPROT);
		// requestReport.setType("application/pdf");
		requestReport.setData(event.getFile().getContents());
		requestReport.setName(event.getFile().getFileName());
		if (taskHandlerAction.getTemporaryTask() != null)
			taskHandlerAction.getTemporaryTask().addReport(requestReport);
	}

	public void preparePrintDialog(Task task) {
		preparePrintDialog(task, PdfTemplate.values(), PdfTemplate.getDefaultTemplate());
	}

	public void preparePrintDialog(Task task, PdfTemplate[] templates, PdfTemplate selectedTemplate) {
		setTaskToPrint(task);

		setTemplates(templates);
		// setting the listtransformer
		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));

		setSelectedTemplate(selectedTemplate);

		taskDAO.initializePdfData(task);

		onChangeTemplate();

		mainHandlerAction.showDialog(Dialog.PRINT);
	}

	public void hidePrintDialog() {
		setTaskToPrint(null);
		mainHandlerAction.hideDialog(Dialog.PRINT);
	}

	public void onChangeTemplate() {
		ByteArrayOutputStream out;
		PdfReader pdfReader;
		PdfStamper pdf;

		switch (getSelectedTemplate()) {
		case UREPROT:
			PDFContainer container = getTaskToPrint().getReport(PdfTemplate.UREPROT);
			if (container != null)
				setPdfContent(
						new DefaultStreamedContent(new ByteArrayInputStream(container.getData()), "application/pdf"));
			else
				setPdfContent(new DefaultStreamedContent(
						new ByteArrayInputStream(generatePlaceholderPdf("Bitte Hochladen")), "application/pdf"));
			return;
		case INTERNAL_EXTENDED:
			out = new ByteArrayOutputStream();
			pdfReader = PdfUtil.getPdfFile(getSelectedTemplate().getFileWithLogo());
			pdf = PdfUtil.getPdfStamper(pdfReader, out);
			populateReportHead(pdfReader, pdf, getTaskToPrint());
			populateExtendedDiagnosis(pdfReader, pdf, getTaskToPrint());
			pdf.setFormFlattening(true);
			PdfUtil.closePdf(pdfReader, pdf);

			setPdfContent(new DefaultStreamedContent(new ByteArrayInputStream(out.toByteArray()), "application/pdf"));
			System.out.println("council");
			return;
		case COUNCIL:
			taskDAO.initializeCouncilData(getTaskToPrint());
			out = new ByteArrayOutputStream();
			pdfReader = PdfUtil.getPdfFile(getSelectedTemplate().getFileWithLogo());
			pdf = PdfUtil.getPdfStamper(pdfReader, out);
			populateReportHead(pdfReader, pdf, getTaskToPrint());
			populateReportCouncil(pdf, getTaskToPrint());
			pdf.setFormFlattening(true);
			PdfUtil.closePdf(pdfReader, pdf);

			setPdfContent(new DefaultStreamedContent(new ByteArrayInputStream(out.toByteArray()), "application/pdf"));
			System.out.println("council");
			return;
		default:
			out = new ByteArrayOutputStream();
			pdfReader = PdfUtil.getPdfFile(getSelectedTemplate().getFileWithLogo());
			pdf = PdfUtil.getPdfStamper(pdfReader, out);
			populateReportHead(pdfReader, pdf, getTaskToPrint());
			populateReportCouncil(pdf, getTaskToPrint());
			pdf.setFormFlattening(true);
			PdfUtil.closePdf(pdfReader, pdf);

			setPdfContent(new DefaultStreamedContent(new ByteArrayInputStream(out.toByteArray()), "application/pdf"));
			System.out.println("council");
			return;
		}
	}

	public final void populateReportHead(PdfReader reader, PdfStamper stamper, Task task) {

		try {
			stamper.getAcroFields().setField("H_Name",
					task.getParent().getPerson().getName() + ", " + task.getParent().getPerson().getSurname());
			stamper.getAcroFields().setField("H_Birthday", resourceBundle.get("pdf.birthday") + " " + TimeUtil
					.formatDate(task.getParent().getPerson().getBirthday(), HistoSettings.STANDARD_DATEFORMAT_GERMAN));
			stamper.getAcroFields().setField("H_ADDRESS", task.getParent().getInsurance());

			if (!task.getParent().getPiz().isEmpty())
				PdfUtil.generateCode128Field(reader, stamper, String.valueOf(task.getParent().getPiz()), 40f, 0.95f, 70,
						105);

			stamper.getAcroFields().setField("H_PIZ",
					task.getParent().getPiz().isEmpty() ? resourceBundle.get("pdf.noPIZ") : task.getParent().getPiz());
			stamper.getAcroFields().setField("H_Date", TimeUtil.formatDate(new Date(System.currentTimeMillis()),
					HistoSettings.STANDARD_DATEFORMAT_GERMAN));
		} catch (IOException | DocumentException e) {
			e.printStackTrace();
		}
	}

	public final void populateExtendedDiagnosis(PdfReader reader, PdfStamper stamper, Task task) {

		List<Sample> samples = task.getSamples();

		StringBuffer material = new StringBuffer();
		StringBuffer diagonsisList = new StringBuffer();

		for (Sample sample : samples) {
			material.append(sample.getSampleID() + " " + sample.getMaterial() + "\r\n");
			diagonsisList.append(sample.getSampleID() + " " + sample.getLastRelevantDiagnosis().getDiagnosis() + "\r\n");
		}

		try {
			stamper.getAcroFields().setField("B_SAMPLES", material.toString());

			stamper.getAcroFields().setField("B_EDATE",
					TimeUtil.formatDate(task.getDateOfSugeryAsDate(), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));

			PdfUtil.generateCode128Field(reader, stamper, String.valueOf(task.getTaskID()), 25f, 1.3f, 453, 345);
			stamper.getAcroFields().setField("B_TASK_NUMBER", task.getTaskID());

			stamper.getAcroFields().setField("B_EYE", resourceBundle.get("enum.eye." + task.getEye().toString()));

			stamper.getAcroFields().setField("B_HISTORY", task.getCaseHistory());
			stamper.getAcroFields().setField("B_INSURANCE_NORMAL", task.getPatient().isPrivateInsurance() ? "0" : "1");
			stamper.getAcroFields().setField("B_INSURANCE_PRIVATE", task.getPatient().isPrivateInsurance() ? "1" : "0");
			stamper.getAcroFields().setField("B_WARD", task.getWard());

			stamper.getAcroFields().setField("B_MALIGN", task.isMalign() ? "1" : "0");

			Contact privatePhysician = task.getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
			Contact surgeon = task.getPrimaryContact(ContactRole.SURGEON);

			stamper.getAcroFields().setField("B_PRIVATE_PHYSICIAN",
					privatePhysician == null ? "" : privatePhysician.getPhysician().getFullName());
			stamper.getAcroFields().setField("B_SURGEON", surgeon == null ? "" : surgeon.getPhysician().getFullName());

			stamper.getAcroFields().setField("B_HISTOLOGICAL_RECORD", task.getReport().getHistologicalRecord());
			stamper.getAcroFields().setField("B_DIAGNOSIS", diagonsisList.toString());

			stamper.getAcroFields().setField("B_DATE", TimeUtil.formatDate(new Date(System.currentTimeMillis()),
					HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));

			if (task.getReport() != null) {
				if (task.getReport().getSignatureLeft() != null) {
					stamper.getAcroFields().setField("S_PHYSICIAN",
							task.getReport().getSignatureLeft().getPhysician().getFullName());
					stamper.getAcroFields().setField("S_PHYSICIAN_ROLE", task.getReport().getSignatureLeft().getRole());
				}

				if (task.getReport().getSignatureRight() != null) {
					stamper.getAcroFields().setField("S_CONSULTANT",
							task.getReport().getSignatureRight().getPhysician().getFullName());
					stamper.getAcroFields().setField("S_CONSULTANT_ROLE",
							task.getReport().getSignatureRight().getRole());
				}
			}
		} catch (IOException | DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// // HashMap<?, ?> test = stamper.getAcroFields().getFields();
	// // for(HashMap<?, ?> fields : test){
	// //
	// // }
	// AcroFields fields = stamper.getAcroFields();
	//
	// Set<String> fldNames = fields.getFields().keySet();
	//
	// for (String fldName : fldNames) {
	// System.out.println(fldName + ": " + fields.getField(fldName));
	// }

	public final void populateReportCouncil(PdfStamper stamper, Task task) {
		// TODO B_DATE
		try {
			stamper.getAcroFields().setField("B_TASK_NUMBER_CODE", task.getTaskID());

			stamper.getAcroFields().setField("B_NAME",
					task.getParent().getPerson().getName() + ", " + task.getParent().getPerson().getSurname());
			stamper.getAcroFields().setField("B_BIRTHDAY", TimeUtil.formatDate(
					task.getParent().getPerson().getBirthday(), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));
			stamper.getAcroFields().setField("B_PIZ", task.getParent().getPiz());

			stamper.getAcroFields().setField("B_TEXT", task.getCouncil().getCouncilText());
			// stamper.getAcroFields().setField("B_SIGANTURE",
			// task.getCouncil().getCouncilPhysician().getFullName());
			// stamper.getAcroFields().setField("B_APPENDIX",
			// task.getCouncil().getAttachment());
		} catch (IOException | DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// B_TASK_NUMBER_CODE
	// B_PIZ
	// B_NAME
	// B_BIRTHDAY
	// B_DATE
	// B_TEXT
	// B_SIGANTURE
	// B_APPENDIX

	public final byte[] generatePlaceholderPdf(String text) {
		Document document = new Document();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			PdfWriter.getInstance(document, outputStream);
			document.open();
			document.add(new Paragraph(text));
			document.close();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return outputStream.toByteArray();
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
			System.out.println("reder repsonse");
			return new DefaultStreamedContent();
		} else {
			System.out.println("PDf " + pdfContent);
			return pdfContent;
		}
	}

	public void setPdfContent(StreamedContent pdfContent) {
		this.pdfContent = pdfContent;
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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
