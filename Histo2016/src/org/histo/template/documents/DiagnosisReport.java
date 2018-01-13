package org.histo.template.documents;

import java.util.List;
import java.util.stream.Collectors;

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

	/**
	 * Updates the pdf content if a associatedContact was chosen for the first time
	 */
	public void onChooseContact(ContactSelector container) {

		// // contact is selected
		// if (container.isSelected()) {
		//
		// // setting as rendered if nothing is rendered
		// if (getRenderedContact() == null) {
		// // generating custom name if organization was selected
		//
		// container.generateAddress(true);
		// setRenderedContact(container);
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// return;
		// }
		//
		// // rerendering if organization has chagned
		// if (container.isOrganizationHasChagned()) {
		// container.setOrganizationHasChagned(false);
		//
		// container.generateAddress(true);
		//
		// // updating beacause container is selected and rendered
		// if (getRenderedContact() == container) {
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// }
		// return;
		// }
		//
		// // if only one address should be selectable
		// if (isSingleAddressSelectMode()) {
		//
		// // deselecting all other containers
		// for (ContactSelector contactContainer : contactList) {
		// if (contactContainer != container && contactContainer.isSelected()) {
		// contactContainer.setSelected(false);
		// }
		// }
		//
		// // rendering if not already rendered
		// if (getRenderedContact() != container) {
		// container.generateAddress(true);
		// setRenderedContact(container);
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// }
		//
		// return;
		// }
		//
		// } else {
		//
		// // only refresh if the rendered contact was deselected
		// if (getRenderedContact() == container) {
		// // deslecting contact and setting the first selected one
		// for (ContactSelector contactContainer : contactList) {
		// if (contactContainer.isSelected()) {
		// setRenderedContact(contactContainer);
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// return;
		// }
		// }
		// setRenderedContact(null);
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// return;
		// }
		// }
	}

	public void onChooseOrganizationOfContact(ContactSelector.OrganizationChooser chooser) {
		// if (chooser.isSelected()) {
		// // only one organization can be selected, removing other
		// // organizations
		// // from selection
		// if (chooser.getParent().isSelected()) {
		// for (ContactSelector.OrganizationChooser organizationChooser :
		// chooser.getParent()
		// .getOrganizazionsChoosers()) {
		// if (organizationChooser != chooser) {
		// organizationChooser.setSelected(false);
		// }
		// }
		// chooser.getParent().setOrganizationHasChagned(true);
		// } else {
		// // setting parent as selected
		// chooser.getParent().setSelected(true);
		// }
		// } else {
		// chooser.getParent().setOrganizationHasChagned(true);
		// }
		//
		// onChooseContact(chooser.getParent());
	}

	public void onChangeAddressManually(ContactSelector container) {
		// if (dialogHandlerAction.getCustomAddressDialog().isAddressChanged()) {
		// if (getRenderedContact() == container) {
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// }
		// }
	}

	public void initData(Patient patient, Task task, String toSendAddress) {
		this.patient = patient;
		this.task = task;
		this.toSendAddress = toSendAddress;
	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("diagnosisRevisions",
				getDiagnosisRevisions().stream().filter(p -> p.isSelected()).collect(Collectors.toList()));
		generator.getConverter().replace("address",
				HistoUtil.isNotNullOrEmpty(toSendAddress) ? (new TextToLatexConverter()).convertToTex(toSendAddress)
						: " ");
		generator.getConverter().replace("subject", "");
		generator.getConverter().replace("date", new DateTool());
		generator.getConverter().replace("latexTextConverter", new TextToLatexConverter());

	}
}
