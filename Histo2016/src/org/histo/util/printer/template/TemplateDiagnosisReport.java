package org.histo.util.printer.template;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.AssociatedContact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

public class TemplateDiagnosisReport extends AbstractTemplate {

	@Getter
	@Setter
	private Patient patient;
	
	@Getter
	@Setter
	private Task task;

	@Getter
	@Setter
	private AssociatedContact toSendAddress;

	public TemplateDiagnosisReport() {
	}

	public PDFContainer generatePDF(PDFGenerator generator) {
		generator.openNewPDf(this);
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("addressee", toSendAddress);
		generator.getConverter().replace("subject", "");
		generator.getConverter().replace("date", new DateTool());
		
		return generator.generatePDF();
	}
}
