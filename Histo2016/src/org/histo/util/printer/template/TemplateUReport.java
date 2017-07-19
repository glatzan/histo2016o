package org.histo.util.printer.template;

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
		setClassName("TemplateUReport");
		this.patient = patient;
		this.task = task;
	}
	
	public PDFContainer generatePDF() {
		PDFGenerator generator = new PDFGenerator(this);
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		return generator.generatePDF();
	}
}
