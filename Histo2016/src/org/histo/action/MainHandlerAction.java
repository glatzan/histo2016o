package org.histo.action;

import java.util.ArrayList;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.histo.model.UserRole;
import org.histo.model.patient.Patient;
import org.histo.util.SearchOptions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MainHandlerAction {

	@PostConstruct
	public void goToLogin() {
	    goToHisto16();
	}

	public String goToHisto16() {
		if (getWorkList() == null) {
			// log.debug("Standard Arbeitsliste geladen");
			Date currentDate = new Date(System.currentTimeMillis());
			// standard settings patients for today
			setWorkList(new ArrayList<Patient>());

			setSearchOptions(new SearchOptions());

			// getting default worklist depending on role
			int userLevel = userHandlerAction.getCurrentUser().getRole().getLevel();

			if (userLevel == UserRole.ROLE_LEVEL_MTA) {
				getSearchOptions().setSearchIndex(SearchOptions.SEARCH_INDEX_STAINING);
				updateWorklistOptions(getSearchOptions());
			} else if (userLevel == UserRole.ROLE_LEVEL_HISTO || userLevel == UserRole.ROLE_LEVEL_MODERATOR) {
				getSearchOptions().setSearchIndex(SearchOptions.SEARCH_INDEX_DIAGNOSIS);
				updateWorklistOptions(getSearchOptions());
			} else {
				// not adding anything to workilist -> superadmin or user
			}

			setRestrictedWorkList(getWorkList());
		}

		return "/pages/worklist/workList";
	}

	
}
