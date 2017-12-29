package org.histo.action.view;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.documents.TemplateSendReport;
import org.histo.ui.interfaces.PdfGuiProvider;
import org.histo.util.PDFGenerator;
import org.histo.util.PDFUtil;
import org.histo.util.interfaces.PDFNonBlockingReturnHandler;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

@Controller
@Scope("session")
@Getter
@Setter
public class ReportViewHandlerAction implements PDFNonBlockingReturnHandler, PdfGuiProvider {
	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;
	
	private boolean taskNoCompleted;

	/**
	 * If true the poll element at the view page will start
	 */
	private boolean stopPoll;
	private boolean autoStartPoll;
	
	private AtomicBoolean renderReport = new AtomicBoolean(false);
	
	private PDFContainer PDFContainerToRender;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReportViewHandlerAction for task");

		renderReport.set(false);
		setAutoStartPoll(false);
		setStopPoll(true);

		if (task.getDiagnosisCompletionDate() == 0)
			setTaskNoCompleted(true);

		PDFContainer c = PDFUtil.getLastPDFofType(task, DocumentType.DIAGNOSIS_REPORT_COMPLETED);

		if (c == null) {
			
			
			setAutoStartPoll(true);
			setStopPoll(false);
			
			DocumentTemplate diagnosisTemplate = DocumentTemplate
					.getDefaultTemplate(DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT));

			if (diagnosisTemplate != null) {
				TemplateDiagnosisReport template = (TemplateDiagnosisReport) diagnosisTemplate;

				template.initData(task.getPatient(), task, "");

				template.generatePDFNoneBlocking(new PDFGenerator(), this);
			}
		}

	}

	public StreamedContent getPdfContent() {
		System.out.println("hallo getting" + getPDFContainerToRender());
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE || getPDFContainerToRender() == null) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream(getPDFContainerToRender().getData()), "application/pdf",
					getPDFContainerToRender().getName());
		}
	}
	
	@Override
	public void setPDFContent(PDFContainer container) {
		setPDFContainerToRender(container);
		logger.debug("Container set");

	}

	@Override
	public void setPDFGenerationStatus(boolean status) {
		System.out.println("thread finished " + status);
		renderReport.set(status);
		setStopPoll(true);
		setAutoStartPoll(false);
	}
	
	public void test() {
		System.out.println("test");
	}
	
	@Synchronized
	public PDFContainer getPDFContainerToRender() {
		return PDFContainerToRender;
	}
	
	public void setPDFContainerToRender(PDFContainer container) {
		this.PDFContainerToRender = container;
	}
}
