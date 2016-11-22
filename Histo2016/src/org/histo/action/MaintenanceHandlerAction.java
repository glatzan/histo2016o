package org.histo.action;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Dialog;
import org.histo.model.transitory.json.ProgramVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MaintenanceHandlerAction {

	@Autowired
	private MainHandlerAction mainHandlerAction;

	private ProgramVersion[] versionInfo;

	public void prepareInfoDialog() {
		setVersionInfo(ProgramVersion.factroy(HistoSettings.VERSION_JSON));
		mainHandlerAction.showDialog(Dialog.INFO);
	}

	public ProgramVersion[] getVersionInfo() {
		return versionInfo;
	}

	public void setVersionInfo(ProgramVersion[] versionInfo) {
		this.versionInfo = versionInfo;
	}

	
	public void requestUnlock(){
		
	}
}
