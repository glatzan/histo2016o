package org.histo.action.dialog.print;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.ui.ContactContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.printer.template.AbstractTemplate;
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
	private PatientDao patientDao;

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
	private PDFGeneratorHandler pDFGeneratorHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsHandler settingsHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * List of all templates for printing
	 */
	private List<AbstractTemplate> templateList;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<AbstractTemplate> templateTransformer;

	/**
	 * Selected template for printing
	 */
	private AbstractTemplate selectedTemplate;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer pdfContainer;

	/**
	 * List with all associated contacts
	 */
	private List<ContactContainer> contactList;

	/**
	 * The associatedContact rendered, the first one will always be rendered, if
	 * not changed, no rendering necessary
	 */
	private ContactContainer renderedContact;

	/**
	 * True if the pdf should be rendered
	 */
	private boolean renderPdf;

	/**
	 * Council to print
	 */
	private Council selectedCouncil;

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
		AbstractTemplate[] subSelect = AbstractTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.U_REPORT,
						DocumentType.U_REPORT_EMTY, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		initBean(task, subSelect, AbstractTemplate.getDefaultTemplate(subSelect));

		// contacts for printing
		setContactList(new ArrayList<ContactContainer>());

		// setting patient
		getContactList().add(new ContactContainer(task, task.getPatient().getPerson(), ContactRole.PATIENT));

		// setting other contacts (physicians)
		getContactList().addAll(ContactContainer.factory(task.getContacts()));

		getContactList().add(new ContactContainer(task, new Person("Individuelle Addresse", new Contact()), ContactRole.NONE));
		
		setRenderedContact(null);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initAndPrepareBeanForCouncil(Task task, Council council) {
		initBeanForCouncil(task, council);
		prepareDialog();
	}

	public void initBeanForCouncil(Task task, Council council) {
		AbstractTemplate[] subSelect = AbstractTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.COUNCIL_REQUEST });

		initBean(task, subSelect, AbstractTemplate.getDefaultTemplate(subSelect));

		setSelectedCouncil(council);

		// contacts for printing
		setContactList(new ArrayList<ContactContainer>());

		// only one adress so set as chosen
		if (getSelectedCouncil().getCouncilPhysician() != null) {
			ContactContainer chosser = new ContactContainer(task,
					getSelectedCouncil().getCouncilPhysician().getPerson(), ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactList().add(chosser);

			// setting council physicians data as rendere associatedContact data
			setRenderedContact(chosser);
		}

		onChangePrintTemplate();
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType) {
		initBeanForExternalDisplay(task, types, defaultType, null);
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType,
			AssociatedContact sendTo) {
		AbstractTemplate[] subSelect = AbstractTemplate.getTemplatesByTypes(types);
		initBeanForExternalDisplay(task, subSelect, AbstractTemplate.getDefaultTemplate(subSelect, defaultType), sendTo);
	}

	public void initBeanForExternalDisplay(Task task, AbstractTemplate[] types, AbstractTemplate defaultType,
			AssociatedContact sendTo) {

		initBean(task, types, defaultType);

		setContactList(new ArrayList<ContactContainer>());

		setRenderedContact(new ContactContainer(sendTo));

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBean(Task task, AbstractTemplate[] templates, AbstractTemplate selectedTemplate) {
		// getting task datalist, if was altered a updated task will be returend
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.PRINT);

		if (templates != null) {
			setTemplateList(new ArrayList<AbstractTemplate>(Arrays.asList(templates)));

			setTemplateTransformer(new DefaultTransformer<AbstractTemplate>(getTemplateList()));

			// sets the selected template
			if (selectedTemplate == null && !getTemplateList().isEmpty())
				setSelectedTemplate(getTemplateList().get(0));
			else
				setSelectedTemplate(selectedTemplate);
		}
	}

	/**
	 * Updates the pdf content if a associatedContact was chosen for the first
	 * time
	 */
	public void onChooseContact(ContactContainer container) {

		// contact is selected
		if (container.isSelected()) {

			// setting as rendered if nothing is rendered
			if (getRenderedContact() == null) {
				// generating custom name if organization was selected
				ContactContainer.generateCustomOrganizationAddress(container);
				setRenderedContact(container);
				onChangePrintTemplate();
				RequestContext.getCurrentInstance().update("dialogContent");
				return;
			}

			// rerendering if organization has chagned
			if (container.isOrganizationHasChagned()) {
				container.setOrganizationHasChagned(false);
				if (!ContactContainer.generateCustomOrganizationAddress(container)) {
					// no organization address was generated, so the
					// organization was deselected
					container.getContact().setCustomContact(null);
				}

				// updating beacause container is selected and rendered
				if (getRenderedContact() == container) {
					onChangePrintTemplate();
					RequestContext.getCurrentInstance().update("dialogContent");
				}
				return;
			}

		} else {
			// deslecting contact and setting the first selected one
			for (ContactContainer contactContainer : contactList) {
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

	public void onChooseOrganizationOfContact(ContactContainer.OrganizationChooser chooser) {
		System.out.println("suu");
		if (chooser.isSelected()) {
			// only one organization can be selected, removing other
			// organizations
			// from selection
			if (chooser.getParent().isSelected()) {
				for (ContactContainer.OrganizationChooser organizationChooser : chooser.getParent()
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
			System.out.println("unselecting");
			chooser.getParent().setOrganizationHasChagned(true);
		}

		onChooseContact(chooser.getParent());
	}

	public void onChangePrintTemplate() {
		setPdfContainer(generatePDFFromTemplate());
		setRenderPdf(getPdfContainer() == null ? false : true);
	}

	private PDFContainer generatePDFFromTemplate() {
		PDFContainer result;
		switch (getSelectedTemplate().getDocumentType()) {
		case U_REPORT:
		case U_REPORT_EMTY:
			result = pDFGeneratorHandler.generateUReport(getSelectedTemplate(), getTask().getPatient(), getTask());
			break;
		case DIAGNOSIS_REPORT:
			result = pDFGeneratorHandler.generateDiagnosisReport(getSelectedTemplate(), getTask().getPatient(),
					getTask(), getRenderedContact() != null ? getRenderedContact().getContact() : null);
			break;
		case COUNCIL_REQUEST:
			result = pDFGeneratorHandler.generateCouncilRequest(getSelectedTemplate(), getTask().getPatient(),
					getSelectedCouncil());
			break;
		default:
			// always render the pdf with the fist associatedContact chosen
			result = pDFGeneratorHandler.generatePDFForReport(getTask().getPatient(), getTask(), getSelectedTemplate(),
					getRenderedContact() != null ? getRenderedContact().getContact() : null);
			break;
		}

		if (result == null) {
			result = new PDFContainer(DocumentType.EMPTY, "", new byte[0]);
			logger.debug("No Pdf created, hiding pdf display");
		}
		return result;
	}

	/**
	 * Gets the selected contacts an returns an list including them
	 * 
	 * @return
	 */
	private List<ContactContainer> getSelectedContactFromList() {
		ArrayList<ContactContainer> result = new ArrayList<ContactContainer>();
		for (ContactContainer contactChooser : getContactList()) {
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
			if (!getSelectedTemplate().isDoNotSave())
				savePdf(getTask(), getPdfContainer());
		}
	}

	public void onPrintNewPdf() {
		// no address was chosen, so the address will be "An den
		// weiterbehandelden Kollegen" this was generated and saved in
		// tmpPdfContainer
		if (getContactList().isEmpty()) {
			if (!getSelectedTemplate().isDoNotSave())
				savePdf(getTask(), getPdfContainer());
			settingsHandler.getSelectedPrinter().print(getPdfContainer());
		} else {
			boolean oneContactSelected = false;
			// addresses where chosen
			for (ContactContainer contactChooser : getContactList()) {
				if (contactChooser.isSelected()) {
					// address of the rendered pdf, not rendering twice
					if (contactChooser == getRenderedContact()) {
						if (!getSelectedTemplate().isDoNotSave())
							savePdf(getTask(), getPdfContainer());
						for (int i = 0; i < contactChooser.getCopies(); i++) {
							settingsHandler.getSelectedPrinter().print(getPdfContainer());
						}
					} else {
						// setting other associatedContact then selected
						ContactContainer tmp = getRenderedContact();
						setRenderedContact(contactChooser);
						// render all other pdfs
						PDFContainer otherAddress = generatePDFFromTemplate();
						for (int i = 0; i < contactChooser.getCopies(); i++) {
							settingsHandler.getSelectedPrinter().print(otherAddress);
						}
						// settings the old selected associatedContact as
						// selected associatedContact
						setRenderedContact(tmp);
					}

					oneContactSelected = true;
				}

			}

			// printin if no container was selected, with the default address
			if (!oneContactSelected) {
				settingsHandler.getSelectedPrinter().print(getPdfContainer());
			}
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
				patientDao.savePatientAssociatedDataFailSave(pdf, task, "log.patient.task.pdf.created", pdf.getName());

				task.getAttachedPdfs().add(pdf);

				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.pdf.attached", pdf.getName());
			} else {
				logger.debug("PDF allready saved, not saving. " + pdf.getName());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
