package org.histo.template.documents;

import java.util.List;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.selectors.DiagnosisRevisionSelector;
import org.histo.util.HistoUtil;
import org.histo.util.latex.TextToLatexConverter;
import org.histo.util.pdf.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisReport extends DocumentTemplate {

	private Patient patient;

	private String toSendAddress;

	/**
	 * List with all associated contacts
	 */
	private List<ContactSelector> contactList;

	/**
	 * List of all diagnosis revision to select from
	 */
	private List<DiagnosisRevisionSelector> diagnosisRevisions;

	/**
	 * The associatedContact rendered, the first one will always be rendered, if not
	 * changed, no rendering necessary
	 */
	private ContactSelector selectedContact;

	public void initData(Task task) {
		super.initData(task);

		contactList = ContactSelector.factory(task);
		diagnosisRevisions = DiagnosisRevisionSelector.factory(task);

		inputInclude = "include/diagnosisReport.xhtml";

	}

	public void initData(Patient patient, Task task, String toSendAddress) {
		this.patient = patient;
		this.task = task;
		this.toSendAddress = toSendAddress;
	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("address",
				HistoUtil.isNotNullOrEmpty(toSendAddress) ? (new TextToLatexConverter()).convertToTex(toSendAddress)
						: " ");
		generator.getConverter().replace("subject", "");
		generator.getConverter().replace("date", new DateTool());
		generator.getConverter().replace("latexTextConverter", new TextToLatexConverter());

	}
}
