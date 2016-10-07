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

	public Role[] getRoles() {
		return Role.values();
	}
	
	/**
	 * Returns the path an an dialog
	 * @param dialog
	 * @return
	 */
	public Dialog getDialog(Dialog dialog){
		return dialog;
	}
}
