package org.histo.util.printer.template;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

public class TemplateUReport extends AbstractTemplate {

	@Getter
	@Setter
	private Patient patient;
	
	@Getter
	@Setter
	private Task task;

	public TemplateUReport() {
		this(null, null);
	}

	public TemplateUReport(Patient patient, Task task) {
		this.patient = patient;
		this.task = task;
	}
	
	public PDFContainer generatePDF(PDFGenerator generator) {
		generator.openNewPDf(this);
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("date", new DateTool());
		return generator.generatePDF();
	}
}
