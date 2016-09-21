package org.histo.model.util.transientObjects;

import org.histo.config.enums.PDFTemplateEnum;
import org.histo.model.util.GsonAble;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class PDFTemplate implements GsonAble {

	@Expose
	private String name;
	@Expose
	private String fileWithLogo;
	@Expose
	private String fileWithOutLogo;
	@Expose
	private PDFTemplateEnum type;

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

	public PDFTemplateEnum getType() {
		return type;
	}

	public void setType(PDFTemplateEnum type) {
		this.type = type;
	}

	public static final PDFTemplate[] factroy(String json) {
		Type type = new TypeToken<PDFTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		PDFTemplate[] result = gson.fromJson(json, type);
		return result;
	}
}
