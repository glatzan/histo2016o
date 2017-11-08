package org.histo.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgramSettings {
	
	private boolean offline;
	
	private String workingDirectory;
	
	private String phoneRegex;
	
	private DefaultDocuments defaultDocuments;

	@Getter
	@Setter
	public class DefaultDocuments{
		private long defaultTaskCreationDocument;
		private long defaultSendReport;
	}
}
