package org.histo.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Display;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.Task;
import org.histo.model.util.transientObjects.PDFTemplate;
import org.histo.ui.transformer.PdfTemplateTransformer;
import org.histo.util.FileUtil;
import org.histo.util.PdfUtil;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskHandlerAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1460063099758733063L;

	@Autowired
	HelperHandlerAction helper;

	@Autowired
	TaskDAO taskDAO;
	
	@Autowired
	GenericDAO genericDAO;

	private Task taskToPrint;
	
	private List<PDFTemplate> templates;

	private PDFTemplate selectedTemplate;

	private PdfTemplateTransformer templateTransformer;

	private StreamedContent pdfContent;

	private String printer;

	private int copies;

	public void preparePrintDialog(Task task) {
		setTaskToPrint(task);
		helper.showDialog(HistoSettings.DIALOG_PRINT, 1024, 600, false, false, true);
		String templateFile = FileUtil.loadTextFile(HistoSettings.PDF_TEMPLATE_JSON);
		setTemplates(Arrays.asList(PDFTemplate.factroy(templateFile)));
		setSelectedTemplate(PdfUtil.getDefaultTemplate(getTemplates()));
		setPdfContent(generatePDF(task, getSelectedTemplate()));
		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));
	}

	public void hidePrintDialog() {
		setTaskToPrint(null);
		helper.hideDialog(HistoSettings.DIALOG_PRINT);
	}
	
	public void changeTemplate(Task task, PDFTemplate template){
		setPdfContent(generatePDF(task, template));
		System.out.println("chagne task");
	}

	public StreamedContent generatePDF(Task task, PDFTemplate template) {
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
			
			PDFContainer container = PdfUtil.createPDFContainer(template, PdfUtil.populatePdf(FileUtil.loadPDFFile(template.getFileWithLogo()), task));
			
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
	
	public List<PDFTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(List<PDFTemplate> templates) {
		this.templates = templates;
	}

	public PDFTemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(PDFTemplate selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public StreamedContent getPdfContent() {
		return pdfContent;
	}

	public void setPdfContent(StreamedContent pdfContent) {
		this.pdfContent = pdfContent;
	}

	public PdfTemplateTransformer getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(PdfTemplateTransformer templateTransformer) {
		this.templateTransformer = templateTransformer;
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
// p:importEnum type="javax.faces.application.ProjectStage"
// var="JsfProjectStages" allSuffix="ALL_ENUM_VALUES" />