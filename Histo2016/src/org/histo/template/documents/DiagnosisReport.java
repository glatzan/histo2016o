package org.histo.template.documents;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.ui.documents.DiagnosisReportUi;
import org.histo.util.HistoUtil;
import org.histo.util.latex.TextToLatexConverter;
import org.histo.util.pdf.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisReport extends DocumentTemplate {

	private String toSendAddress;

	private List<DiagnosisRevision> diagnosisRevisions;

	public void initData(Task task, String toSendAddress) {
		List<DiagnosisRevision> reports = new ArrayList<DiagnosisRevision>();
		
		// selecting last diagnosis for rendering
		if (task.getDiagnosisRevisions().size() > 0) {
			reports.add(task.getDiagnosisRevisions()
					.get(task.getDiagnosisRevisions().size() - 1));
		}

		initData(task, reports, toSendAddress);
	}

	public void initData(Task task, List<DiagnosisRevision> diagnosisRevisions, String toSendAddress) {
		super.initData(task);
		this.toSendAddress = toSendAddress;
		this.diagnosisRevisions = diagnosisRevisions;
	}

	public DiagnosisReportUi getDocumentUi() {
		return new DiagnosisReportUi(this);
	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);

		generator.getConverter().replace("diagnosisRevisions", diagnosisRevisions);
		generator.getConverter().replace("address",
				HistoUtil.isNotNullOrEmpty(toSendAddress) ? (new TextToLatexConverter()).convertToTex(toSendAddress)
						: " ");
		generator.getConverter().replace("subject", "");
		generator.getConverter().replace("date", new DateTool());
		generator.getConverter().replace("latexTextConverter", new TextToLatexConverter());

	}

}
