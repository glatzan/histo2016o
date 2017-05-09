package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SlideNamingDialogHandler extends AbstractDialog {

	public void initBean() {
		super.initBean(null, Dialog.SLIDE_NAMING);
	}
}
