package org.histo.action.dialog.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.Dialog;
import org.histo.model.Physician;
import org.histo.settings.LdapHandler;
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

	public void initAndPrepareBean() {
		if (initBean())
			prepareDialog();
	}

	public boolean initBean() {

		super.initBean(task, Dialog.PHYSICIAN_SEARCH);
		return true;
	}

	/**
	 * Generates an ldap search filter (?(xxx)....) and offers the result list.
	 * The result list is a physician list with minimal details. Before adding
	 * an clinic physician a ldap fetch for more details has to be done
	 *
	 * @param name
	 */
	public void searchForPhysician(String name) {
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
