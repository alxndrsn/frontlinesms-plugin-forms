/**
 * 
 */
package net.frontlinesms.plugins.forms.ui;

import java.awt.Image;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import thinlet.Thinlet;

import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.plugins.forms.FormsPluginController;
import net.frontlinesms.plugins.forms.data.domain.*;
import net.frontlinesms.plugins.forms.data.repository.*;
import net.frontlinesms.plugins.forms.ui.components.*;
import net.frontlinesms.plugins.BasePluginThinletTabController;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.UiGeneratorControllerConstants;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * Thinlet controller class for the FrontlineSMS Forms plugin.
 * @author Alex
 */
@TextResourceKeyOwner(prefix="I18N_")
public class FormsThinletTabController extends BasePluginThinletTabController<FormsPluginController> implements ThinletUiEventHandler {
//> CONSTANTS
	/** XML file containing forms pane for viewing results of a form */
	protected static final String UI_FILE_RESULTS_VIEW = "/ui/plugins/forms/formsTab_resultsView.xml";
	/** XML file containing dialog for exporting form data */
	private static final String UI_FILE_FORM_EXPORT_DIALOG = "/ui/plugins/forms/formExportDialogForm.xml";
	/** XML file containing dialog for choosing which contacts to send a form to */
	private static final String XML_CHOOSE_CONTACTS = "/ui/plugins/forms/dgChooseContacts.xml";
	
	/** Component name of the forms list */
	private static final String FORMS_LIST_COMPONENT_NAME = "formsList";
	
//> I18N KEYS
	/** i18n key: "Form Name" */
	static final String I18N_KEY_FORM_NAME = "forms.editor.name.label";
	/** i18n key: "Form Editor" */
	static final String I18N_KEY_FORMS_EDITOR = "forms.editor.title";
	/** i18n key: "You have not entered a name for this form" */
	static final String I18N_KEY_MESSAGE_FORM_NAME_BLANK = "forms.editor.name.validation.notset";
	/** i18n key: "You will not be able to edit this form again." */
	private static final String I18N_KEY_CONFIRM_FINALISE = "forms.send.finalise.confirm";
	/** i18n key: "There are no contacts to notify." */
	private static final String I18N_KEY_NO_CONTACTS_TO_NOTIFY = "forms.send.nocontacts";
	/** i18n key: "Form submitter" */
	public static final String I18N_FORM_SUBMITTER = "form.submitter";

	/** i18n key: "Currency field" */
	public static final String I18N_FCOMP_CURRENCY = "form.field.currency";
	public static final String I18N_FCOMP_DROP_DOWN_LIST = "common.dropdownlist";
	public static final String I18N_FCOMP_MENU_ITEM = "common.menuitem";
	public static final String I18N_FCOMP_NUMBER = "common.number";
	public static final String I18N_FCOMP_PASSWORD = "common.password";
	public static final String I18N_FCOMP_PHONENUMBER = "common.phonenumber";
	public static final String I18N_FCOMP_RADIO_BUTTON = "common.radiobutton";
	public static final String I18N_FCOMP_TEXT_AREA = "common.textarea";
	public static final String I18N_FCOMP_TEXT_FIELD = "common.textfield";
	public static final String I18N_FCOMP_BUTTON = "common.button";
	public static final String I18N_FCOMP_CHECKBOX = "common.checkbox";
	public static final String I18N_FCOMP_CREDITCARD = "common.creditcard";
	public static final String I18N_FCOMP_TIME = "common.time";
	public static final String I18N_FCOMP_TRUNCATED_TEXT = "common.truncatedtext";
	public static final String I18N_FCOMP_WRAPPED_TEXT = "common.wrappedtext";

	public static final String COMMON_PALETTE = "common.palette";
	public static final String COMMON_PREVIEW = "common.preview";
	public static final String COMMON_PROPERTY = "common.property";
	public static final String COMMON_VALUE = "common.value";
	public static final String TOOLTIP_DRAG_TO_REMOVE = "tooltip.drag.to.remove";
	public static final String TOOLTIP_DRAG_TO_PREVIEW = "tooltip.drag.to.preview";
	public static final String SENTENCE_DELETE_KEY = "sentence.delete.key";
	public static final String SENTENCE_UP_KEY = "sentence.up.key";
	public static final String SENTENCE_DOWN_KEY = "sentence.down.key";
	
//> INSTANCE PROPERTIES
	// FIXME work out what this is here for
	private Object formResultsComponent;
	/** DAO for {@link Contact}s */
	private ContactDao contactDao;
	/** DAO for {@link Form}s */
	private FormDao formsDao;
	/** DAO for {@link FormResponse}s */
	private FormResponseDao formResponseDao;

//> CONSTRUCTORS
	public FormsThinletTabController(FormsPluginController pluginController, UiGeneratorController uiController) {
		super(pluginController, uiController);
	}
	
//> INSTANCE METHODS	
	/** Refresh the tab's display. */
	public void refresh() {
		Object formList = getFormsList();
		
		// If there was something selected previously, we will attempt to select it again after updating the list
		Object previousSelectedItem = this.uiController.getSelectedItem(formList);
		Form previousSelectedForm = previousSelectedItem == null ? null : this.uiController.getAttachedObject(previousSelectedItem, Form.class);
		uiController.removeAll(formList);
		Object newSelectedItem = null;
		for(Form f : formsDao.getAllForms()) {
			Object formNode = getNode(f);
			uiController.add(formList, formNode);
			if(f.equals(previousSelectedForm)) {
				newSelectedItem = formNode;
			}
		}
		
		// Restore the selected item
		if(newSelectedItem != null) {
			this.uiController.setSelectedItem(formList, newSelectedItem);
		}

		// We should enable or disable buttons as appropriate
		formsList_selectionChanged();
	}
	
//> THINLET EVENT METHODS
	/** Show the dialog for exporting form results. */
	public void showFormExportDialog() {
		uiController.add(uiController.loadComponentFromFile(UI_FILE_FORM_EXPORT_DIALOG, this));
	}
	
	/** Show the AWT Forms Editor window */
	public void showFormsEditor() {
		VisualForm form = new VisualForm();
		form = FormsUiController.getInstance().showFormsEditor(uiController.getFrameLauncher(), form);
		if (form != null) {
			saveFormInformation(form);
		}
	}
	
	public void removeSelected(Object component) {
		Object[] selected = this.uiController.getSelectedItems(component);
		if(selected != null) {
			for(Object selectedComponent : selected) {
				this.uiController.remove(selectedComponent);
			}
		}
	}

	/**
	 * Called when the user has selected a different item on the forms tree.
	 * @param formsList
	 */
	public void formsList_selectionChanged() {
		Form selectedForm = getForm(uiController.getSelectedItem(getFormsList()));
		
		if (selectedForm != null) {
			if (selectedForm.isFinalised()) {
				showResultsPanel(selectedForm);
			}
		} else {
			//Nothing selected
			Object pnRight = find("pnRight");
			uiController.removeAll(pnRight);
		}
		formsTab_enabledFields();
	}
	
	/**
	 * Show the GUI to edit a form.
	 * @param list Reference to the Forms tree object.
	 */
	public void formsList_editSelected() {
		Form selectedForm = getSelectedForm();
		if (selectedForm != null) {
			VisualForm visualForm = VisualForm.getVisualForm(selectedForm);
			List<PreviewComponent> old = new ArrayList<PreviewComponent>();
			old.addAll(visualForm.getComponents());
			visualForm = FormsUiController.getInstance().showFormsEditor(uiController.getFrameLauncher(), visualForm);
			if (visualForm != null) {
				if (!visualForm.getName().equals(selectedForm.getName())) {
					selectedForm.setName(visualForm.getName());
				}
				updateForm(old, visualForm.getComponents(), selectedForm);
				formsList_selectionChanged();
			}
		}
	}
	
	/** Shows a selecter for assigning a {@link Group} to a {@link Form} */
	public void formsList_showGroupSelecter() {
		Form selectedForm = getSelectedForm();
		LOG.info("FormsThinletTabController.showGroupSelecter() : " + selectedForm);
		if(selectedForm != null) {
			// FIXME i18n
			uiController.showGroupSelecter(selectedForm, false, "Choose a group", "setSelectedGroup(groupSelecter, groupSelecter_groupList)", this);
		}
	}

	/**
	 * @param groupSelecter
	 * @param groupList
	 */
	public void setSelectedGroup(Object groupSelecter, Object groupList) {
		Form form = getForm(groupSelecter);
		LOG.info("Form: " + form);
		Group group = uiController.getGroup(uiController.getSelectedItem(groupList));
		LOG.info("Group: " + group);
		if(group != null) {
			// Set the permitted group for this form, then save it
			form.setPermittedGroup(group);
			this.formsDao.updateForm(form);
			this.refresh();
			
			removeDialog(groupSelecter);
		}
	}
	
	/**
	 * Attempt to send the form selected in the forms list
	 * @param formsList the forms list component
	 */
	public void formsList_sendSelected() {
		Form selectedForm = getSelectedForm();
		if(selectedForm != null) {
			// check the form has a group set
			if(selectedForm.getPermittedGroup() == null) {
				// The form has no group set, so we should explain that this needs to be done.
				// FIXME i18n
				uiController.alert("You must set a group for this form.\nYou can do this by right-clicking on the form.");
			} else if(!selectedForm.isFinalised()) { // check the form is finalised.
				// if form is not finalised, warn that it will be!
				uiController.showConfirmationDialog("showSendSelectionDialog", this, I18N_KEY_CONFIRM_FINALISE);
			} else {
				// show dialog for selecting group members to send the form to
				showSendSelectionDialog();
			}
		}
	}
	
	/**
	 * Show dialog for selecting users to send a form to.  If the form is not finalised, it will be
	 * finalised within this method.
	 */
	public void showSendSelectionDialog() {
		uiController.removeConfirmationDialog();
		
		Form form = getSelectedForm();
		if(form != null) {
			// if form is not finalised, finalise it now
			if(!form.isFinalised()) {
				formsDao.finaliseForm(form);
				this.refresh();
			}
			
			// show selection dialog for Contacts in the form's group
			Object chooseContactsDialog = uiController.loadComponentFromFile(XML_CHOOSE_CONTACTS, this);
			uiController.setAttachedObject(chooseContactsDialog, form);
			
			// Add each contact in the group to the list.  The user can then remove any contacts they don't
			// want to be sent an SMS about the form at this time.
			Object contactList = uiController.find(chooseContactsDialog, "lsContacts");
			for(Contact contact : form.getPermittedGroup().getAllMembers()) {
				Object listItem = uiController.createListItem(contact.getDisplayName(), contact);
				uiController.add(contactList, listItem);
			}
			uiController.add(chooseContactsDialog);
		}
	}
	
	/**
	 * Send a form to the contacts selected in the dialog.
	 * @param dgChooseContacts Dialog containing the contact selection
	 */
	public void sendForm(Object dgChooseContacts) {
		// Work out which contacts we should be sending the form to
		Object[] recipientItems = uiController.getItems(uiController.find(dgChooseContacts, "lsContacts"));
		Form form = getForm(dgChooseContacts);
		if(recipientItems.length == 0) {
			// There are no contacts in the "send to" list.  We should remove the dialog and inform the user
			// of the problem.
			uiController.alert(InternationalisationUtils.getI18NString(I18N_KEY_NO_CONTACTS_TO_NOTIFY));
			uiController.removeDialog(dgChooseContacts);
		} else {
			HashSet<Contact> selectedContacts = new HashSet<Contact>();
			for(Object o : recipientItems) {
				Object attachment = uiController.getAttachedObject(o);
				if(attachment instanceof Contact) {
					selectedContacts.add((Contact)attachment);
				} else if(attachment instanceof Group) {
					Group g = (Group)attachment;
					selectedContacts.addAll(g.getDirectMembers());
				}
			}
		
			// Issue the send command to the plugin controller
			this.getPluginController().sendForm(form, selectedContacts);

			// FIXME i18n
			uiController.alert("Your form '" + form.getName() + "' has been sent to " + selectedContacts.size() + " contacts.");
			
			uiController.removeDialog(dgChooseContacts);
		}
	}
	
	/** Finds the forms list and deletes the selected item. */
	public void formsList_deleteSelected() {
		Form selectedForm = getSelectedForm();
		if(selectedForm != null) {
			this.formsDao.deleteForm(selectedForm);
		}
		this.refresh();
		// Now remove the confirmation dialog.
		uiController.removeConfirmationDialog();
	}
	
	/**
	 * Duplicates the selected form.
	 * @param formsList
	 */
	public void formsList_duplicateSelected() {
		Form selected = getSelectedForm();
		assert(selected != null) : "Duplicate Form button should not be enabled if there is no form selected!";
		
		Form clone = new Form(selected.getName() + '*');
		for (FormField oldField : selected.getFields()) {
			FormField newField = new FormField(oldField.getType(), oldField.getLabel());
			clone.addField(newField, oldField.getPositionIndex());
		}
		this.formsDao.saveForm(clone);
		this.refresh();
	}
	
	/** Form selection has changed, so decide which toolbar and popup options should be available considering the current selection. */
	public void formsTab_enabledFields() {
		enableMenuOptions(find("formsList_toolbar"));
		enableMenuOptions(find("formsList_popupMenu"));
	}
	
	/**
	 * Enable menu options for the supplied menu component.
	 * @param menuComponent Menu component, a button bar or popup menu
	 * @param selectedComponent The selected object of the control that this menu applied to
	 */
	private void enableMenuOptions(Object menuComponent) {
		Object selectedComponent = formsList_getSelected();
		Form selectedForm = getForm(selectedComponent);
		for (Object o : uiController.getItems(menuComponent)) {
			String name = uiController.getName(o);
			if(name != null) { 
				if (name.contains("Delete")) {
					// Tricky to remove the component for a form when the field is selected.  If someone wants to
					// solve that, they're welcome to enable delete here for FormFields
					uiController.setEnabled(o, uiController.getAttachedObject(selectedComponent) instanceof Form);
				} else if (name.contains("Edit")) {
					uiController.setEnabled(o, selectedForm != null && !selectedForm.isFinalised());
				} else if (name.contains("New")) {
					uiController.setEnabled(o, true);
				} else {
					uiController.setEnabled(o, selectedForm != null);
				}
			}
		}
	}
	
	/** Update the results for the selected form, taking into account the page number as well. */
	public void formsTab_updateResults() {
		Form selected = getSelectedForm();
		assert(selected != null) : "Should not be attempting to update the Form's results view if no form is selected.";
		
		int limit = uiController.getListLimit(formResultsComponent);
		int pageNumber = uiController.getListCurrentPage(formResultsComponent);
		uiController.removeAll(formResultsComponent);
		
		if (selected != null) {
			for (FormResponse response : formResponseDao.getFormResponses(selected, (pageNumber - 1) * limit, limit)) {
				Object row = getRow(response);
				uiController.add(formResultsComponent, row);
			}
		}
		
		uiController.updatePageNumber(formResultsComponent, getTabComponent());
	}
	
	/**
	 * Shows a confirmation dialog before calling a method.  The method to be called
	 * is passed in as a string, and then called using reflection.
	 * @param methodToBeCalled
	 */
	public void showFormConfirmationDialog(String methodToBeCalled){
		uiController.showConfirmationDialog(methodToBeCalled, this);
	}

//> THINLET EVENT HELPER METHODS
	/** @return the {@link Form} selected in the {@link #getFormsList()}, or <code>null</code> if none is selected */
	private Form getSelectedForm() {
		Object selectedComponent = formsList_getSelected();
		if(selectedComponent == null) return null;
		else return getForm(selectedComponent);
	}
	
	/** @return gets the ui component selected in the forms list */
	private Object formsList_getSelected() {
		return this.uiController.getSelectedItem(getFormsList());
	}

	/** @return the forms list component */
	private Object getFormsList() {
		return find(FormsThinletTabController.FORMS_LIST_COMPONENT_NAME);
	}
	
	/** Given a {@link VisualForm}, the form edit window, this saves its details. */
	private void saveFormInformation(VisualForm visualForm) {
		Form form = new Form(visualForm.getName());
		for (PreviewComponent comp : visualForm.getComponents()) {
			FormFieldType fieldType = FComponent.getFieldType(comp.getComponent().getClass());
			FormField newField = new FormField(fieldType, comp.getComponent().getLabel());
			form.addField(newField);
		}
		this.formsDao.saveForm(form);
		this.refresh();
	}
	
	private void updateForm(List<PreviewComponent> old, List<PreviewComponent> newComp, Form form) {
		//Let's remove from database the ones the user removed
		List<PreviewComponent> toRemove = new ArrayList<PreviewComponent>();
		for (PreviewComponent c : old) {
			if (!newComp.contains(c)) {
				form.removeField(c.getFormField());
				toRemove.add(c);
			}
		}
		// Compare the lists
		for (PreviewComponent c : newComp) {
			if (c.getFormField() != null) {
				FormField ff = c.getFormField();
				if (ff.getPositionIndex() != newComp.indexOf(c)) {
					ff.setPositionIndex(newComp.indexOf(c));
				}
				ff.setLabel(c.getComponent().getLabel());
			} else {
				FormFieldType fieldType = FComponent.getFieldType(c.getComponent().getClass());
				FormField newField = new FormField(fieldType, c.getComponent().getLabel());
				form.addField(newField, newComp.indexOf(c));
			}
		}
		
		this.formsDao.updateForm(form);
		this.refresh();
	}
	
	/** Adds the result panel to the forms tab. */
	private void addFormResultsPanel() {
		Object pnRight = find("pnRight");
		uiController.removeAll(pnRight);
		Object resultsView = uiController.loadComponentFromFile(UI_FILE_RESULTS_VIEW, this);
		Object pagePanel = uiController.loadComponentFromFile(UiGeneratorControllerConstants.UI_FILE_PAGE_PANEL, this);
		Object placeholder = uiController.find(resultsView, "pageControlsPanel");
		int index = uiController.getIndex(uiController.getParent(placeholder), placeholder);
		uiController.add(uiController.getParent(placeholder), pagePanel, index);
		uiController.remove(placeholder);
		uiController.add(pnRight, resultsView);
		uiController.setPageMethods(getTabComponent(), "formResultsList", pagePanel);
		formResultsComponent = uiController.find(resultsView, "formResultsList");
		uiController.setListLimit(formResultsComponent);
		uiController.setListPageNumber(1, formResultsComponent);
		uiController.setAction(formResultsComponent, "formsTab_updateResults", this.getTabComponent(), this);
	}

	/**
	 * Adds the form results panel to the GUI, and refreshes it for the selected form.
	 * @param selected The form whose results should be displayed.
	 */
	private void showResultsPanel(Form selected) {
		addFormResultsPanel();
		Object pagePanel = find("pagePanel");
		uiController.setVisible(pagePanel, true);
		Object pnResults = find("pnFormResults");
		uiController.setInteger(pnResults, "columns", 2);
		
		int count = selected == null ? 0 : formResponseDao.getFormResponseCount(selected);
		form_createColumns(selected);
		uiController.setListPageNumber(1, formResultsComponent);
		uiController.setListElementCount(count, formResultsComponent);
		formsTab_updateResults();
		
		uiController.setEnabled(formResultsComponent, selected != null && uiController.getItems(formResultsComponent).length > 0);
		uiController.setEnabled(find("btExportFormResults"), selected != null && uiController.getItems(formResultsComponent).length > 0);
	}

	/**
	 * @param selectedComponent Screen component's selectedItem
	 * @return a {@link Form} if a form or formfield was selected, or <code>null</code> if none could be found
	 */
	private Form getForm(Object selectedComponent) {
		Object selectedAttachment = uiController.getAttachedObject(selectedComponent);
		if (selectedAttachment == null
				|| !(selectedAttachment instanceof Form)) {
			// The selected item was not a form item, so probably was a child of that.  Get it's parent, and check if that was a form instead
			selectedAttachment = this.uiController.getAttachedObject(this.uiController.getParent(selectedComponent));
		}
		
		if (selectedAttachment == null
				|| !(selectedAttachment instanceof Form)) {
			// No form was found; return null
			return null;
		} else {
			return (Form) selectedAttachment;
		}
	}
	
	/**
	 * Gets {@link Thinlet} table row component for the supplied {@link FormResponse}
	 * @param response the {@link FormResponse} to represent as a table row
	 * @return row component to insert in a thinlet table
	 */
	private Object getRow(FormResponse response) {
		Object row = uiController.createTableRow(response);
		Contact sender = contactDao.getFromMsisdn(response.getSubmitter());
		String senderDisplayName = sender != null ? sender.getDisplayName() : response.getSubmitter();
		uiController.add(row, uiController.createTableCell(senderDisplayName));
		for (ResponseValue result : response.getResults()) {
			uiController.add(row, uiController.createTableCell(result.toString()));
		}
		return row;
	}
	
	/**
	 * Creates a {@link Thinlet} tree node for the supplied form.
	 * @param form The form to represent as a node.
	 * @return node to insert in thinlet tree
	 */
	private Object getNode(Form form) {
		LOG.trace("ENTER");
		// Create the node for this form
		
		LOG.debug("Form [" + form.getName() + "]");
		
		Image icon = getIcon(form.isFinalised() ? FormIcon.FORM_FINALISED: FormIcon.FORM);
		Object node = uiController.createNode(form.getName(), form);
		uiController.setIcon(node, Thinlet.ICON, icon);

		// Create a node showing the group for this form
		Group g = form.getPermittedGroup();
		// FIXME i18n
		String groupName = g == null ? "(not set)" : g.getName();
		// FIXME i18n
		Object groupNode = uiController.createNode("Group: " + groupName, null);
		uiController.setIcon(groupNode, Icon.GROUP);
		uiController.add(node, groupNode);
		
		for (FormField field : form.getFields()) {
			Object child = uiController.createNode(field.getLabel(), field);
			uiController.setIcon(child, Thinlet.ICON, getIcon(field.getType()));
			uiController.add(node, child);
		}
		LOG.trace("EXIT");
		return node;
	}

	private void form_createColumns(Form selected) {
		Object resultsTable = find("formResultsList");
		Object header = uiController.get(resultsTable, Thinlet.HEADER);
		uiController.removeAll(header);
		if (selected != null) {
			// FIXME check if this constant can be removed from frontlinesmsconstants class
			Object column = uiController.createColumn(InternationalisationUtils.getI18NString(I18N_FORM_SUBMITTER), null);
			uiController.setWidth(column, 100);
			uiController.setIcon(column, Icon.PHONE_CONNECTED);
			uiController.add(header, column);
			// For some reason we have a number column
			int count = 0;
			for (FormField field : selected.getFields()) {
				if(field.getType().hasValue()) {
					column = uiController.createColumn(field.getLabel(), new Integer(++count));
					uiController.setInteger(column, "width", 100);
					uiController.setIcon(column, getIcon(field.getType()));
					uiController.add(header, column);
				}
			}
		}
	}

//> ACCESSORS
	/**
	 * Set {@link FormDao}
	 * @param formsDao new value for {@link #formsDao}
	 */
	public void setFormsDao(FormDao formsDao) {
		this.formsDao = formsDao;
	}
	
	/**
	 * Set {@link FormResponseDao}
	 * @param formResponseDao new value for {@link FormResponseDao}
	 */
	public void setFormResponseDao(FormResponseDao formResponseDao) {
		this.formResponseDao = formResponseDao;
	}
	
	/**
	 * Set {@link #contactDao}
	 * @param contactDao new value for {@link #contactDao}
	 */
	public void setContactDao(ContactDao contactDao) {
		this.contactDao = contactDao;
	}
	
//> TEMPORARY METHODS THAT NEED SORTING OUT
	/**
	 * Gets an icon with the specified name.
	 * @param iconPath
	 * @return currently this returns <code>null</code> - needs to be implemented!
	 */
	private Image getIcon(String iconPath) {
		return this.uiController.getIcon(iconPath);
	}
	
	/**
	 * Gets the icon for a particular {@link FComponent}.
	 * @param fieldType
	 * @return icon to use for a particular {@link FComponent}.
	 */
	public Image getIcon(FormFieldType fieldType) {
		if(fieldType == FormFieldType.CHECK_BOX)			return getIcon(FormIcon.CHECKBOX);
		if(fieldType == FormFieldType.CURRENCY_FIELD)		return getIcon(FormIcon.CURRENCY_FIELD);
		if(fieldType == FormFieldType.DATE_FIELD)			return getIcon(FormIcon.DATE_FIELD);
		if(fieldType == FormFieldType.EMAIL_FIELD)			return getIcon(FormIcon.EMAIL_FIELD);
		if(fieldType == FormFieldType.NUMERIC_TEXT_FIELD)	return getIcon(FormIcon.NUMERIC_TEXT_FIELD);
		if(fieldType == FormFieldType.PASSWORD_FIELD) 		return getIcon(FormIcon.PASSWORD_FIELD);
		if(fieldType == FormFieldType.PHONE_NUMBER_FIELD) 	return getIcon(FormIcon.PHONE_NUMBER_FIELD);
		if(fieldType == FormFieldType.TEXT_AREA)			return getIcon(FormIcon.TEXT_AREA);
		if(fieldType == FormFieldType.TEXT_FIELD) 			return getIcon(FormIcon.TEXT_FIELD);
		if(fieldType == FormFieldType.TIME_FIELD) 			return getIcon(FormIcon.TIME_FIELD);
		if(fieldType == FormFieldType.TRUNCATED_TEXT) 		return getIcon(FormIcon.TRUNCATED_TEXT);
		if(fieldType == FormFieldType.WRAPPED_TEXT) 		return getIcon(FormIcon.WRAPPED_TEXT);
		throw new IllegalStateException("No icon is mapped for field type: " + fieldType);
	}
}
