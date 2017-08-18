package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.HistoUser;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserSettingsDialog extends AbstractDialog {

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private SettingsHandler settingsHandler;

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

		PredefinedRoleSettings roleSetting = settingsHandler
				.getRoleSettingsForRole(userHandlerAction.getCurrentUser().getRole());

		setAvailableViews(roleSetting.getSelectableViews());
		
		setAvailableWorklistsToLoad(new ArrayList<WorklistSearchOption>());
		getAvailableWorklistsToLoad().add(WorklistSearchOption.DIAGNOSIS_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.STAINING_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.NOTIFICATION_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.EMTY);
		
		return true;
	}

	public void saveUserSettings(){
		logger.debug("Saving user Settings");
		
		try {
			genericDAO.save(getUser());
			settingsHandler.updateSelectedPrinters();
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
	
	public void resetUserSettings(){
		logger.debug("Resetting user Settings");
		genericDAO.reset(getUser());
	}
	
	// ************************ Getter/Setter ************************
	public HistoUser getUser() {
		return user;
	}

	public void setUser(HistoUser user) {
		this.user = user;
	}

	public List<View> getAvailableViews() {
		return availableViews;
	}

	public void setAvailableViews(List<View> availableViews) {
		this.availableViews = availableViews;
	}

	public List<WorklistSearchOption> getAvailableWorklistsToLoad() {
		return availableWorklistsToLoad;
	}

	public void setAvailableWorklistsToLoad(List<WorklistSearchOption> availableWorklistsToLoad) {
		this.availableWorklistsToLoad = availableWorklistsToLoad;
	}

}
