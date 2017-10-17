package org.histo.action.dialog.settings;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.SettingsDialogHandler.HistoUserPage;
import org.histo.action.dialog.settings.PhysicianSearchDialog.SearchView;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Physician;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PhysicianEditDialog extends AbstractDialog {

	private Physician physician;

	public void initAndPrepareBean(Physician physician) {
		if (initBean(physician))
			prepareDialog();
	}

	public boolean initBean(Physician physician) {
		setPhysician(physician);

		super.initBean(task, Dialog.PHYSICIAN_EDIT);
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
}
