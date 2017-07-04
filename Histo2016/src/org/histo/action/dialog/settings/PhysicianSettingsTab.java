package org.histo.action.dialog.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.naming.NamingException;

import org.histo.action.dialog.SettingsDialogHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.SettingsTab;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.settings.LdapHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configurable
public class PhysicianSettingsTab extends AbstractSettingsTab {

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private SettingsHandler settingsHandler;

	@Autowired
	private SettingsDialogHandler settingsDialogHandler;

	/**
	 * Tabindex of the settings tab
	 */
	private Page page;
	// private SettingsTab physicianTabIndex = SettingsTab.P_LIST;

	/**
	 * True if archived physicians should be display
	 */
	private boolean showArchivedPhysicians = false;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showPhysicianRoles;

	/**
	 * List containing all physicians known in the histo database
	 */
	private List<Physician> physicianList;

	/**
	 * Used for creating new or for editing existing physicians
	 */
	private Physician tmpPhysician;

	/**
	 * List containing all physicians available from ldap
	 */
	private List<Physician> ldapPhysicianList;

	/**
	 * Used for selecting a physician from the ldap list
	 */
	private Physician tmpLdapPhysician;

	/**
	 * String is used for searching for internal physicians
	 */
	private String ldapPhysicianSearchString;

	public void prepare() {
		switch (getPage()) {
		case LIST:

			if (getShowPhysicianRoles() == null || getShowPhysicianRoles().length == 0)
				setShowPhysicianRoles(new ContactRole[] { ContactRole.PRIVATE_PHYSICIAN, ContactRole.SURGEON,
						ContactRole.OTHER_PHYSICIAN, ContactRole.SIGNATURE });

			setPhysicianList(physicianDAO.getPhysicians(getShowPhysicianRoles(), isShowArchivedPhysicians()));
			break;

		default:
			break;
		}
	}

	/**
	 * Shows the add external or ldap screen per default the ldap select screnn is
	 * used.
	 */
	public void prepareNewPhysician() {
		setTmpPhysician(new Physician());
		getTmpPhysician().setPerson(new Person());
		setPage(Page.ADD_LDAP);

		setLdapPhysicianSearchString("");
		setLdapPhysicianList(new ArrayList<Physician>());
	}

	/**
	 * Shows the gui for editing an existing physician
	 * 
	 * @param physician
	 */
	public void prepareEditPhysician(Physician physician) {
		setTmpPhysician(physician);
		setPage(Page.EDIT);
	}

	/**
	 * Opens the passed physician in the settingsDialog in order to edit the phone
	 * number, email or faxnumber.
	 * 
	 * @param associatedContact
	 */
	public void prepareEditPhysicianFromExtern(Person person) {
		Physician result = physicianDAO.getPhysicianByPerson(person);
		if (result != null) {
			setTmpPhysician(result);
			setPhysicianTabIndex(SettingsTab.P_EDIT_EXTERN);
			setActiveSettingsIndex(SettingsTab.PHYSICIAN.getTabNumber());
			prepareSettingsDialog();
		}
	}

	/**
	 * Generates an ldap search filter (?(xxx)....) and offers the result list. The
	 * result list is a physician list with minimal details. Before adding an clinic
	 * physician a ldap fetch for more details has to be done
	 * 
	 * @param name
	 */
	public void searchForPhysician(String name) {
		// removing multiple spaces an commas and replacing them with one space,
		// splitting the whole thing into an array
		String[] arr = name.replaceAll("[ ,]+", " ").split(" ");
		StringBuffer request = new StringBuffer("(&");
		for (int i = 0; i < arr.length; i++) {
			request.append("(cn=*" + arr[i] + "*)");
		}
		request.append(")");

		try {
			logger.debug("Search for " + request.toString());

			LdapHandler connection = settingsHandler.getLdapHandler();

			// searching for physicians
			connection.openConnection();
			setLdapPhysicianList(connection.getListOfPhysicians(request.toString()));
			connection.closeConnection();

			setTmpLdapPhysician(null);

		} catch (NamingException | IOException e) {
			setLdapPhysicianList(null);
			// TODO to many results
		}
	}

	/**
	 * Saves an edited physician to the database
	 * 
	 * @param physician
	 */
	public void saveEditPhysician(Physician physician) {
		try {
			if (physician.hasNoAssociateRole())
				physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);

			physicianDAO.save(physician, "log.settings.physician.physician.edit",
					new Object[] { physician.getPerson().getFullName() });

			discardTmpPhysician();
		} catch (CustomDatabaseInconsistentVersionException e) {
			settingsDialogHandler.onDatabaseVersionConflict();
		}
	}

	/**
	 * Saves a physician to the database, if no role was selected ContactRole.Other
	 * will be set per default.
	 * 
	 * @param physician
	 */
	public void saveNewPrivatePhysician(Physician physician) {
		try {
			// always set role to miscellaneous if no other role was selected
			if (physician.hasNoAssociateRole())
				physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);

			genericDAO.saveDataRollbackSave(physician, resourceBundle
					.get("log.settings.physician.privatePhysician.save", physician.getPerson().getFullName()));

			discardTmpPhysician();
		} catch (CustomDatabaseInconsistentVersionException e) {
			settingsDialogHandler.onDatabaseVersionConflict();
		}
	}

	/**
	 * 
	 * @param ldapPhysician
	 * @param editPhysician
	 */
	public void savePhysicianFromLdap(Physician ldapPhysician, HashSet<ContactRole> roles) {
		try {
			if (ldapPhysician == null) {
				discardTmpPhysician();
				return;
			}

			// removing id from the list
			ldapPhysician.setId(0);

			if (roles == null || roles.size() == 0)
				ldapPhysician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);
			else
				ldapPhysician.setAssociatedRoles(roles);

			// tje internal physician from ldap it might have been added before
			// (if
			// the the physician is a user of this programm),
			// search fur unique uid
			Physician physicianFromDatabase = physicianDAO.loadPhysicianByUID(ldapPhysician.getUid());

			// undating the foud physician
			if (physicianFromDatabase != null) {
				physicianFromDatabase.copyIntoObject(ldapPhysician);

				physicianFromDatabase.setArchived(false);

				// overwriting roles
				physicianFromDatabase.setAssociatedRoles(roles);

				genericDAO.saveDataRollbackSave(physicianFromDatabase, resourceBundle
						.get("log.settings.physician.ldap.update", ldapPhysician.getPerson().getFullName()));

				setTmpPhysician(physicianFromDatabase);
				discardTmpPhysician();
				return;
			}

			genericDAO.saveDataRollbackSave(ldapPhysician,
					resourceBundle.get("log.settings.physician.ldap.save", ldapPhysician.getPerson().getFullName()));

			discardTmpPhysician();
		} catch (CustomDatabaseInconsistentVersionException e) {
			settingsDialogHandler.onDatabaseVersionConflict();
		}
	}

	/**
	 * Archvies or dearchvies physicians depending on the given parameters.
	 * 
	 * @param physician
	 * @param archive
	 */
	public void archivePhysician(Physician physician, boolean archive) {
		try {
			physician.setArchived(archive);
			genericDAO.saveDataRollbackSave(physician,
					resourceBundle.get(
							archive ? "log.settings.physician.archived" : "log.settings.physician.archived.undo",
							physician.getPerson().getFullName()));
			preparePhysicianList();
		} catch (CustomDatabaseInconsistentVersionException e) {
			settingsDialogHandler.onDatabaseVersionConflict();
		}
	}

	/**
	 * Clears the temporary variables and the the physician list to display
	 */
	public void discardTmpPhysician() {
		// if a physician is in database and changes should be discarded, so
		// refresh from database
		if ((getPhysicianTabIndex() == SettingsTab.P_EDIT || getPhysicianTabIndex() == SettingsTab.P_EDIT_EXTERN)
				&& getTmpPhysician().getId() != 0)
			genericDAO.reset(getTmpPhysician());

		setTmpPhysician(null);
		setTmpLdapPhysician(null);

		if (getPhysicianTabIndex() != SettingsTab.P_EDIT_EXTERN) {
			// update physician list
			preparePhysicianList();
		} else {
			// if the edit was called externally close the dialog
			hideDialog();
		}

		setPhysicianTabIndex(SettingsTab.P_LIST);
	}

	public enum Page {
		LIST, EDIT, EDIT_EXTERN, ADD_EXTERN, ADD_LDAP;
	}
}