package org.histo.util.latex;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

public class TextToLatexConverter {

	private Config config;

	public TextToLatexConverter() {
		this.config = new Config();
	}

	public TextToLatexConverter(Config config) {
		this.config = config;
	}

	public String convertToTex(String string) {
		string = StringUtils.replace(string, "\r\n", config.getLineBreak());
		System.out.println(string);
		return string;
	}

	@Getter
	@Setter
	public class Config {
		private String lineBreak = "\r\n\\\\[1mm]";
	}
}
