package org.histo.template.ui.documents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.DiagnosisReport;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.pdf.PDFGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

			getContactList().add(new ContactSelector(task,
					new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));

			getContactList().add(new ContactSelector(task,
					new Person(resourceBundle.get("dialog.print.blankAddress"), new Contact()), ContactRole.NONE, true,
					true));
		} else
			setContactList(contactList);

	}

	/**
	 * Return default template configuration for printing
	 */
	public DocumentTemplate getDefaultTemplateConfiguration() {
		documentTemplate.initData(task, Arrays.asList(selectedDiagnosis), renderSelectedContact ? getAddressOfFirstSelectedContact() : "");
		return documentTemplate;
	}

	public boolean hasNextTemplateConfiguration() {
		return false;
	}
	
	public DocumentTemplate getNextTemplateConfiguration() {
		return null;
	}
}
