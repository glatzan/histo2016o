package org.histo.config.enums;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * <p:importEnum type="org.histo.config.enums.Display" var="display" />
 * 
 * @author glatza
 *
 */
@Component
@Scope(value = "session")
public class EnumProvider {

	/**
	 * Used for select view via p:selectOneMenu, p:importEnum not working in
	 * this context
	 * 
	 * @return
	 */
	public Display[] getDisplays() {
		return Display.values();
	}

	/**
	 * Returns an array containing all available roles.
	 * 
	 * @return
	 */
	public Role[] getRoles() {
		return Role.values();
	}

	/**
	 * Returns an array containing all available month.
	 * 
	 * @return
	 */
	public Month[] getMonth() {
		return Month.values();
	}

	/**
	 * Returns an array containing all values of the eye enumeration
	 * 
	 * @return
	 */
	public Eye[] getEyes() {
		return Eye.values();
	}

	/**
	 * Retuerns an array containin all values of the contactRole enummeration.
	 * 
	 * @return
	 */
	public ContactRole[] getContactRoles() {
		return ContactRole.values();
	}

	/**
	 * Returns the path an an dialog
	 * 
	 * @param dialog
	 * @return
	 */
	public Dialog getDialog(Dialog dialog) {
		return dialog;
	}

	/**
	 * Takes a string (from primefeaces) or a WorklistSearchOption and returns a
	 * WorklistSearchOption. Workaround for primefaces.
	 * 
	 * @param worklistSearchOption
	 * @return
	 */
	public WorklistSearchOption getSearchOption(WorklistSearchOption worklistSearchOption) {
		return worklistSearchOption;
	}

	/**
	 * Takes a string (from primefeaces) or a WorklistSearchFilter and returns a
	 * WorklistSearchFilter. Workaround for primefaces.
	 * 
	 * @param WorklistSearchFilter
	 * @return
	 */
	public WorklistSearchFilter getWorklistSearchFilter(WorklistSearchFilter worklistSearchFilter) {
		return worklistSearchFilter;
	}

	/**
	 * Takes a string (from primefeaces) or a WorklistSortOrder and returns a
	 * WorklistSortOrder. Workaround for primefaces.
	 * 
	 * @param worklistSearchFilter
	 * @return
	 */
	public WorklistSortOrder getWorklistSortOrder(WorklistSortOrder worklistSearchFilter) {
		return worklistSearchFilter;
	}

	/**
	 * Takes a string (from primefeaces) or a DiagnosisType and returns a
	 * DiagnosisType. Workaround for primefaces.
	 * 
	 * @param diagnosisType
	 * @return
	 */
	public DiagnosisType getDiagnosisType(DiagnosisType diagnosisType) {
		return diagnosisType;
	}

	/**
	 * Takes a string (from primefeaces) or a ContactRole and returns a
	 * ContactRole. Workaround for primefaces.
	 * 
	 * @param contactRole
	 * @return
	 */
	public ContactRole getContactRole(ContactRole contactRole) {
		return contactRole;
	}

	/**
	 * Takes a string (from primefeaces) or a SettingsTab and returns a
	 * SettingsTab. Workaround for primefaces.
	 * 
	 * @param settingsTab
	 * @return
	 */
	public SettingsTab getSettingsTab(SettingsTab settingsTab) {
		return settingsTab;
	}
}
