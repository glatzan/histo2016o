package org.histo.action.view;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.DiagnosisReport;
import org.histo.ui.LazyPDFGuiManager;
import org.histo.util.pdf.PDFUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

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

				DiagnosisReport report = DocumentTemplate
						.getTemplateByID(globalSettings.getDefaultDocuments().getDiagnosisReportForUsers());

				if (report != null) {

					report.initData(task, "");
					guiManager.startRendering(report);
				}
			} else {
				guiManager.setManuallyCreatedPDF(c);
			}
		}

	}
}
