package org.histo.action.dialog.settings.physician;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.LdapHandler;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PhysicianSearchDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	/**
	 * If true it is possible to create an external physician or patient
	 */
	private boolean externalMode;

	/**
	 * Tabindex of the settings tab
	 */
	private SearchView searchView;

	/**
	 * List containing all physicians known in the histo database
	 */
	private List<Physician> physicianList;

	/**
	 * Used for creating new or for editing existing physicians
	 */
	private Physician selectedPhysician;

	/**
	 * String is used for searching for internal physicians
	 */
	private String searchString;

	/**
	 * Array of roles which the physician should be associated
	 */
	private ContactRole[] associatedRoles;

	private List<ContactRole> allRoles;

	public void initAndPrepareBean() {
		initAndPrepareBean(false);
	}

	public void initAndPrepareBean(boolean externalMode) {
		if (initBean(externalMode))
			prepareDialog();
	}

	public boolean initBean(boolean externalMode) {
		setSelectedPhysician(null);
		setSearchString("");
		setPhysicianList(null);
		setExternalMode(externalMode);
		setSearchView(SearchView.INTERNAL);
		setAssociatedRoles(new ContactRole[] { ContactRole.NONE });
		setAllRoles(Arrays.asList(ContactRole.values()));

		super.initBean(task, Dialog.SETTINGS_PHYSICIAN_SEARCH);
		return true;
	}

	/**
	 * Generates an ldap search filter (?(xxx)....) and offers the result list. The
	 * result list is a physician list with minimal details. Before adding an clinic
	 * physician a ldap fetch for more details has to be done
	 *
	 * @param name
	 */
	public void searchForPhysician(String name) {
		if (name != null && name.length() > 3) {
			// removing multiple spaces an commas and replacing them with one
			// space,
			// splitting the whole thing into an array
			String[] arr = name.replaceAll("[ ,]+", " ").split(" ");
			StringBuffer request = new StringBuffer("(&");
			for (int i = 0; i < arr.length; i++) {
				request.append("(cn=*" + arr[i] + "*)");
			}
			request.append(")");

			try {
				logger.debug("Search for " + request.toString());

				LdapHandler ldapHandler = globalSettings.getLdapHandler();
				DirContext connection = ldapHandler.openConnection();

				setPhysicianList(ldapHandler.getListOfPhysicians(connection, request.toString()));

				ldapHandler.closeConnection(connection);

				setSelectedPhysician(null);

			} catch (NamingException | IOException e) {
				setPhysicianList(new ArrayList<Physician>());
				// TODO to many results
			}
		}
	}

	public void changeMode() {
		setAssociatedRoles(new ContactRole[] { ContactRole.NONE });

		if (searchView == SearchView.EXTERNAL) {
			setSelectedPhysician(new Physician(new Person(new Contact())));
			// person is not auto update able
			getSelectedPhysician().getPerson().setAutoUpdate(false);
			getSelectedPhysician().getPerson().setOrganizsations(new ArrayList<Organization>());
		} else
			setSelectedPhysician(null);
	}

	/**
	 * Sets the role Array and returns the physician
	 * 
	 * @return
	 */
	public Physician getPhysician() {
		if (getSelectedPhysician() == null)
			return null;

		getSelectedPhysician().setAssociatedRolesAsArray(getAssociatedRoles());
		return getSelectedPhysician();
	}

	/**
	 * Adds an organization to the user
	 * 
	 * @param event
	 */
	public void onReturnOrganizationDialog(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof Organization
				&& !getSelectedPhysician().getPerson().getOrganizsations().contains((Organization) event.getObject())) {
			getSelectedPhysician().getPerson().getOrganizsations().add((Organization) event.getObject());
		}
	}

	public enum SearchView {
		EXTERNAL, INTERNAL;
	}
}
