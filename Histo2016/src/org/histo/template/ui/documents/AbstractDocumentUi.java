package org.histo.template.ui.documents;

import org.histo.config.ResourceBundle;
import org.histo.model.AssociatedContact;
import org.histo.model.interfaces.HasID;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
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
	
	public void beginNextTemplateIteration() {
	}
	
	public boolean hasNextTemplateConfiguration() {
		return false;
	}
	
	public TemplateConfiguration<T> getNextTemplateConfiguration() {
		return null;
	}
	
	public TemplateConfiguration<T> getDefaultTemplateConfiguration() {
		documentTemplate.initData(task);
		return new TemplateConfiguration<T>(documentTemplate);
	}

	@Override
	public long getId() {
		return documentTemplate.getId();
	}

	/**
	 * Return container for generated template
	 * @author andi
	 *
	 */
	@Getter
	@Setter
	public class TemplateConfiguration<I extends DocumentTemplate> {
		private I documentTemplate;
		private AssociatedContact contact;
		private String address;
		
		public TemplateConfiguration(I documentTemplate) {
			this(documentTemplate, null, "");
		}
		
		public TemplateConfiguration(I documentTemplate, AssociatedContact contact, String address) {
			this.documentTemplate = documentTemplate;
			this.contact = contact;
			this.address = address;
		}
	}
}
