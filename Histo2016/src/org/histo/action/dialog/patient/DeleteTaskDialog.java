package org.histo.action.dialog.patient;

import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class DeleteTaskDialog {

	public boolean taskWasAltered() {
		return false;
	}
}
