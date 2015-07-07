/*
 * Sonar Functional Test Harness
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Iain Adams
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.iadams.sonarqube.functional

import com.energizedwork.spock.extensions.TempDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * Functional Test Specification.
 *
 * Provides all the core functionality to the base specification
 * for functional tests to use.
 *
 * @author iwarapter
 */
class FunctionalSpecBase extends Specification {

	private static final Logger LOG = LoggerFactory.getLogger(FunctionalSpecBase.class);

	@TempDirectory(clean = false)
	protected File projectDir

	protected String moduleName
	protected File sonarProjectFile

	protected static boolean didSonarStart

	protected static String SONAR_URL = "http://localhost:9000"
	protected static String JAR_DIR = 'build/libs/'
	protected static String PLUGIN_DIR = 'extensions/plugins/'
	protected static String PLUGIN_NAME_REGEX

	/**
	 * The setup method is run before each test and will provide a working directory
	 * with a sonar-project.properties file configured with sensible defaults.
	 *
	 * @return
	 */
	def setup() {
		moduleName = findModuleName()

		if (!sonarProjectFile) {
			sonarProjectFile = new File(projectDir, 'sonar-project.properties')
		}
		sonarProjectFile << "sonar.projectKey=$moduleName\n"
		sonarProjectFile << "sonar.projectName=$moduleName\n"
		sonarProjectFile << "sonar.projectVersion=1.0\n"
		sonarProjectFile << "sonar.sources=.\n"
		sonarProjectFile << "sonar.scm.disabled=true\n"

		LOG.info "Running test from ${projectDir.getAbsolutePath()}"
	}

	def setupSpec() {
		String sonarHome = System.getenv('SONAR_HOME')
		LOG.info "SONAR_HOME: $sonarHome"
	}

	def cleanupSpec() {
		if (didSonarStart) {
		}
	}

	private String findModuleName() {
		projectDir.getName().replaceAll(/_\d+/, '')
	}

	//*********************//
	// Test Helper Methods //
	//*********************//
}