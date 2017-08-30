package org.histo.template.documents;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.AssociatedContact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateCouncil extends DocumentTemplate {

	private Patient patient;

	private Council council;

	private Task task;

	private AssociatedContact toSendAddress;

	public void initData(Patient patient, Task task, Council council, AssociatedContact toSendAddress) {
		this.patient = patient;
		this.task = task;
		this.council = council;
		this.toSendAddress = toSendAddress;
	}

	public PDFContainer generatePDF(PDFGenerator generator) {
		generator.openNewPDf(this);

		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("council", council);
		generator.getConverter().replace("addressee", toSendAddress);
		generator.getConverter().replace("date", new DateTool());

		return generator.generatePDF();
	}

}
