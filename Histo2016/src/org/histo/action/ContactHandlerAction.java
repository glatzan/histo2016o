package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.histo.config.HistoSettings;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.ContactTab;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.SettingsTab;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Contact;
import org.histo.model.Physician;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PhysicianRoleOptions;
import org.histo.util.FileUtil;
import org.histo.util.NotificationHandler;
import org.histo.util.ResourceBundle;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class ContactHandlerAction implements Serializable {

	private static final long serialVersionUID = -3672859612072175725L;

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;
	
	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private SettingsHandlerAction settingsHandlerAction;

	/**
	 * List with all available contacts
	 */
	private List<Contact> allAvailableContact;

	/**
	 * containing options for the physician list
	 */
	private PhysicianRoleOptions physicianRoleOptions;

	/**
	 * Determines which tab is displayed.
	 */
	private ContactTab contactTab;

	/**
	 * Gets a list with all available contact for a specific task. Filters all
	 * duplicated entries.
	 * 
	 * @param task
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param addedContact
	 */
	public void prepareContacts(Task task, boolean surgeon, boolean extern, boolean other,
			boolean showAddedContactsOnly) {

		if (getPhysicianRoleOptions() == null)
			setPhysicianRoleOptions(new PhysicianRoleOptions());

		setContactTab(ContactTab.LIST);

		updateContactList(task, surgeon, extern, other, showAddedContactsOnly);

		mainHandlerAction.showDialog(Dialog.CONTACTS);
	}

	/**
	 * Refreshes the contact list.
	 * 
	 * @param task
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param showAddedContactsOnly
	 */
	public void updateContactList(Task task, boolean surgeon, boolean extern, boolean other,
			boolean showAddedContactsOnly) {
		// refreshing the selected task
		genericDAO.refresh(task);

		setAllAvailableContact(new ArrayList<Contact>());

		List<Contact> contacts = task.getContacts();

		// getting all contact options
		List<Physician> databaseContacts = physicianDAO.getPhysicians(
				new ContactRole[] { ContactRole.SURGEON, ContactRole.PRIVATE_PHYSICIAN, ContactRole.OTHER }, false);

		if (!showAddedContactsOnly) {
			// shows all contacts but marks the already selected contacts with
			// the selected flag.
			loop: for (Physician physician : databaseContacts) {
				for (Contact contact : contacts) {
					if (contact.getPhysician().getId() == physician.getId()) {
						contact.setSelected(true);
						getAllAvailableContact().add(contact);
						continue loop;
					}
				}

				getAllAvailableContact().add(new Contact(physician));

			}
		} else {
			// show only selected contacts, mark them as selected
			for (Contact contact : contacts) {
				contact.setSelected(true);
			}
			getAllAvailableContact().addAll(contacts);
		}

	}

	/**
	 * Opens the passed physician in the settingsDialog in order to edit the
	 * phone number, email or faxnumber.
	 * 
	 * @param contact
	 */
	public void editContactData(Contact contact) {
		settingsHandlerAction.prepareSettingsDialog();
		settingsHandlerAction.setTmpPhysician(contact.getPhysician());
		settingsHandlerAction.setPhysicianTabIndex(SettingsTab.PHYSICIAN_EDIT);
		settingsHandlerAction.setActiveSettingsIndex(SettingsHandlerAction.TAB_PERSON);
	}

	/**
	 * Sobald im Kontaktdialog ein neuer Kontakt ausgewählt wird, wird je nach
	 * Art eine Benachrichtigung vorausgewählt.
	 * 
	 * @param contact
	 */
	public void onContactChangeRole(Contact contact, Task task) {
		// role was set to none so deselect every marker
		if (contact.getRole() == ContactRole.NONE) {
			task.getContacts().remove(contact);
			contact.setUseEmail(false);
			contact.setUseFax(false);
			contact.setUsePhone(false);
			genericDAO.delete(contact, resourceBundle.get("log.patient.task.contact.remove", task.getTaskID(),
					contact.getPhysician().getName()), task.getPatient());
		} else {

			if (contact.isUseEmail() || contact.isUsePhone() || contact.isUseFax()){
				// something was already select, do nothing				
			}else if (contact.getRole() == ContactRole.SURGEON) {
				// surgeon use email per default
				contact.setUseEmail(true);
			}else if (contact.getRole() == ContactRole.PRIVATE_PHYSICIAN && contact.getPhysician().getFax() != null
					&& !contact.getPhysician().getFax().isEmpty()) {
				// private physician use fax per default
				contact.setUseFax(true);
			}else if(contact.getPhysician().getEmail() != null && !contact.getPhysician().getEmail().isEmpty()){
				// other contacts use email per default
				contact.setUseEmail(true);
			}

			// adds contact if not added jet
			if(!task.getContacts().contains(contact)){
				task.getContacts().add(contact);
			}

			genericDAO.save(contact, resourceBundle.get("log.patient.task.contact.add", task.getTaskID(),
					contact.getPhysician().getName()), task.getPatient());
			
			System.out.println("saving");
			
		}

		genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());
	}

	private int test = 1;
	
	public void updateTest(){
	}
	
	public void sendTest() {

		System.out.println(taskExecutor);
		NotificationHandler test = new NotificationHandler(this,genericDAO);
		test.setName("was geht");
		
		taskExecutor.execute(test);
		//FileUtil.loadTextFile(null);

		// System.out.println("ok");
		// SimpleEmail email = new SimpleEmail();
		// email.setHostName("smtp.ukl.uni-freiburg.de");
		// email.setDebug(true);
		// email.setSmtpPort(465);
		// email.setSSLOnConnect(true);
		// try {
		// email.addTo("andreas.glatz@uniklinik-freiburg.de");
		// email.setFrom("augenklinik.histologie@uniklinik-freiburg.de", "Name
		// des Senders");
		// email.setSubject("Testnachricht");
		// email.setMsg("Hallo, das ist nur ein simpler Test");
		// email.send();
		// } catch (EmailException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	@Async("taskExecutor")
	public void test1(){
		while(true){
			test++;
			System.out.println("test" +test);
			genericDAO.save(new Slide());
			

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public List<Contact> getAllAvailableContact() {
		return allAvailableContact;
	}

	public void setAllAvailableContact(List<Contact> allAvailableContact) {
		this.allAvailableContact = allAvailableContact;
	}

	public PhysicianRoleOptions getPhysicianRoleOptions() {
		return physicianRoleOptions;
	}

	public void setPhysicianRoleOptions(PhysicianRoleOptions physicianRoleOptions) {
		this.physicianRoleOptions = physicianRoleOptions;
	}

	public ContactTab getContactTab() {
		return contactTab;
	}

	public void setContactTab(ContactTab contactTab) {
		this.contactTab = contactTab;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public int getTest() {
		return test;
	}

	public void setTest(int test) {
		this.test = test;
	}
}
