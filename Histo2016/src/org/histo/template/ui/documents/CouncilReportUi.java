package org.histo.template.ui.documents;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.template.documents.TemplateCouncil;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.selectors.DiagnosisRevisionSelector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouncilReportUi extends DocumentUi<TemplateCouncil> {

	/**
	 * List with all associated contacts
	 */
	private List<ContactSelector> contactList;

	/**
	 * Council to print
	 */
	private Council selectedCouncil;

	public CouncilReportUi(TemplateCouncil templateCouncil) {
		super( templateCouncil);
		inputInclude = "include/councilReport.xhtml";
	}

	public void initialize(Task task) {
		initialize(task, null);
	}

	public void initialize(Task task, Council council) {
		super.initialize(task);
		setSelectedCouncil(council);

		// contacts for printing
		setContactList(new ArrayList<ContactSelector>());

		// only one adress so set as chosen
		if (council != null && council.getCouncilPhysician() != null) {
			ContactSelector chosser = new ContactSelector(task, getSelectedCouncil().getCouncilPhysician().getPerson(),
					ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactList().add(chosser);
		}

		getContactList().add(new ContactSelector(task,
				new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));

	}
}