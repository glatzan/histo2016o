package org.histo.util;

import java.util.ArrayList;

import org.histo.config.enums.View;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.histo.worklist.search.WorklistSimpleSearch.SimpleSearchOption;

public class CopySettingsUtil {

	/**
	 * Copies crucial settings from group settings to user settings
	 * 
	 * @param user
	 * @param group
	 */
	public static void copyCrucialGroupSettings(HistoUser user, HistoGroup group, boolean overwrite) {

		user.setArchived(group.isUserDeactivated());

		user.getSettings().setAvailableViews(new ArrayList<View>(group.getSettings().getAvailableViews()));
		user.getSettings().setDefaultView(group.getSettings().getDefaultView());
		user.getSettings().setStartView(group.getSettings().getStartView());

		if (user.getSettings().getInputFieldColor() == null || overwrite)
			user.getSettings().setInputFieldColor(group.getSettings().getInputFieldColor());

		if (user.getSettings().getInputFieldFontColor() == null || overwrite)
			user.getSettings().setInputFieldFontColor(group.getSettings().getInputFieldFontColor());

		user.getSettings()
				.setAvailableWorklists(new ArrayList<SimpleSearchOption>(group.getSettings().getAvailableWorklists()));

		user.getSettings().setWorklistToLoad(group.getSettings().getWorklistToLoad());

		user.getSettings().setWorklistSortOrder(group.getSettings().getWorklistSortOrder());

		user.getSettings().setWorklistSortOrderAsc(group.getSettings().isWorklistSortOrderAsc());

		user.getSettings().setWorklistHideNoneActiveTasks(group.getSettings().isWorklistHideNoneActiveTasks());

		user.getSettings().setWorklistAutoUpdate(group.getSettings().isWorklistAutoUpdate());

		user.getSettings().setAlternatePatientAddMode(group.getSettings().isAlternatePatientAddMode());

		user.getSettings().setAddTaskWithSingelClick(group.getSettings().isAddTaskWithSingelClick());

	}

	/**
	 * Copies only view settings to the user settings
	 * 
	 * @param user
	 * @param group
	 * @param overwrite
	 */
	public static void copyUpdatedGroupSettings(HistoUser user, HistoGroup group) {

		user.getSettings().setAvailableViews(new ArrayList<View>(group.getSettings().getAvailableViews()));
		user.getSettings().setDefaultView(group.getSettings().getDefaultView());
		user.getSettings().setStartView(group.getSettings().getStartView());

		user.getSettings()
				.setAvailableWorklists(new ArrayList<SimpleSearchOption>(group.getSettings().getAvailableWorklists()));

		user.getSettings().setWorklistToLoad(group.getSettings().getWorklistToLoad());
	}

	public static void copyPhysicianData(Physician source, Physician destination, boolean force) {

		destination.setEmployeeNumber(source.getEmployeeNumber());
		destination.setUid(source.getUid());

		if (destination.getPerson().isAutoUpdate() || force)
			destination.setClinicRole(source.getClinicRole());

		copyPersonData(source.getPerson(), destination.getPerson(), force);
		// TODO is this necessary ?
		// setAssociatedRoles(dataToUpdate.getAssociatedRoles());
	}

	/**
	 * Copies data form one person to an other person but only if autoupdate is true
	 * 
	 * @param source
	 * @param destination
	 */
	public static void copyPersonData(Person source, Person destination, boolean force) {
		if (destination.isAutoUpdate()) {
			destination.setLastName(source.getLastName());
			destination.setFirstName(source.getFirstName());
			destination.setTitle(source.getTitle());
			destination.setBirthName(source.getBirthName());

			if (source.getBirthday() != null || force)
				destination.setBirthday(source.getBirthday());

			if (source.getNote() != null || force)
				destination.setNote(source.getNote());

			if (source.getLanguage() != null || force)
				destination.setLanguage(source.getLanguage());

			if (force)
				destination.setAutoUpdate(source.isAutoUpdate());

			copyContactData(source.getContact(), destination.getContact());

			// TODO real copy!!!!
			destination.setOrganizsations(source.getOrganizsations());
		}
	}

	/**
	 * Copies contact data form one contact to an other contact
	 */
	public static void copyContactData(Contact source, Contact destination) {
		destination.setBuilding(source.getBuilding());
		destination.setStreet(source.getStreet());
		destination.setTown(source.getTown());
		destination.setPostcode(source.getPostcode());
		destination.setCountry(source.getCountry());
		destination.setPhone(source.getPhone());
		destination.setMobile(source.getMobile());
		destination.setEmail(source.getEmail());
		destination.setHomepage(source.getHomepage());
		destination.setFax(source.getFax());
		destination.setPager(source.getPager());
	}

}
