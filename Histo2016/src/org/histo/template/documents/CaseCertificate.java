package org.histo.template.documents;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.ui.documents.CaseCertificateUi;
import org.histo.util.pdf.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseCertificate extends DocumentTemplate {

	public void initData(Task task) {
		super.initData(task);
	}

	public CaseCertificateUi getDocumentUi() {
		return new CaseCertificateUi(this);
	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("date", new DateTool());
	}
}
