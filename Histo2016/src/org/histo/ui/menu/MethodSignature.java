package org.histo.ui.menu;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.primefaces.behavior.ajax.AjaxBehavior;
import org.primefaces.behavior.ajax.AjaxBehaviorListenerImpl;
import org.primefaces.component.commandbutton.CommandButton;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MethodSignature {

	private static Logger logger = Logger.getLogger("org.histo");

	private String signature;
	private String update;
	private String process;
	private String buttonId;

	private String onComplete;

	private CommandButton button;

	private VaribaleHolder<?>[] varibales;

	FacesContext fc;
	ExpressionFactory ef;
	ELContext context;

	public MethodSignature(String signature, VaribaleHolder<?>... varibales) {
		this(signature, null, null, null, varibales);
	}

	public MethodSignature(String signature, String update, String process, String onComplete,
			VaribaleHolder<?>... varibales) {
		this.varibales = varibales;
		this.signature = signature;
		this.update = update;
		this.process = process;
		this.onComplete = onComplete;
		this.buttonId = "button" + Math.round(Math.random() * 1000);
	}

	public MethodSignature addAjaxBehaviorToButton(String event, MethodSignature siganture) {
		AjaxBehavior ajaxBehavior = (AjaxBehavior) fc.getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);

		if (siganture.getProcess() != null)
			ajaxBehavior.setProcess(siganture.getProcess());
		if (siganture.getUpdate() != null)
			ajaxBehavior.setUpdate(siganture.getUpdate());
		if (siganture.getOnComplete() != null)
			ajaxBehavior.setOncomplete(siganture.getOnComplete());

		MethodExpression me = buildMethodExpression(ef, context, siganture.getSignature(), siganture.getVaribales());
		
		ajaxBehavior.addAjaxBehaviorListener(new AjaxBehaviorListenerImpl(me, me));

		getButton().addClientBehavior(event, ajaxBehavior);

		return this;
	}

	public MethodSignature generateButton() {
		fc = FacesContext.getCurrentInstance();
		ef = fc.getApplication().getExpressionFactory();
		context = fc.getELContext();

		CommandButton button = new CommandButton();
		button.setValue("");
		button.setStyle("visible:none");

		if (getProcess() != null)
			button.setProcess(getProcess());
		if (getUpdate() != null)
			button.setUpdate(getUpdate());
		if (getOnComplete() != null)
			button.setOncomplete(getOnComplete());

		// assign random id to avoid duplicate id for subsquent click
		button.setId(getButtonId());
		button.setActionExpression(buildMethodExpression(ef, context, getSignature(), getVaribales()));

		setButton(button);

		return this;
	}

	public MethodSignature addToParent(HtmlPanelGroup parent) {
		if (parent == null) {
			return this;
		}

		parent.getChildren().add(getButton());

		return this;
	}

	public MethodSignature setUpdate(String update) {
		getButton().setUpdate(update);
		this.update = update;
		return this;
	}

	public MethodSignature setProcess(String process) {
		getButton().setProcess(process);
		this.process = process;
		return this;
	}

	public MethodSignature setOncomplete(String onComplete) {
		getButton().setOncomplete(onComplete);
		this.onComplete = onComplete;
		return this;
	}

	private static MethodExpression buildMethodExpression(ExpressionFactory ef, ELContext context,
			String methodSignature, VaribaleHolder<?>[] varibales) {
		StringBuilder methodSignatureBuilder = new StringBuilder("#{" + methodSignature + "(");
		Class<?>[] classArr = null;
		
		if (varibales != null) {
			classArr = new Class<?>[varibales.length];
			// creating variables
			for (int i = 0; i < varibales.length; i++) {
				context.getVariableMapper().setVariable(varibales[i].getVaribaleName(),
						ef.createValueExpression(varibales[i].getObj(), varibales[i].getObj().getClass()));

				classArr[i] = varibales[i].getObj().getClass();
				methodSignatureBuilder.append(varibales[i].getVaribaleName() + (i != varibales.length - 1 ? "," : ""));
			}
		}

		methodSignatureBuilder.append(")}");
		logger.debug("Creating methode: " + methodSignatureBuilder.toString());

		return ef.createMethodExpression(context, methodSignatureBuilder.toString(), null, classArr);
	}

}

@Getter
@Setter
@AllArgsConstructor
class VaribaleHolder<T> {
	private T obj;
	private String varibaleName;
}
