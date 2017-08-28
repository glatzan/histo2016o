package org.histo.config;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

@Component(value = "msg")
public class ResourceBundle extends HashMap<Object, Object> {

	private static final long serialVersionUID = 1668009329184453712L;

	@Autowired
	private MessageSource messageSource;

	@Override
	public String get(Object key) {
		return get(key, FacesContext.getCurrentInstance().getViewRoot().getLocale());
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