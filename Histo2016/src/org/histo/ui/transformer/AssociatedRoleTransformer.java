package org.histo.ui.transformer;

import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.histo.config.enums.ContactRole;

public class AssociatedRoleTransformer implements Converter {

	private List<ContactRole> roles;

	public AssociatedRoleTransformer(List<ContactRole> roles) {
		this.roles = roles;
	}
	
	public AssociatedRoleTransformer(ContactRole[] roles) {
		this.roles = Arrays.asList(roles);
	}

	public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				for (ContactRole role : roles) {
					if (value.equals(role.name()))
						return role;
				}
				return null;
			} catch (NumberFormatException e) {
			}
		} else {
			return null;
		}
		return null;
	}

	public String getAsString(FacesContext fc, UIComponent uic, Object object) {
		if (object != null && object instanceof ContactRole) {
			return String.valueOf(((ContactRole) object).name());
		} else {
			return "";
		}
	}
}