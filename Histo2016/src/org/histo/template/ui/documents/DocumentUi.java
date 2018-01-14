package org.histo.template.ui.documents;

import javax.persistence.Transient;

import org.histo.config.ResourceBundle;
import org.histo.model.interfaces.HasID;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.pdf.PDFGenerator;
import org.histo.util.pdf.PrintOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configurable
public class DocumentUi<T extends DocumentTemplate> implements HasID {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	protected ResourceBundle resourceBundle;

	protected T documentTemplate;

	protected Task task;

	/**
	 * True if task is set
	 */
	protected boolean initialized;

	/**
	 * String for input include
	 */
	protected String inputInclude = "include/empty.xhtml";

	public DocumentUi(T documentTemplate) {
		this.documentTemplate = documentTemplate;
	}

	public void initialize(Task task) {
		this.task = task;
	}

	public boolean hasNextTemplateConfiguration() {
		return false;
	}
	
	public DocumentTemplate getNextTemplateConfiguration() {
		return null;
	}
	
	public DocumentTemplate getDefaultTemplateConfiguration() {
		documentTemplate.initData(task);
		return documentTemplate;
	}

	@Override
	public long getId() {
		return documentTemplate.getId();
	}

}
