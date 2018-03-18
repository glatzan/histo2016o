package org.histo.template.ui.documents;

import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.CaseCertificate;

public class CaseCertificateUi extends AbstractDocumentUi<CaseCertificate> {

	private boolean printed;

	public CaseCertificateUi(CaseCertificate documentTemplate) {
		super(documentTemplate);
	}

	public void initialize(Task task) {
		super.initialize(task);
	}

	public void beginNextTemplateIteration() {
		printed = false;
	}

	public boolean hasNextTemplateConfiguration() {
		return !printed;
	}

	/**
	 * Return default template configuration for printing
	 */
	public TemplateConfiguration<CaseCertificate> getDefaultTemplateConfiguration() {
		documentTemplate.initData(task);
		return new TemplateConfiguration<CaseCertificate>(documentTemplate);
	}

	/**
	 * Sets the data for the next print
	 */
	public TemplateConfiguration<CaseCertificate> getNextTemplateConfiguration() {
		printed = true;
		return getDefaultTemplateConfiguration();
	}
}
