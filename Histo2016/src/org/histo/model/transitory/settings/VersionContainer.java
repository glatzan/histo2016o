package org.histo.model.transitory.settings;

import java.util.List;

public class VersionContainer {

	private List<Version> versions;
	
	private String currentVersion;

	public List<Version> getVersions() {
		return versions;
	}

	public void setVersions(List<Version> versions) {
		this.versions = versions;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}
	
	
}
