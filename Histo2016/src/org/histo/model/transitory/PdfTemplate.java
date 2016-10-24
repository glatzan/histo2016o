package org.histo.model.transitory;

import org.histo.config.enums.BuildInTemplates;
import org.histo.model.util.GsonAble;
import org.histo.util.FileUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class PdfTemplate implements GsonAble {

	public static final String BUILD_IN_UREPORT = "uReport";
	public static final String BUILD_IN_INTERNAl_EXTENDED = "internalExtended";
	public static final String BUILD_IN_COUNCIL = "uReport";

	@Expose
	private String name;
	/**
	 * If true the name will be taken from the resoucesProvider
	 */
	@Expose
	private boolean nameAsResources;
	@Expose
	private String fileWithLogo;
	@Expose
	private String fileWithOutLogo;
	@Expose
	private String type;
	@Expose
	private boolean defaultTemplate;

	/**
	 * If external document is true, this document can not be genreated by the
	 * programm but has to be uploaded by the user.
	 */
	@Expose
	private boolean externalDocument;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isNameAsResources() {
		return nameAsResources;
	}

	public void setNameAsResources(boolean nameAsResources) {
		this.nameAsResources = nameAsResources;
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

	public boolean isExternalDocument() {
		return externalDocument;
	}

	public void setExternalDocument(boolean externalDocument) {
		this.externalDocument = externalDocument;
	}

	public static final PdfTemplate[] factroy(String jsonFile) {

		Type type = new TypeToken<PdfTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		PdfTemplate[] result = gson.fromJson(FileUtil.loadTextFile(jsonFile), type);
		return result;
	}

	/**
	 * Return the first template of an array which is marked as default.
	 * 
	 * @param array
	 * @return
	 */
	public static final PdfTemplate getDefaultTemplate(PdfTemplate[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].isDefaultTemplate())
				return array[i];
		}
		return null;
	}

	/**
	 * Returns a template matching the given type string.
	 * 
	 * @param array
	 * @param type
	 * @return
	 */
	public static final PdfTemplate getTemplateByType(PdfTemplate[] array, String type) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].getType().equals(type))
				return array[i];
		}
		return null;
	}

	/**
	 * Returns an array with only templates that can be used by the program to
	 * generate a report
	 * 
	 * @param json
	 * @return
	 */
	public static final PdfTemplate[] getInternalReportsOnly(String jsonFile) {
		PdfTemplate[] tmp = factroy(jsonFile);
		ArrayList<PdfTemplate> result = new ArrayList<>();
		for (int i = 0; i < tmp.length; i++) {
			if (!tmp[i].isExternalDocument())
				result.add(tmp[i]);
		}
		return result.toArray(new PdfTemplate[result.size()]);

	}
}