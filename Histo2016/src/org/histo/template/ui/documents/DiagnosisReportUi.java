package org.histo.template.ui.documents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.DiagnosisReport;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configurable
public class DiagnosisReportUi extends AbsctractContactUi<DiagnosisReport> {

	/**
	 * List of all diagnoses
	 */
	private List<DiagnosisRevision> diagnoses;

	/**
	 * Transformer for diagnoses
	 */
	private DefaultTransformer<DiagnosisRevision> diagnosesTransformer;

	/**
	 * Selected diangosis
	 */
	private DiagnosisRevision selectedDiagnosis;

	public DiagnosisReportUi(DiagnosisReport report) {
		super(report);
		inputInclude = "include/diagnosisReport.xhtml";
	}

	public void initialize(Task task) {
		initialize(task, null);
	}

	public void initialize(Task task, List<ContactSelector> contactList) {
		super.initialize(task);

		setDiagnoses(task.getDiagnosisRevisions());
		setDiagnosesTransformer(new DefaultTransformer<>(getDiagnoses()));

		// getting last diagnosis
		setSelectedDiagnosis(getDiagnoses().get(getDiagnoses().size() - 1));

		if (contactList == null) {
			// contacts for printing
			setContactList(new ArrayList<ContactSelector>());

			// setting other contacts (physicians)
			getContactList().addAll(ContactSelector.factory(task));

			System.out.println(resourceBundle);
			
			getContactList().add(new ContactSelector(task,
					new Person("Individuelle Addresse", new Contact()), ContactRole.NONE));

			getContactList().add(new ContactSelector(task,
					new Person("Leere Adresse", new Contact()), ContactRole.NONE, true,
					true));
			
//			getContactList().add(new ContactSelector(task,
//					new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));
//
//			getContactList().add(new ContactSelector(task,
//					new Person(resourceBundle.get("dialog.print.blankAddress"), new Contact()), ContactRole.NONE, true,
//					true));
		} else
			setContactList(contactList);

	}

	/**
	 * Return default template configuration for printing
	 */
	public DocumentTemplate getDefaultTemplateConfiguration() {
		documentTemplate.initData(task, Arrays.asList(selectedDiagnosis),
				renderSelectedContact ? getAddressOfFirstSelectedContact() : "");
		return documentTemplate;
	}

	/**
	 * Sets the data for the next print 
	 */
	public DocumentTemplate getNextTemplateConfiguration() {
		documentTemplate.initData(task, Arrays.asList(selectedDiagnosis),
				contactListPointer != null ? contactListPointer.getCustomAddress() : "");
		documentTemplate.setCopies(contactListPointer != null ? contactListPointer.getCopies() : 1);
		return documentTemplate;
	}
}
