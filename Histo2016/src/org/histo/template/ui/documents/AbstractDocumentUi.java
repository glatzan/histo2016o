package org.histo.template.ui.documents;

import java.util.List;

import javax.persistence.Transient;

import org.histo.config.ResourceBundle;
import org.histo.model.AssociatedContact;
import org.histo.model.interfaces.HasID;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.ui.selectors.ContactSelector;
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
public class AbstractDocumentUi<T extends DocumentTemplate> implements HasID {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	protected ResourceBundle resourceBundle;

	protected T documentTemplate;

	/**
	 * TAsk
	 */
	protected Task task;

	/**
	 * True if task is set
	 */
	protected boolean initialized;

	/**
	 * If true the pdf will be updated on every settings change
	 */
	protected boolean updatePdfOnEverySettingChange = false;
	
	/**
	 * If true the first selected contact will be rendered
	 */
	protected boolean renderSelectedContact = false;
	
	/**
	 * String for input include
	 */
	protected String inputInclude = "include/empty.xhtml";

	public AbstractDocumentUi(T documentTemplate) {
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
