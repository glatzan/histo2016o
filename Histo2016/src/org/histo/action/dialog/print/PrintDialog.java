package org.histo.action.dialog.print;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateCouncil;
import org.histo.template.documents.DiagnosisReport;
import org.histo.template.documents.TemplateUReport;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.StreamUtils;
import org.histo.util.pdf.PDFGenerator;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PrintDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	/**
	 * List of all templates for printing
	 */
	private List<DocumentTemplate> templateList;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<DocumentTemplate> templateTransformer;

	/**
	 * Selected template for printing
	 */
	private DocumentTemplate selectedTemplate;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer pdfContainer;

	/**
	 * List with all associated contacts
	 */
	private List<ContactSelector> contactList;

	/**
	 * The associatedContact rendered, the first one will always be rendered, if not
	 * changed, no rendering necessary
	 */
	private ContactSelector renderedContact;

	/**
	 * True if the pdf should be rendered
	 */
	private boolean renderPdf;

	/**
	 * Council to print
	 */
	private Council selectedCouncil;

	/**
	 * Can be set to true, if so the generated pdf will be saved
	 */
	private boolean savePDF;

	/**
	 * if true no print button, but instead a select button will be display
	 */
	private boolean selectMode;

	/**
	 * If true only on address can be selected
	 */
	private boolean singleAddressSelectMode;

	/**
	 * If true at certain address changes the pdfs will be regenerated
	 */
	private boolean autoRefresh;

	/**
	 * If true a fax button will be displayed
	 */
	private boolean faxMode;

	/**
	 * Initializes the bean and shows the council dialog
	 * 
	 * @param task
	 */
	public void initAndPrepareBeanForPrinting(Task task) {
		initBeanForPrinting(task);
		prepareDialog();
	}

	public void initBeanForPrinting(Task task) {

		DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
				DocumentType.U_REPORT, DocumentType.U_REPORT_EMTY, DocumentType.DIAGNOSIS_REPORT_EXTERN);

		initBean(task, subSelect, DocumentTemplate.getDefaultTemplate(subSelect));

		// contacts for printing
		setContactList(new ArrayList<ContactSelector>());

		// setting other contacts (physicians)
		getContactList().addAll(ContactSelector.factory(task));

		getContactList().add(new ContactSelector(task,
				new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));
		getContactList().add(
				new ContactSelector(task, new Person(resourceBundle.get("dialog.print.blankAddress"), new Contact()),
						ContactRole.NONE, true, true));

		setRenderedContact(null);

		setSelectMode(false);

		setFaxMode(true);

		setSavePDF(true);

		setSingleAddressSelectMode(false);

		setAutoRefresh(false);

		// rendering the template
		onChangePrintTemplate(true);
	}

	public void initAndPrepareBeanForCouncil(Task task, Council council) {
		initBeanForCouncil(task, council);
		prepareDialog();
	}

	public void initBeanForCouncil(Task task, Council council) {
		DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.COUNCIL_REQUEST);

		initBean(task, subSelect, DocumentTemplate.getDefaultTemplate(subSelect));

		setSelectedCouncil(council);

		// contacts for printing
		setContactList(new ArrayList<ContactSelector>());

		// only one adress so set as chosen
		if (getSelectedCouncil().getCouncilPhysician() != null) {
			ContactSelector chosser = new ContactSelector(task, getSelectedCouncil().getCouncilPhysician().getPerson(),
					ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactList().add(chosser);

			// setting council physicians data as rendere associatedContact data
			setRenderedContact(chosser);
		}

		getContactList().add(new ContactSelector(task,
				new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));

		setSelectMode(false);

		setSingleAddressSelectMode(false);

		setAutoRefresh(true);

		onChangePrintTemplate();
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType) {
		initBeanForExternalDisplay(task, types, defaultType, new AssociatedContact(task, new Person(new Contact())));
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType,
			AssociatedContact sendTo) {
		DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(types);
		initBeanForExternalDisplay(task, subSelect, DocumentTemplate.getDefaultTemplate(subSelect, defaultType),
				sendTo);
	}

	public void initBeanForExternalDisplay(Task task, DocumentTemplate[] types, DocumentTemplate defaultType,
			AssociatedContact sendTo) {

		initBean(task, types, defaultType);

		setContactList(new ArrayList<ContactSelector>());

		setRenderedContact(new ContactSelector(sendTo));

		setSelectMode(false);

		setFaxMode(false);

		setSingleAddressSelectMode(false);

		setAutoRefresh(true);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBeanForSelecting(Task task, DocumentTemplate[] types, DocumentTemplate defaultType,
			AssociatedContact[] addresses, boolean allowIndividualAddress) {

		initBean(task, types, defaultType);

		setContactList(new ArrayList<ContactSelector>());

		if (addresses != null && addresses.length > 0) {
			for (AssociatedContact associatedContact : addresses) {
				getContactList()
						.add(new ContactSelector(task, associatedContact.getPerson(), associatedContact.getRole()));
			}

			getContactList().get(0).setSelected(true);
			setRenderedContact(getContactList().get(0));
		}

		if (allowIndividualAddress)
			getContactList().add(new ContactSelector(task,
					new Person(resourceBundle.get("dialog.print.individualAddress"), new Contact()), ContactRole.NONE));

		setSelectMode(true);
		setAutoRefresh(true);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBean(Task task, DocumentTemplate[] templates, DocumentTemplate selectedTemplate) {
		// getting task datalist, if was altered a updated task will be returend
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.PRINT);

		if (templates != null) {
			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(templates)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			// sets the selected template
			if (selectedTemplate == null && !getTemplateList().isEmpty())
				setSelectedTemplate(getTemplateList().get(0));
			else
				setSelectedTemplate(selectedTemplate);
		}
	}

	/**
	 * Updates the pdf content if a associatedContact was chosen for the first time
	 */
	public void onChooseContact(ContactSelector container) {

		// contact is selected
		if (container.isSelected()) {

			// setting as rendered if nothing is rendered
			if (getRenderedContact() == null) {
				// generating custom name if organization was selected

				container.generateAddress(true);
				setRenderedContact(container);
				onChangePrintTemplate();
				RequestContext.getCurrentInstance().update("dialogContent");
				return;
			}

			// rerendering if organization has chagned
			if (container.isOrganizationHasChagned()) {
				container.setOrganizationHasChagned(false);

				container.generateAddress(true);

				// updating beacause container is selected and rendered
				if (getRenderedContact() == container) {
					onChangePrintTemplate();
					RequestContext.getCurrentInstance().update("dialogContent");
				}
				return;
			}

			// if only one address should be selectable
			if (isSingleAddressSelectMode()) {

				// deselecting all other containers
				for (ContactSelector contactContainer : contactList) {
					if (contactContainer != container && contactContainer.isSelected()) {
						contactContainer.setSelected(false);
					}
				}

				// rendering if not already rendered
				if (getRenderedContact() != container) {
					container.generateAddress(true);
					setRenderedContact(container);
					onChangePrintTemplate();
					RequestContext.getCurrentInstance().update("dialogContent");
				}

				return;
			}

		} else {

			// only refresh if the rendered contact was deselected
			if (getRenderedContact() == container) {
				// deslecting contact and setting the first selected one
				for (ContactSelector contactContainer : contactList) {
					if (contactContainer.isSelected()) {
						setRenderedContact(contactContainer);
						onChangePrintTemplate();
						RequestContext.getCurrentInstance().update("dialogContent");
						return;
					}
				}
				setRenderedContact(null);
				onChangePrintTemplate();
				RequestContext.getCurrentInstance().update("dialogContent");
				return;
			}
		}
	}

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
				chooser.getParent().setOrganizationHasChagned(true);
			} else {
				// setting parent as selected
				chooser.getParent().setSelected(true);
			}
		} else {
			chooser.getParent().setOrganizationHasChagned(true);
		}

		onChooseContact(chooser.getParent());
	}

	public void onChangeAddressManually(ContactSelector container) {
		if (dialogHandlerAction.getCustomAddressDialog().isAddressChanged()) {
			if (getRenderedContact() == container) {
				onChangePrintTemplate();
				RequestContext.getCurrentInstance().update("dialogContent");
			}
		}
	}

	public void onChangePrintTemplate() {
		onChangePrintTemplate(false);
	}

	public void onChangePrintTemplate(boolean force) {

		getSelectedTemplate().initData(getTask());

		if (autoRefresh || force) {
			setPdfContainer(generatePDFFromTemplate());
			setRenderPdf(getPdfContainer() == null ? false : true);
		}
	}

	private PDFContainer generatePDFFromTemplate() {
		PDFContainer result;
		PDFGenerator generator = new PDFGenerator();
		switch (getSelectedTemplate().getDocumentType()) {
		case U_REPORT:
		case U_REPORT_EMTY:
			((TemplateUReport) getSelectedTemplate()).initData(getTask().getPatient(), getTask());
			result = generator.getPDF(getSelectedTemplate());
			break;
		case DIAGNOSIS_REPORT:
			((DiagnosisReport) getSelectedTemplate()).initData(getTask().getPatient(), getTask(),
					getRenderedContact() != null ? getRenderedContact().getCustomAddress() : null);
			result = generator.getPDF(getSelectedTemplate());
			break;
		case COUNCIL_REQUEST:
			((TemplateCouncil) getSelectedTemplate()).initData(getTask().getPatient(), getTask(), getSelectedCouncil(),
					getRenderedContact() != null ? getRenderedContact().getCustomAddress() : null);
			result = generator.getPDF(getSelectedTemplate());
			break;
		default:
			// always render the pdf with the fist associatedContact chosen
			result = null;
			break;
		}

		if (result == null) {
			result = new PDFContainer(DocumentType.EMPTY, "", new byte[0]);
			logger.debug("No Pdf created, hiding pdf display");
		}
		return result;
	}

	public void resetPDF() {
		setPdfContainer(null);
		setRenderPdf(false);
	}

	/**
	 * Gets the selected contacts an returns an list including them
	 * 
	 * @return
	 */
	private List<ContactSelector> getSelectedContactFromList() {
		ArrayList<ContactSelector> result = new ArrayList<ContactSelector>();
		for (ContactSelector contactChooser : getContactList()) {
			if (contactChooser.isSelected())
				result.add(contactChooser);
		}

		logger.debug("Return " + result.size() + " selected contatcs");
		return result;
	}

	/**
	 * Return the pdf as streamed content
	 * 
	 * @return
	 */
	public StreamedContent getPdfContent() {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream(getPdfContainer().getData()), "application/pdf",
					getPdfContainer().getName());
		}
	}

	public void onDownloadPdf() {
		if (getPdfContainer().getId() == 0) {
			logger.debug("Pdf not saved jet, saving");
			if (!getSelectedTemplate().isTransientContent())
				savePdf(getTask(), getPdfContainer());
		}
	}

	public void onPrintNewPdf() {

		boolean oneContactSelected = false;
		// addresses where chosen
		for (ContactSelector contactChooser : getContactList()) {
			if (contactChooser.isSelected()) {
				// address of the rendered pdf, not rendering twice
				// setting other associatedContact then selected
				ContactSelector tmp = getRenderedContact();
				setRenderedContact(contactChooser);
				// render all other pdfs
				PDFContainer otherAddress = generatePDFFromTemplate();
				for (int i = 0; i < contactChooser.getCopies(); i++) {
					userHandlerAction.getSelectedPrinter().print(otherAddress, getSelectedTemplate().getAttributes());
				}
				// settings the old selected associatedContact as
				// selected associatedContact
				setRenderedContact(tmp);

				// individual contact, adding to contact list
				if (contactChooser.getContact().getRole() != ContactRole.NONE) {

					contactDAO.addNotificationType(task, contactChooser.getContact(),
							AssociatedContactNotification.NotificationTyp.PRINT, false, true, false,
							new Date(System.currentTimeMillis()), contactChooser.getCustomAddress());
				} else {
					// TODO add indivuell address as person
				}

				oneContactSelected = true;
			}

		}

		// no address was chosen, so the address will be "An den
		// weiterbehandelden Kollegen" this was generated and saved in
		// tmpPdfContainer
		if (!oneContactSelected) {
			if (!getSelectedTemplate().isTransientContent())
				savePdf(getTask(), getPdfContainer());
			userHandlerAction.getSelectedPrinter().print(getPdfContainer(), getSelectedTemplate().getAttributes());
		}

	}

	/**
	 * Saves a new pdf within the task
	 * 
	 * @param pdf
	 */
	public void savePdf(Task task, PDFContainer pdf) {

		try {
			if (pdf.getId() == 0) {
				logger.debug("Pdf not saved jet, saving" + pdf.getName());

				// saving new pdf and updating task
				genericDAO.savePatientData(pdf, task, "log.patient.task.pdf.created", pdf.getName());

				task.getAttachedPdfs().add(pdf);

				genericDAO.savePatientData(task, "log.patient.pdf.attached", pdf.getName());
			} else {
				logger.debug("PDF allready saved, not saving. " + pdf.getName());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void setDefaultTemplateOfType(DocumentType type) {
		if (getTemplateList() != null) {
			try {
				DocumentTemplate defaultTemplate = getTemplateList().stream()
						.filter(p -> p.getDocumentType().equals(type) && p.isDefaultOfType())
						.collect(StreamUtils.singletonCollector());

				setSelectedTemplate(defaultTemplate);

				onChangePrintTemplate();
			} catch (IllegalStateException e) {
				// do nothing
			}
		}
	}
}
