package org.histo.config;

import org.histo.config.enums.ContactRole;
import org.histo.config.enums.CouncilState;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Display;
import org.histo.config.enums.Eye;
import org.histo.config.enums.InformedConsentType;
import org.histo.config.enums.Month;
import org.histo.config.enums.SignatureRole;
import org.histo.config.enums.StainingListAction;
import org.histo.config.enums.TaskPriority;
import org.histo.model.ListItem;
import org.histo.model.StainingPrototype;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoPermissions;
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
	 * Returns an array containing all values of the StainingListAction
	 * enumeration.
	 * 
	 * @return
	 */
	public StainingListAction[] getStainingListActions() {
		return StainingListAction.values();
	}

	/**
	 * Returns an array containing all values of the StaticList enumeration.
	 * 
	 * @return
	 */
	public ListItem.StaticList[] getStaticLists() {
		return ListItem.StaticList.values();
	}

	/**
	 * Returns a dateformat, is used, because in mainHandlerAction the date
	 * method can take a string, an primefaces prefers the string method other
	 * the DateFormat method.
	 * 
	 * @param dateFormat
	 * @return
	 */
	public DateFormat getDateFormat(DateFormat dateFormat) {
		return dateFormat;
	}

	/**
	 * Returns an array with all values of the InformedConsentInterface
	 * 
	 * @return
	 */
	public InformedConsentType[] getInformedConsentTypes() {
		return InformedConsentType.values();
	}

	/**
	 * Returns an array with all values of the {@link CouncilState} enum
	 * 
	 * @return
	 */
	public CouncilState[] getCouncilStates() {
		return CouncilState.values();
	}

	/**
	 * Returns an array with all values of the StainingType enum
	 * @return
	 */
	public StainingPrototype.StainingType[] getStainingTypes(){
		return  StainingPrototype.StainingType.values();
	}
	
	public HistoGroup.AuthRole[] getAuthRoles(){
		return HistoGroup.AuthRole.values();
	}
}