package org.histo.action.view;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.DiagnosisReport;
import org.histo.ui.LazyPDFGuiManager;
import org.histo.ui.transformer.DefaultTransformer;
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

	private Task task;

	/**
	 * Manager for rendering the pdf lazy style
	 */
	private LazyPDFGuiManager guiManager = new LazyPDFGuiManager();

	/**
	 * List of all diagnoses
	 */
	private List<DiagnosisRevision> diagnoses;

	/**
	 * Transformer for diagnoses
	 */
	private DefaultTransformer<DiagnosisRevision> diagnosesTransformer;

	/**
	 * Selected diangosis
	 */
	private DiagnosisRevision selectedDiagnosis;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReportViewHandlerAction for task");
		
		this.task = task;
		
		if (task.getDiagnosisCompletionDate() == 0) {
			guiManager.reset();
			guiManager.setRenderComponent(false);
		} else {

			setDiagnoses(task.getDiagnosisRevisions());
			setDiagnosesTransformer(new DefaultTransformer<>(getDiagnoses()));

			// getting last diagnosis
			setSelectedDiagnosis(getDiagnoses().get(getDiagnoses().size() - 1));

			onChangeDiagnosis();
			
			// PDFContainer c = PDFUtil.getLastPDFofType(task,
			// DocumentType.DIAGNOSIS_REPORT_COMPLETED);
			//
			// if (c == null) {
			// // no document found, rendering diagnosis report in background thread
			//
			// } else {
			// guiManager.setManuallyCreatedPDF(c);
			// }
		}

	}

	public void onChangeDiagnosis() {

		guiManager.reset();
		
		guiManager.setRenderComponent(true);

		DiagnosisReport report = DocumentTemplate
				.getTemplateByID(globalSettings.getDefaultDocuments().getDiagnosisReportForUsers());

		if (report != null) {

			report.initData(task, Arrays.asList(selectedDiagnosis), "");

			guiManager.startRendering(report);
		}
	}
}
