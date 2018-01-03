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
import org.histo.ui.LazyPDFGuiManager;
import org.histo.ui.interfaces.PdfStreamProvider;
import org.histo.util.PDFGenerator;
import org.histo.util.PDFUtil;
import org.histo.util.interfaces.LazyPDFReturnHandler;
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
public class ReportViewHandlerAction {
	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	/**
	 * Manager for rendering the pdf lazy style
	 */
	private LazyPDFGuiManager guiManager = new LazyPDFGuiManager();

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReportViewHandlerAction for task");

		guiManager.reset();

		if (task.getDiagnosisCompletionDate() == 0) {
			guiManager.setRenderComponent(false);
		} else {
			guiManager.setRenderComponent(true);

			PDFContainer c = PDFUtil.getLastPDFofType(task, DocumentType.DIAGNOSIS_REPORT_COMPLETED);

			if (c == null) {
				// no document found, rendering diagnosis report in background thread
				DocumentTemplate diagnosisTemplate = DocumentTemplate
						.getDefaultTemplate(DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT));

				if (diagnosisTemplate != null) {
					TemplateDiagnosisReport template = (TemplateDiagnosisReport) diagnosisTemplate;

					template.initData(task.getPatient(), task, "");

					guiManager.startRendering(template);
				}
			} else {
				guiManager.setManuallyCreatedPDF(c);
			}
		}

	}
}