/*
 * Copyright (c) 2015-2016 Holger de Carne and contributors, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.carne.certmgr.jfx.storepreferences;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import de.carne.certmgr.certs.UserCertStore;
import de.carne.certmgr.certs.UserCertStorePreferences;
import de.carne.certmgr.certs.security.CRLUpdatePeriod;
import de.carne.certmgr.certs.security.CRTValidityPeriod;
import de.carne.certmgr.certs.security.KeyPairAlgorithm;
import de.carne.certmgr.certs.security.SignatureAlgorithm;
import de.carne.certmgr.jfx.util.converter.CRLUpdatePeriodStringConverter;
import de.carne.certmgr.jfx.util.converter.CRTValidityPeriodStringConverter;
import de.carne.certmgr.util.Days;
import de.carne.jfx.scene.control.Alerts;
import de.carne.jfx.scene.control.Controls;
import de.carne.jfx.scene.control.DialogController;
import de.carne.jfx.util.validation.ValidationAlerts;
import de.carne.util.Strings;
import de.carne.util.validation.InputValidator;
import de.carne.util.validation.PathValidator;
import de.carne.util.validation.ValidationException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

/**
 * Store options dialog.
 */
public class StorePreferencesController extends DialogController<UserCertStore>
		implements Callback<ButtonType, UserCertStore> {

	private UserCertStore store = null;

	private UserCertStorePreferences storePreferences = null;

	private boolean expertMode = false;

	@FXML
	TextField ctlNameInput;

	@FXML
	TextField ctlPathInput;

	@FXML
	Button cmdChoosePathButton;

	@FXML
	ComboBox<CRTValidityPeriod> ctlDefCRTValidityInput;

	@FXML
	ComboBox<CRLUpdatePeriod> ctlDefCRLUpdateInput;

	@FXML
	ComboBox<KeyPairAlgorithm> ctlDefKeyAlgOption;

	@FXML
	ComboBox<Integer> ctlDefKeySizeOption;

	@FXML
	ComboBox<SignatureAlgorithm> ctlDefSigAlgOption;

	@FXML
	void onCmdChoosePath(ActionEvent evt) {
		DirectoryChooser chooser = new DirectoryChooser();
		File path = chooser.showDialog(getWindow());

		if (path != null) {
			this.ctlPathInput.setText(path.toString());
		}
	}

	private void onDefKeyAlgChanged(KeyPairAlgorithm keyAlg) {
		Integer keySizeDefaultHint = null;
		String sigAlgDefaultHint = null;

		if (this.storePreferences != null
				&& keyAlg.algorithm().equals(this.storePreferences.defaultKeyPairAlgorithm.get())) {
			keySizeDefaultHint = this.storePreferences.defaultKeySize.get();
			sigAlgDefaultHint = this.storePreferences.defaultSignatureAlgorithm.get();
		}
		Controls.resetComboBoxOptions(this.ctlDefKeySizeOption, keyAlg.getStandardKeySizes(keySizeDefaultHint),
				(o1, o2) -> o1.compareTo(o2));
		Controls.resetComboBoxOptions(this.ctlDefSigAlgOption,
				SignatureAlgorithm.getDefaultSet(keyAlg.algorithm(), sigAlgDefaultHint, this.expertMode),
				(o1, o2) -> o1.toString().compareTo(o2.toString()));
	}

	private void onApply(ActionEvent evt) {
		if (this.store == null) {
			try {
				Path storeHome = validateStoreHomeInput();

				this.store = UserCertStore.createStore(storeHome);
				this.storePreferences = this.store.storePreferences();
			} catch (ValidationException e) {
				ValidationAlerts.error(e).showAndWait();
				evt.consume();
			} catch (Exception e) {
				Alerts.unexpected(e).showAndWait();
				evt.consume();
			}
		}
		if (this.storePreferences != null) {
			try {
				this.storePreferences.defaultCRTValidityPeriod.put(validateDefCRTValidityInput().days().count());
				this.storePreferences.defaultCRLUpdatePeriod.put(validateDefCRLUpdateInput().days().count());
				this.storePreferences.defaultKeyPairAlgorithm.put(validateDefKeyAlgInput().algorithm());
				this.storePreferences.defaultKeySize.put(validateDefKeySizeInput());
				this.storePreferences.defaultSignatureAlgorithm.put(validateDefSigAlgInput().algorithm());
				this.storePreferences.sync();
			} catch (ValidationException e) {
				ValidationAlerts.error(e).showAndWait();
				evt.consume();
			} catch (Exception e) {
				Alerts.unexpected(e).showAndWait();
				evt.consume();
			}
		}
	}

	@Override
	protected void setupDialog(Dialog<UserCertStore> dialog) {
		dialog.setTitle(StorePreferencesI18N.formatSTR_STAGE_TITLE());
		this.ctlDefKeyAlgOption.valueProperty().addListener((p, o, n) -> onDefKeyAlgChanged(n));
		this.ctlDefKeySizeOption.setConverter(new IntegerStringConverter());
		addButtonEventFilter(ButtonType.APPLY, (evt) -> onApply(evt));
	}

	/**
	 * Initialize dialog for creating a new store.
	 *
	 * @param expertModeParam Whether to run in expert mode ({@code true}) or
	 *        not ({@code false}).
	 * @return This controller.
	 */
	public StorePreferencesController init(boolean expertModeParam) {
		this.store = null;
		this.expertMode = expertModeParam;
		initExpertMode();
		initDefCRTValidityPeriods();
		initDefCRLUpdatePeriods();
		initDefKeyAlgOptions();
		((Button) lookupButton(ButtonType.APPLY)).setText(StorePreferencesI18N.formatSTR_TEXT_CREATE());
		return this;
	}

	/**
	 * Initialize dialog for editing an existing store's preferences.
	 *
	 * @param storeParam The store to edit the preferences for.
	 * @param expertModeParam Whether to run in expert mode ({@code true}) or
	 *        not ({@code false}).
	 * @return This controller.
	 */
	public StorePreferencesController init(UserCertStore storeParam, boolean expertModeParam) {
		assert storeParam != null;

		this.store = storeParam;
		this.storePreferences = this.store.storePreferences();
		this.expertMode = expertModeParam;

		Path storeHome = this.store.storeHome();

		this.ctlNameInput.setText(storeHome.getFileName().toString());
		this.ctlNameInput.setDisable(true);
		this.ctlPathInput.setText(storeHome.getParent().toString());
		this.ctlPathInput.setDisable(true);
		this.cmdChoosePathButton.setDisable(true);
		initExpertMode();
		initDefCRTValidityPeriods();
		initDefCRLUpdatePeriods();
		initDefKeyAlgOptions();
		return this;
	}

	private void initExpertMode() {
		this.ctlDefCRTValidityInput.setEditable(this.expertMode);

		CRTValidityPeriodStringConverter defCRTValidityConverter = new CRTValidityPeriodStringConverter();

		defCRTValidityConverter.attach(this.ctlDefCRTValidityInput);
		this.ctlDefCRLUpdateInput.setEditable(this.expertMode);

		CRLUpdatePeriodStringConverter defCRLUpdateConverter = new CRLUpdatePeriodStringConverter();

		defCRLUpdateConverter.attach(this.ctlDefCRLUpdateInput);
		this.ctlDefKeySizeOption.setEditable(this.expertMode);
	}

	private void initDefCRTValidityPeriods() {
		Days defaultHint = null;

		if (this.storePreferences != null) {
			defaultHint = new Days(this.storePreferences.defaultCRTValidityPeriod.getInt(0));
		}
		Controls.resetComboBoxOptions(this.ctlDefCRTValidityInput, CRTValidityPeriod.getDefaultSet(defaultHint),
				(o1, o2) -> o1.days().compareTo(o2.days()));
	}

	private void initDefCRLUpdatePeriods() {
		Days defaultHint = null;

		if (this.storePreferences != null) {
			defaultHint = new Days(this.storePreferences.defaultCRLUpdatePeriod.getInt(0));
		}
		Controls.resetComboBoxOptions(this.ctlDefCRLUpdateInput, CRLUpdatePeriod.getDefaultSet(defaultHint),
				(o1, o2) -> o1.days().compareTo(o2.days()));
	}

	private void initDefKeyAlgOptions() {
		String defaultHint = null;

		if (this.storePreferences != null) {
			defaultHint = this.storePreferences.defaultKeyPairAlgorithm.get();
		}
		Controls.resetComboBoxOptions(this.ctlDefKeyAlgOption,
				KeyPairAlgorithm.getDefaultSet(defaultHint, this.expertMode),
				(o1, o2) -> o1.toString().compareTo(o2.toString()));
	}

	@Override
	public UserCertStore call(ButtonType param) {
		return this.store;
	}

	private Path validateStoreHomeInput() throws ValidationException {
		String nameInput = InputValidator.notEmpty(Strings.safeTrim(this.ctlNameInput.getText()),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_NAME(a));
		String pathInput = InputValidator.notEmpty(Strings.safeTrim(this.ctlPathInput.getText()),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_PATH(a));
		Path path = PathValidator.isWritableDirectory(pathInput,
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_INVALID_PATH(a));
		Path storeHome = PathValidator.isPath(path, nameInput,
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_INVALID_NAME(a));

		InputValidator.isTrue(!Files.exists(storeHome),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_STORE_HOME_EXISTS(storeHome));
		return storeHome;
	}

	private CRTValidityPeriod validateDefCRTValidityInput() throws ValidationException {
		return InputValidator.notNull(this.ctlDefCRTValidityInput.getValue(),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_DEFCRTVALIDITY(a));
	}

	private CRLUpdatePeriod validateDefCRLUpdateInput() throws ValidationException {
		return InputValidator.notNull(this.ctlDefCRLUpdateInput.getValue(),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_DEFCRLUPDATE(a));
	}

	private KeyPairAlgorithm validateDefKeyAlgInput() throws ValidationException {
		return InputValidator.notNull(this.ctlDefKeyAlgOption.getValue(),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_DEFKEYALG(a));
	}

	private Integer validateDefKeySizeInput() throws ValidationException {
		return InputValidator.notNull(this.ctlDefKeySizeOption.getValue(),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_DEFKEYSIZE(a));
	}

	private SignatureAlgorithm validateDefSigAlgInput() throws ValidationException {
		return InputValidator.notNull(this.ctlDefSigAlgOption.getValue(),
				(a) -> StorePreferencesI18N.formatSTR_MESSAGE_NO_DEFSIGALG(a));
	}

}