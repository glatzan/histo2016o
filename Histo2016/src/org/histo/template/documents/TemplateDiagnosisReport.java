package org.histo.template.documents;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.HistoUtil;
import org.histo.util.PDFGenerator;
import org.histo.util.latex.TextToLatexConverter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateDiagnosisReport extends DocumentTemplate {

	private Patient patient;

	private Task task;

	private String toSendAddress;

	public void initData(Patient patient, Task task, String toSendAddress) {
		this.patient = patient;
		this.task = task;
		this.toSendAddress = toSendAddress;
	}

	public PDFContainer generatePDF(PDFGenerator generator) {
		generator.openNewPDf(this);
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("address",
				HistoUtil.isNotNullOrEmpty(toSendAddress) ? (new TextToLatexConverter()).convertToTex(toSendAddress)
						: " ");
		generator.getConverter().replace("subject", "");
		generator.getConverter().replace("date", new DateTool());
		generator.getConverter().replace("latexTextConverter", new TextToLatexConverter());
		
		return generator.generatePDF();
	}
}
