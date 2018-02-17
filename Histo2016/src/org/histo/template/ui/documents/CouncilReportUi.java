package org.histo.template.ui.documents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.CouncilReport;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configurable
public class CouncilReportUi extends AbsctractContactUi<CouncilReport> {

	/**
	 * List of all councils
	 */
	private List<Council> councilList;

	/**
	 * Transformer for council
	 */
	private DefaultTransformer<Council> councilListTransformer;

	/**
	 * Council to print
	 */
	private Council selectedCouncil;

	public CouncilReportUi(CouncilReport templateCouncil) {
		super(templateCouncil);
		inputInclude = "include/councilReport.xhtml";
	}

	public void initialize(Task task) {
		this.initialize(task, new Council());
	}

	public void initialize(Task task, Council council) {
		super.initialize(task);

		setCouncilList(task.getCouncils());
		setCouncilListTransformer(new DefaultTransformer<Council>(councilList));

		setSelectedCouncil(council);

		updateContactList();
	}

	/**
	 * Updates the contact list for the selected council
	 */
	public void updateContactList() {
		// contacts for printing
		setContactList(new ArrayList<ContactSelector>());

		// only one adress so set as chosen
		if (getSelectedCouncil() != null && getSelectedCouncil().getCouncilPhysician() != null) {
			ContactSelector chosser = new ContactSelector(task, getSelectedCouncil().getCouncilPhysician().getPerson(),
					ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactList().add(chosser);
		}

		getContactList().add(new ContactSelector(task,
				new Person("Individuelle Addresse", new Contact()), ContactRole.NONE));
		
//		getContactList().add(new ContactSelector(task,
//				new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));
	}

	/**
	 * Return default template configuration for printing
	 */
	public DocumentTemplate getDefaultTemplateConfiguration() {
		documentTemplate.initData(task, selectedCouncil,
				renderSelectedContact ? getAddressOfFirstSelectedContact() : "");
		return documentTemplate;
	}

	/**
	 * Sets the data for the next print 
	 */
	public DocumentTemplate getNextTemplateConfiguration() {
		documentTemplate.initData(task, selectedCouncil,
				contactListPointer != null ? contactListPointer.getCustomAddress() : "");
		documentTemplate.setCopies(contactListPointer != null ? contactListPointer.getCopies() : 1);
		return documentTemplate;
	}
}