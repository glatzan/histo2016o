package org.histo.util;

import org.histo.model.util.GsonAble;

import com.google.gson.annotations.Expose;

public class PDFTemplate implements GsonAble {
	
	@Expose
	private String name;
	@Expose
	private String fileWithLogo;
	@Expose
	private String fileWithOutLogo;
	@Expose
	private PDFTemplate type;
	
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
	public PDFTemplate getType() {
		return type;
	}
	public void setType(PDFTemplate type) {
		this.type = type;
	}
	
	public static final PDFTemplate[] factroy(String json){
//		Type type = new TypeToken<List<String[]>>() {}.getType();
//
//		3.- Finally parse the JSON into a structure of type type:
//
//		List<String[]> yourList = gson.fromJson(yourJsonString, type);
		return null;
	}
}
