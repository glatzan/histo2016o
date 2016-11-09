package org.histo.model.transitory;

import org.histo.model.interfaces.GsonAble;
import org.histo.util.FileUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class PdfTemplate implements GsonAble {
	
	public static final String UREPORT = "UREPORT";
	public static final String COUNCIL = "COUNCIL";
	public static final String INTERNAL_SHORT = "INTERNAL_SHORT";
	public static final String INTERNAL = "INTERNAL";
	public static final String SHORT = "SHORT";
	public static final String EXTERN = "EXTERN";
	public static final String MANUAL_REPOT = "MANUAL_REPOT";
	
	/**
	 * Name of the pdf file
	 */
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

	/**
	 * Type as String
	 */
	@Expose
	private String type;

	/**
	 * True if default template
	 */
	@Expose
	private boolean defaultTemplate;

	/**
	 * If external document is true, this document can not be genreated by the
	 * programm but has to be uploaded by the user.
	 */
	@Expose
	private boolean externalDocument;

	@Expose
	private CodeRectangle[] pizCode;

	@Expose
	private CodeRectangle[] taskCode;

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

	public CodeRectangle[] getPizCode() {
		return pizCode;
	}

	public CodeRectangle[] getTaskCode() {
		return taskCode;
	}

	public void setPizCode(CodeRectangle[] pizCode) {
		this.pizCode = pizCode;
	}

	public void setTaskCode(CodeRectangle[] taskCode) {
		this.taskCode = taskCode;
	}

	public class CodeRectangle {
		int x;
		int y;

		float width;
		float height;

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public float getWidth() {
			return width;
		}

		public float getHeight() {
			return height;
		}

		public void setX(int x) {
			this.x = x;
		}

		public void setY(int y) {
			this.y = y;
		}

		public void setWidth(float width) {
			this.width = width;
		}

		public void setHeight(float height) {
			this.height = height;
		}

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
	public static final PdfTemplate getTemplateByType(String jsonFile, String type) {
		PdfTemplate[] array = factroy(jsonFile);
		for (int i = 0; i < array.length; i++) {
			if (array[i].getType().equals(type))
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