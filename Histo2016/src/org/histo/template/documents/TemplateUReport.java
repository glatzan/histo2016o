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
public class TemplateUReport extends DocumentTemplate {

	private Patient patient;

	private Task task;

	public void initData(Patient patient, Task task) {
		this.patient = patient;
		this.task = task;
	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("date", new DateTool());
	}
}
