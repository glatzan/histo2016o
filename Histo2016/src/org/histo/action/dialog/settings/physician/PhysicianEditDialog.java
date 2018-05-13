package org.histo.action.dialog.settings.physician;

import java.util.Arrays;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.settings.organizations.OrganizationFunctions;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.service.PhysicianService;
import org.histo.ui.transformer.DefaultTransformer;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PhysicianEditDialog extends AbstractDialog implements OrganizationFunctions {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianService physicianService;

	private Physician physician;

	private List<ContactRole> allRoles;

	private DefaultTransformer<Organization> organizationTransformer;

	public void initAndPrepareBean(Physician physician) {
		if (initBean(physician))
			prepareDialog();
	}

	public boolean initBean(Physician physician) {
		setPhysician(physician);
		setAllRoles(Arrays.asList(ContactRole.values()));

		// setting organization transformer for selecting default address
		setOrganizationTransformer(
				new DefaultTransformer<Organization>(getPhysician().getPerson().getOrganizsations()));

		super.initBean(task, Dialog.SETTINGS_PHYSICIAN_EDIT);

		return true;
	}

	/**
	 * Saves an edited physician to the database
	 * 
	 * @param physician
	 */
	public void save() {
		try {
			if (getPhysician().hasNoAssociateRole())
				getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);

			genericDAO.save(getPhysician(), resourceBundle.get("log.settings.physician.physician.edit",
					getPhysician().getPerson().getFullName()));

		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Updates the data of the physician with data from the clinic backend
	 */
	public void updateDataFromLdap() {
		try {
			physicianService.ldapUpdate(getPhysician());
			updateOrganizationSelection();
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Getting person from physician
	 */
	public Person getPerson() {
		return getPhysician().getPerson();
	}
}
