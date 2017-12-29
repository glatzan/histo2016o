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

	/**
	 * True if diagnosis completed, report will be rendered
	 */
	private boolean diagnosisCompleted;

	/**
	 * If true the poll element at the view page will start
	 */
	private boolean stopPoll;

	/**
	 * If true the poll element will start
	 */
	private boolean autoStartPoll;

	/**
	 * If true the pdf will be rendered
	 */
	private AtomicBoolean renderPDF = new AtomicBoolean(false);

	/**
	 * pdf container
	 */
	private PDFContainer PDFContainerToRender;

	/**
	 * Thread id of the last pdf generating thread
	 */
	private String currentTaskUuid;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReportViewHandlerAction for task");

		getRenderPDF().set(false);
		setAutoStartPoll(false);
		setStopPoll(true);

		if (task.getDiagnosisCompletionDate() == 0) {
			setDiagnosisCompleted(false);
		} else {
			setDiagnosisCompleted(true);

			PDFContainer c = PDFUtil.getLastPDFofType(task, DocumentType.DIAGNOSIS_REPORT_COMPLETED);

			if (c == null) {

				setAutoStartPoll(true);
				setStopPoll(false);

				DocumentTemplate diagnosisTemplate = DocumentTemplate
						.getDefaultTemplate(DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT));

				if (diagnosisTemplate != null) {
					TemplateDiagnosisReport template = (TemplateDiagnosisReport) diagnosisTemplate;

					template.initData(task.getPatient(), task, "");

					currentTaskUuid = template.generatePDFNoneBlocking(new PDFGenerator(), this);
				}
			}
		}

	}

	@Override
	@Synchronized
	public void setPDFContent(PDFContainer container, String uuid) {
		if (getCurrentTaskUuid().equals(uuid)) {
			setPDFContainerToRender(container);
			getRenderPDF().set(true);
			setStopPoll(true);
			setAutoStartPoll(false);
		} else {
			logger.debug("More then one Thread! Old Thread");
		}

	}

	public StreamedContent getPdfContent() {
		return PdfGuiProvider.super.getPdfContent();
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
