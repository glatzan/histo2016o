package org.histo.template.ui.documents;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.DiagnosisReport;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.selectors.DiagnosisRevisionSelector;
import org.histo.util.pdf.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisReportUi extends DocumentUi<DiagnosisReport> {

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

	public DiagnosisReportUi(DiagnosisReport report) {
		super(report);
		inputInclude = "include/diagnosisReport.xhtml";
	}

	public void initialize(Task task) {
		super.initialize(task);
		diagnosisRevisions = DiagnosisRevisionSelector.factory(task);

		if (diagnosisRevisions.size() > 0)
			diagnosisRevisions.get(diagnosisRevisions.size() - 1).setSelected(true);

		// contacts for printing
		setContactList(new ArrayList<ContactSelector>());

		// setting other contacts (physicians)
		getContactList().addAll(ContactSelector.factory(task));

		getContactList().add(new ContactSelector(task,
				new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));

		getContactList().add(
				new ContactSelector(task, new Person(resourceBundle.get("dialog.print.blankAddress"), new Contact()),
						ContactRole.NONE, true, true));

	}

	/**
	 * Return default template configuration for printing
	 */
	public DocumentTemplate getDefaultTemplateConfiguration() {
		documentTemplate.initData(task, diagnosisRevisions.stream().filter(p -> p.isSelected())
				.map(p -> p.getDiagnosisRevision()).collect(Collectors.toList()), "");
		return documentTemplate;
	}

	/**
	 * Updates the pdf content if a associatedContact was chosen for the first time
	 */
	public void onChooseContact(ContactSelector container) {
		container.generateAddress(true);

		if (!container.isSelected())
			container.getOrganizazionsChoosers().forEach(p -> p.setSelected(false));
	}

	/**
	 * Updates the person if a organization was selected or deselected
	 * 
	 * @param chooser
	 */
	public void onChooseOrganizationOfContact(ContactSelector.OrganizationChooser chooser) {
		if (chooser.isSelected()) {
			// only one organization can be selected, removing other
			// organizations
			// from selection
			if (chooser.getParent().isSelected()) {
				for (ContactSelector.OrganizationChooser organizationChooser : chooser.getParent()
						.getOrganizazionsChoosers()) {
					if (organizationChooser != chooser) {
						organizationChooser.setSelected(false);
					}
				}
			} else {
				// setting parent as selected
				chooser.getParent().setSelected(true);
			}
		}

		chooser.getParent().generateAddress(true);
	}
}
