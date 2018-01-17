package org.histo.action.dialog.settings.physician;

import java.util.Arrays;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Organization;
import org.histo.model.Physician;
import org.histo.service.PhysicianService;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PhysicianEditDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianService physicianService;

	private Physician physician;

	private List<ContactRole> allRoles;

	public void initAndPrepareBean(Physician physician) {
		if (initBean(physician))
			prepareDialog();
	}

	public boolean initBean(Physician physician) {
		setPhysician(physician);
		setAllRoles(Arrays.asList(ContactRole.values()));

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

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Updates the data of the physician with data from the clinic backend
	 */
	public void updateDataFromLdap() {
		try {
			physicianService.updatePhysicianDataFromLdap(getPhysician());
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Adds an organization to the user
	 * 
	 * @param event
	 */
	public void onReturnOrganizationDialog(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof Organization
				&& !getPhysician().getPerson().getOrganizsations().contains((Organization) event.getObject())) {
			getPhysician().getPerson().getOrganizsations().add((Organization) event.getObject());
		}
	}
}
