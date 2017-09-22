package org.histo.config.exception;

import org.histo.config.ResourceBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable(preConstruction = true)
@Getter
@Setter
public class CustomUserNotificationExcepetion extends RuntimeException {

	private static final long serialVersionUID = -57860678338686758L;

	@Autowired
	private ResourceBundle resourceBundle;

	private String headline;
	private String message;

	public CustomUserNotificationExcepetion(String headline, String message) {
		super();
		this.message = resourceBundle.get(message);
		this.headline = resourceBundle.get(headline);
	}
}
