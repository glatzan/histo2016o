package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.histo.model.user.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class UserSettingsDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;
	
	private HistoUser user;

	private List<View> availableViews;

	private List<WorklistSearchOption> availableWorklistsToLoad;

	public void initAndPrepareBean() {
		if (initBean())
			prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.USER_SETTINGS);
		setUser(userHandlerAction.getCurrentUser());

		PredefinedRoleSettings roleSetting = globalSettings
				.getRoleSettingsForRole(userHandlerAction.getCurrentUser().getRole());

		setAvailableViews(roleSetting.getSelectableViews());

		setAvailableWorklistsToLoad(new ArrayList<WorklistSearchOption>());
		getAvailableWorklistsToLoad().add(WorklistSearchOption.DIAGNOSIS_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.STAINING_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.NOTIFICATION_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.EMTY);

		return true;
	}

	public void saveUserSettings() {
		logger.debug("Saving user Settings");

		try {
			genericDAO.save(getUser());
			userHandlerAction.updateSelectedPrinters();
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void resetUserSettings() {
		logger.debug("Resetting user Settings");
		genericDAO.refresh(getUser());
	}
}
