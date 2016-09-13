package org.histo.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextHolderUtil {

	/**
	 * Setes a key value pair to the securityContext. Workaround for passing values to the RevisionListener.
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public static void setObjectToSecurityContext(String key, Object value) {
		SecurityContext sec = SecurityContextHolder.getContext();
		AbstractAuthenticationToken auth = (AbstractAuthenticationToken) sec.getAuthentication();

		HashMap<String, Object> info = null;

		if (auth.getDetails() == null || !(auth.getDetails() instanceof Map<?, ?>)) {
			info = new HashMap<String, Object>();
			auth.setDetails(info);
		} else
			info = ((HashMap<String, Object>) auth.getDetails());

		info.put(key, value);
	}

	/**
	 * Reads a value for the passed key from the securityContext.  Workaround for passing values to the RevisionListener.
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object getObjectFromSecurityContext(String key) {
		if (SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof Map<?, ?>) {
			Map<String, Object> info = (Map<String, Object>) SecurityContextHolder.getContext().getAuthentication()
					.getDetails();

			return info.get(key);
		}
		return null;
	}
}
