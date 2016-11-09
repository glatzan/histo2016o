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
	 * Returns an array containing all values of the contactRole enumeration.
	 * 
	 * @return
	 */
	public ContactRole[] getContactRoles() {
		return ContactRole.values();
	}

	/**
	 * Returns an array containing all values of the TaskPriority enumeration
	 * 
	 * @return
	 */
	public TaskPriority[] getTaskPriority() {
		return TaskPriority.values();
	}

	/**
	 * Returns an array containing all values of the SigantureRole enumeration.
	 * 
	 * @return
	 */
	public SignatureRole[] getSignatureRoles() {
		return SignatureRole.values();
	}

	/**
	 * Returns an array containing all values of the {@link NotificationOption}
	 * enumeration.
	 * 
	 * @return
	 */
	public NotificationOption[] getNotificationEmailOptions() {
		return new NotificationOption[] { NotificationOption.NONE, NotificationOption.TEXT, NotificationOption.PDF };
	}

	/**
	 * Returns an array containing all values of the {@link NotificationOption}
	 * enumeration.
	 * 
	 * @return
	 */
	public NotificationOption[] getNotificationFaxOptions() {
		return new NotificationOption[] { NotificationOption.NONE, NotificationOption.FAX };
	}

	/**
	 * Returns an array containing all values of the {@link NotificationOption}
	 * enumeration.
	 * 
	 * @return
	 */
	public NotificationOption[] getNotificationPhoneOptions() {
		return new NotificationOption[] { NotificationOption.NONE, NotificationOption.PHONE };
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
	 * Returns a dateFormat
	 * 
	 * @param dialog
	 * @return
	 */
	public DateFormat getDateFormat(DateFormat dateFormat) {
		return dateFormat;
	}

}
