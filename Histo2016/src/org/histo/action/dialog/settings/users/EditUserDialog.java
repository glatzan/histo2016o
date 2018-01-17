package org.histo.action.dialog.settings.users;

import java.util.Arrays;
import java.util.List;

import javax.faces.event.AbortProcessingException;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.settings.physician.PhysicianSearchDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseConstraintViolationException;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.LogDAO;
import org.histo.dao.UserDAO;
import org.histo.model.Organization;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.histo.service.PhysicianService;
import org.histo.ui.transformer.DefaultTransformer;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class EditUserDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserDAO userDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private LogDAO logDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianService physicianService;

	private HistoUser user;

	private List<ContactRole> allRoles;

	private List<HistoGroup> groups;

	private DefaultTransformer<HistoGroup> groupTransformer;

	/**
	 * True if userdata where changed, an the dialog needs to be saved.
	 */
	private boolean saveAble;

	public void initAndPrepareBean(HistoUser user) {
		if (initBean(user))
			prepareDialog();
	}

	public boolean initBean(HistoUser user) {
		setUser(user);
		setAllRoles(Arrays.asList(ContactRole.values()));

		setSaveAble(false);

		setGroups(userDAO.getGroups(false));
		setGroupTransformer(new DefaultTransformer<HistoGroup>(getGroups()));

		super.initBean(task, Dialog.SETTINGS_USERS_EDIT);
		return true;
	}

	/**
	 * Saves user data
	 */
	public void saveUser() {
		try {
			if (user.getPhysician().hasNoAssociateRole())
				user.getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);

			genericDAO.save(user, "log.user.role.changed", new Object[] { user.toString() });
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Changes the group on the user, saves all data
	 */
	public void onChangeUserGroup() {
		try {
			userHandlerAction.groupOfUserHasChanged(getUser());
			setSaveAble(false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Updates the data of the physician with data from the clinic backend
	 */
	public void updateDataFromLdap() {
		try {
			physicianService.updatePhysicianDataFromLdap(user.getPhysician());
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Is called on return of the delete dialog. If true is passed into this method
	 * the edit user dialog will be closed.
	 * 
	 * @param event
	 */
	public void onDeleteDialogReturn(SelectEvent event) {
		if (event.getObject() instanceof Boolean && ((Boolean) event.getObject()).booleanValue()) {
			hideDialog();
		}
	}

	/**
	 * Opens a delete dialog for deleting the user.
	 */
	public void prepareDeleteUser() {
		prepareDialog(Dialog.SETTINGS_USERS_DELETE);
	}

	/**
	 * Tries to delete the user, if not deleteable it will show a dialog for
	 * disabling the user.
	 */
	public void deleteUser() {
		try {
			userDAO.deleteUser(user);
			hideDialog(true);
		} catch (CustomDatabaseConstraintViolationException e) {
			logger.debug("Delete not possible, change group dialog");
			prepareDialog(Dialog.SETTINGS_USERS_DELETE_DISABLE);
		}
	}

	/**
	 * Disables the user via group
	 */
	public void disableUser() {
		HistoGroup group = userDAO.getHistoGroup(HistoGroup.GROUP_DISABLED, true);
		user.setGroup(group);
		onChangeUserGroup();
	}

	/**
	 * Adds an organization to the user
	 * 
	 * @param event
	 */
	public void onReturnOrganizationDialog(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof Organization
				&& !getUser().getPhysician().getPerson().getOrganizsations().contains((Organization) event.getObject())) {
			getUser().getPhysician().getPerson().getOrganizsations().add((Organization) event.getObject());
		}
	}
}
