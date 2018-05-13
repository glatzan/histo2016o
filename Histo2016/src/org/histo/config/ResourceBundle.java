package org.histo.config;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;

import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(value = "msg")
@Scope("singleton")
public class ResourceBundle extends HashMap<Object, Object> {

	private static final long serialVersionUID = 1668009329184453712L;

	// todo shift to settings
	public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

	@Autowired
	private MessageSource messageSource;

	@Override
	public String get(Object key) {
		// if view root is available use the current locale, if not use the default one
		return get(key,
				FacesContext.getCurrentInstance() != null ? FacesContext.getCurrentInstance().getViewRoot().getLocale()
						: Locale.GERMAN);
	}

	public String get(Object key, Locale locale) {
		try {
			return messageSource.getMessage((String) key, null, locale);
		} catch (NoSuchMessageException e) {
			return "???" + key + "???";
		}
	}

	public String get(String key, Object... params) {
		try {
			return MessageFormat.format(this.get(key), params);
		} catch (MissingResourceException e) {
			return "???" + key + "???";
		}
	}

	public String get(String key, Locale locale, Object... params) {
		try {
			return MessageFormat.format(this.get(key, locale), params);
		} catch (MissingResourceException e) {
			return "???" + key + "???";
		}
	}

}