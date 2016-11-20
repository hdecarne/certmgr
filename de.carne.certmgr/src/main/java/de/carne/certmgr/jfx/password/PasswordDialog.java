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
package de.carne.certmgr.jfx.password;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import de.carne.certmgr.certs.PasswordCallback;
import de.carne.jfx.application.PlatformHelper;
import de.carne.jfx.stage.StageController;
import de.carne.util.Exceptions;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.util.Callback;

/**
 * This class implements the {@link PasswordCallback} interface by displaying
 * this package's "Enter password" or "Enter new password" dialogs.
 */
public final class PasswordDialog implements PasswordCallback {

	private final StageController owner;
	private final Class<? extends PasswordController> controllerClass;
	private boolean cancelAll = false;
	private char[] rememberedPassword = null;

	private PasswordDialog(StageController owner, Class<? extends PasswordController> passwordController) {
		this.owner = owner;
		this.controllerClass = passwordController;
	}

	/**
	 * Create a {@link PasswordCallback} for enter an existing password.
	 *
	 * @param owner The owner to use for dialog display.
	 * @return The created {@link PasswordCallback}.
	 */
	public static PasswordDialog enterPassword(StageController owner) {
		return new PasswordDialog(owner, EnterPasswordController.class);
	}

	/**
	 * Create a {@link PasswordCallback} for enter a new password.
	 *
	 * @param owner The owner to use for dialog display.
	 * @return The created {@link PasswordCallback}.
	 */
	public static PasswordDialog enterNewPassword(StageController owner) {
		return new PasswordDialog(owner, EnterNewPasswordController.class);
	}

	@Override
	public char[] queryPassword(String resource) {
		return queryPasswordHelper(() -> queryPasswordDialogHelper(resource, null));
	}

	@Override
	public char[] requeryPassword(String resource, Throwable cause) {
		return queryPasswordHelper(() -> queryPasswordDialogHelper(resource, cause));
	}

	private char[] queryPasswordHelper(Supplier<PasswordResult> query) {
		PasswordResult passwordResult;

		if (this.cancelAll) {
			passwordResult = PasswordResult.CANCEL;
		} else if (this.rememberedPassword != null) {
			passwordResult = new PasswordResult(ButtonType.YES, this.rememberedPassword, true);
		} else {
			passwordResult = PlatformHelper.runLater(query);
		}

		ButtonData dialogResultData = passwordResult.dialogResult().getButtonData();

		if (dialogResultData == ButtonData.YES) {
			this.cancelAll = false;
			this.rememberedPassword = (passwordResult.rememberPassword() ? passwordResult.password() : null);
		} else if (dialogResultData == ButtonData.NO) {
			this.cancelAll = false;
			this.rememberedPassword = null;
		} else {
			this.cancelAll = true;
			this.rememberedPassword = null;
		}
		return passwordResult.password();
	}

	private PasswordResult queryPasswordDialogHelper(String resource, Throwable cause) {
		PasswordResult passwordResult = PasswordResult.CANCEL;

		try {
			Optional<PasswordResult> dialogResult = this.owner
					.loadDialog((c) -> new PasswordInputDialog(c), this.controllerClass)
					.init(resource, this.rememberedPassword != null, cause).showAndWait();

			if (dialogResult.isPresent()) {
				passwordResult = dialogResult.get();
			}
		} catch (IOException e) {
			Exceptions.warn(e);
		}
		return passwordResult;
	}

	private class PasswordInputDialog extends Dialog<PasswordResult> {

		private final PasswordController controller;

		PasswordInputDialog(PasswordController controller) {
			this.controller = controller;
			setResultConverter(new Callback<ButtonType, PasswordResult>() {

				@Override
				public PasswordResult call(ButtonType param) {
					return getPasswordInputResult(param);
				}

			});
		}

		PasswordResult getPasswordInputResult(ButtonType button) {
			PasswordResult passwordResult = PasswordResult.CANCEL;
			ButtonData dialogResultData = button.getButtonData();

			if (dialogResultData == ButtonData.YES) {
				passwordResult = new PasswordResult(button, this.controller.getPasswordInput().toCharArray(),
						this.controller.getRememberPasswordOption());
			} else if (dialogResultData == ButtonData.NO) {
				passwordResult = new PasswordResult(button, null, this.controller.getRememberPasswordOption());
			}
			return passwordResult;
		}

	}

}
