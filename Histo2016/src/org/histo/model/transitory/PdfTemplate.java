package org.histo.model.transitory;

import java.lang.reflect.Type;

import org.histo.model.util.GsonAble;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class PdfTemplate implements GsonAble {

	@Expose
	private String name;
	@Expose
	private String fileWithLogo;
	@Expose
	private String fileWithOutLogo;
	@Expose
	private String type;
	@Expose
	private boolean defaultTemplate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFileWithLogo() {
		return fileWithLogo;
	}

	public void setFileWithLogo(String fileWithLogo) {
		this.fileWithLogo = fileWithLogo;
	}

	public String getFileWithOutLogo() {
		return fileWithOutLogo;
	}

	public void setFileWithOutLogo(String fileWithOutLogo) {
		this.fileWithOutLogo = fileWithOutLogo;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isDefaultTemplate() {
		return defaultTemplate;
	}

	public void setDefaultTemplate(boolean defaultTemplate) {
		this.defaultTemplate = defaultTemplate;
	}

	public static final PdfTemplate[] factroy(String json) {
		Type type = new TypeToken<PdfTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		PdfTemplate[] result = gson.fromJson(json, type);
		return result;
	}
}
