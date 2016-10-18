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
package de.carne.certmgr.jfx;

import de.carne.certmgr.jfx.store.StoreController;
import de.carne.jfx.stage.StageController;
import de.carne.util.cmdline.CmdLine;
import de.carne.util.cmdline.CmdLineException;
import de.carne.util.logging.Log;
import de.carne.util.logging.LogConfig;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application class responsible for running the UI.
 */
public class CertMgrApplication extends Application {

	private static final Log LOG = new Log();

	/**
	 * Launch this JavaFX application.
	 *
	 * @param args The application's command line.
	 */
	public static void launch(String[] args) {
		Application.launch(CertMgrApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Evaluate command line as soon as possible to apply logging options as
		// soon as possible
		evalCmdLine();

		LOG.info("JavaFX GUI starting...");

		StoreController store = StageController.loadPrimaryStage(primaryStage, StoreController.class);

		store.show();
	}

	@Override
	public void stop() throws Exception {
		LOG.info("JavaFX GUI stopped");
	}

	private void evalCmdLine() {
		CmdLine cmdLine = new CmdLine(getParameters().getRaw());

		cmdLine.switchAction((s) -> LogConfig.applyConfig(LogConfig.CONFIG_VERBOSE)).arg("--verbose");
		cmdLine.switchAction((s) -> LogConfig.applyConfig(LogConfig.CONFIG_DEBUG)).arg("--debug");
		try {
			cmdLine.eval();
			LOG.info("Running command line ''{0}''", cmdLine);
		} catch (CmdLineException e) {
			LOG.warning(e, "Invalid argument usage for ''{0}'' in command line ''{0}''; ", e.getArg(), cmdLine);
		}
	}

}
