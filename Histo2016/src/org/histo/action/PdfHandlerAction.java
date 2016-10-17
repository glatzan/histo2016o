package org.histo.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

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
import org.histo.util.ResourceBundle;
import org.histo.util.TimeUtil;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
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
		setTaskToPrint(task);

		if (getTemplates() == null) {
			setTemplates(PdfTemplate.values());
		}

		// setting the default selected template
		setSelectedTemplate(PdfTemplate.getDefaultTemplate());

		// setting the listtransformer
		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));

		taskDAO.initializePdfData(task);

		setPdfContent(generatePDF(task, getSelectedTemplate()));
		
		mainHandlerAction.showDialog(Dialog.PRINT);
	}

	public void hidePrintDialog() {
		setTaskToPrint(null);
		mainHandlerAction.hideDialog(Dialog.PRINT);
	}

	public void changeTemplate(Task task, PdfTemplate template) {
		setPdfContent(generatePDF(task, template));
		System.out.println("chagne task");
	}

	public StreamedContent generatePDF(Task task, PdfTemplate template) {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			PDFContainer pdf = generatePdfForTemplate(task, template);
			return new DefaultStreamedContent(new ByteArrayInputStream(pdf.getData()), "application/pdf");
		}
	}

	private PDFContainer generatePdfForTemplate(Task task, PdfTemplate template) {
		if (task.isDiagnosisCompleted()) {
			return null;
		} else {
			if (!template.isStaticDocument()) {
				PdfReader balnkPdf = FileUtil.loadPDFFile(template.getFileWithLogo());
				byte[] byteOfPdf = populatePdf(balnkPdf, task);
				PDFContainer generatedPdf = new PDFContainer(template, byteOfPdf);
				return generatedPdf;
			} else {
				PDFContainer pdf = task.getReport(template);
				if (pdf == null)
					pdf = new PDFContainer(template, generatePlaceholderPdf("Bitte hochladen"));
				return pdf;
			}

		}
	}

	public final byte[] populatePdf(PdfReader pdf, Task task) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			PdfStamper stamper = new PdfStamper(pdf, out);

			stamper.setFormFlattening(true);

			stamper.getAcroFields().setField("I_Name",
					task.getParent().getPerson().getName() + ", " + task.getParent().getPerson().getSurname());
			stamper.getAcroFields().setField("I_Birthday", TimeUtil.formatDate(
					task.getParent().getPerson().getBirthday(), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));
			stamper.getAcroFields().setField("I_Insurance", task.getParent().getInsurance());
			stamper.getAcroFields().setField("I_PIZ_CODE", task.getParent().getPiz());
			stamper.getAcroFields().setField("I_PIZ", task.getParent().getPiz());
			stamper.getAcroFields().setField("I_Date", TimeUtil.formatDate(new Date(System.currentTimeMillis()),
					HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));

			List<Sample> samples = task.getSamples();

			StringBuffer material = new StringBuffer();
			StringBuffer diagonsisList = new StringBuffer();

			for (Sample sample : samples) {
				material.append(sample.getSampleID() + " " + sample.getMaterial() + "\r\n");
				diagonsisList.append(sample.getSampleID() + " " + sample.getLastRelevantDiagnosis().getDiagnosis());
			}

			stamper.getAcroFields().setField("I_SAMPLES", material.toString());
			stamper.getAcroFields().setField("I_EDATE",
					TimeUtil.formatDate(task.getDateOfSugeryAsDate(), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));

			stamper.getAcroFields().setField("I_TASK_NUMBER_CODE", task.getTaskID());
			stamper.getAcroFields().setField("I_TASK_NUMBER", task.getTaskID());

			stamper.getAcroFields().setField("I_EYE", resourceBundle.get("enum.eye." + task.getEye().toString()));

			stamper.getAcroFields().setField("I_HISTORY", task.getCaseHistory());
			stamper.getAcroFields().setField("C_INSURANCE_NORMAL",
					task.getPatient().isPrivateInsurance() ? "no" : "yes");
			stamper.getAcroFields().setField("C_INSURANCE_PRIVAT",
					task.getPatient().isPrivateInsurance() ? "yes" : "no");
			stamper.getAcroFields().setField("I_WARD", task.getWard());

			stamper.getAcroFields().setField("C_MALIGN", task.isMalign() ? "yes" : "no");

			Contact privatePhysician = task.getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
			Contact surgeon = task.getPrimaryContact(ContactRole.SURGEON);

			stamper.getAcroFields().setField("I_RESIDENT_DOCTOR_FAX",
					privatePhysician == null ? "" : privatePhysician.getPhysician().getFullName());
			stamper.getAcroFields().setField("I_SURGEON", surgeon == null ? "" : surgeon.getPhysician().getFullName());

			stamper.getAcroFields().setField("I_DIAGNOSIS_EXTENDED", task.getHistologicalRecord());
			stamper.getAcroFields().setField("I_DIAGNOSIS", diagonsisList.toString());

			if (task.getSignatures() != null) {
				if (task.getSignatures().getSignatureLeft() != null) {
					stamper.getAcroFields().setField("I_PHYSICIAN",
							task.getSignatures().getSignatureLeft().getPhysician().getFullName());
					stamper.getAcroFields().setField("I_PHYSICIAN", task.getSignatures().getSignatureLeft().getRole());
				}

				if (task.getSignatures().getSigantureRight() != null) {
					stamper.getAcroFields().setField("I_CONSULTANT",
							task.getSignatures().getSigantureRight().getPhysician().getFullName());
					stamper.getAcroFields().setField("I_CONSULTANT_ROLE",
							task.getSignatures().getSigantureRight().getRole());
				}
			}

			stamper.close();
			pdf.close();

			// FileOutputStream fos = new
			// FileOutputStream("Q:\\AUG-T-HISTO\\Formulare\\ergebnis-test.pdf");
			//
			// fos.write(out.toByteArray());
			//
			// fos.close();

		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return out.toByteArray();
	}

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
		return pdfContent;
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
