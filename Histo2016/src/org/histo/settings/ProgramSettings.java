package org.histo.settings;

public class ProgramSettings {
	
	private boolean offline;
	
	private String workingDirectory;

	// ************************ Getter/Setter ************************
	public boolean isOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
}
