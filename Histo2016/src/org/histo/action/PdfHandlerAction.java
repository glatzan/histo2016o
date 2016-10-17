package org.histo.action;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Dialog;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PdfTemplate;
import org.histo.ui.transformer.PdfTemplateTransformer;
import org.histo.util.FileUtil;
import org.histo.util.PdfUtil;
import org.histo.util.ResourceBundle;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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

	private List<PdfTemplate> templates;

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
		PDFContainer orderLetterPdf = new PDFContainer();
		orderLetterPdf.setType("application/pdf");
		orderLetterPdf.setData(event.getFile().getContents());
		orderLetterPdf.setName(event.getFile().getFileName());
		if (taskHandlerAction.getTemporaryTask() != null)
			getTemporaryTask().setOrderLetter(orderLetterPdf);
	}

	
	public void preparePrintDialog(Task task) {
		setTaskToPrint(task);
		
		if (getTemplates() == null) {
			String templateFile = FileUtil.loadTextFile(HistoSettings.PDF_TEMPLATE_JSON);
			setTemplates(Arrays.asList(PdfTemplate.factroy(templateFile)));
		}

		setSelectedTemplate(PdfUtil.getDefaultTemplate(getTemplates()));
		
		setPdfContent(generatePDF(task, getSelectedTemplate()));
		
		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));
		
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
			taskDAO.initializeTask(task);

			if (task.getPdfs() != null && !task.getPdfs().isEmpty()) {
				for (PDFContainer pdf : task.getPdfs()) {
					if (pdf.getType().equals(template.getType()))
						return new DefaultStreamedContent(new ByteArrayInputStream(pdf.getData()), "application/pdf");
				}
			}

			PDFContainer container = PdfUtil.createPDFContainer(template,
					PdfUtil.populatePdf(FileUtil.loadPDFFile(template.getFileWithLogo()), task));

			task.getPdfs().add(container);
			genericDAO.save(task);

			return new DefaultStreamedContent(new ByteArrayInputStream(container.getData()), "application/pdf");

		}
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

	public List<PdfTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(List<PdfTemplate> templates) {
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
