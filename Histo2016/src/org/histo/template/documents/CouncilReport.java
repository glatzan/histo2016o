package org.histo.template.documents;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.ui.documents.CouncilReportUi;
import org.histo.template.ui.documents.DiagnosisReportUi;
import org.histo.util.HistoUtil;
import org.histo.util.latex.TextToLatexConverter;
import org.histo.util.pdf.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouncilReport extends DocumentTemplate {

	private Council council;

	private String toSendAddress;

	public void initData(Task task, Council council, String toSendAddress) {
		super.initData(task);
		this.council = council;
		this.toSendAddress = toSendAddress;
	}

	public CouncilReportUi getDocumentUi() {
		return new CouncilReportUi(this);
	}
	
	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("council", council);
		generator.getConverter().replace("address",
				HistoUtil.isNotNullOrEmpty(toSendAddress) ? (new TextToLatexConverter()).convertToTex(toSendAddress)
						: " ");
		generator.getConverter().replace("date", new DateTool());
	}

}
