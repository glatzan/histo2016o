package org.histo.config.enums;

public enum PdfTemplate {

	UREPROT("uReport", ""), 
	COUNCIL("concil", "json.pdfTemplate.council", "templates/Final5-Logo.pdf", "templates/Final5-noLogo.pdf", false),
	SHORTREPORT("shortReport", "json.pdfTemplate.shortReport", "templates/Final5-Logo.pdf", "templates/Final5-noLogo.pdf", false),
	INTERNAL("internal", "json.pdfTemplate.diagnosis.internal", "templates/Final5-Logo.pdf", "templates/Final5-noLogo.pdf", false),
	INTERNAL_EXTENDED("internalExtended", "json.pdfTemplate.diagnosis.internal.extended", "templates/Final5-Logo.pdf", "templates/Final5-noLogo.pdf", true),
	FAMILY_PHYSICIAN("familyPhysician", "json.pdfTemplate.shortReport", "templates/Final5-Logo.pdf", "templates/Final5-noLogo.pdf", false),
	PRIVATE_PHYSICIAN("privatePhysician", "json.pdfTemplate.shortReport", "templates/Final5-Logo.pdf", "templates/Final5-noLogo.pdf", false);

	private final String type;

	private final String name;

	private final String fileWithLogo;

	private final String fileWithoutLogo;

	private final boolean defaultTemplate;

	private PdfTemplate(final String type, final String name) {
		this.type = type;
		this.name = name;
		this.fileWithLogo = null;
		this.fileWithoutLogo = null;
		this.defaultTemplate = false;
	}

	private PdfTemplate(final String type, final String name, final String fileWithLogo, final String fileWithoutLogo,
			final boolean defaultTemplate) {
		this.type = type;
		this.name = name;
		this.fileWithLogo = fileWithLogo;
		this.fileWithoutLogo = fileWithoutLogo;
		this.defaultTemplate = defaultTemplate;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getFileWithLogo(){
		return fileWithLogo;
	}

	public String getFileWithoutLogo(){
		return fileWithoutLogo;
	}

	public boolean isDefaultTemplate(){
		return defaultTemplate;
	}
}
