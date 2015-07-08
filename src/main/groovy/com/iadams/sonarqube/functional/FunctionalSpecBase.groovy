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
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import spock.lang.Specification

/**
 * Functional Test Specification.
 *
 * Provides all the core functionality to the base specification
 * for functional tests to use.
 *
 * @author iwarapter
 */
@Slf4j
class FunctionalSpecBase extends Specification {

	@TempDirectory(clean = false)
	protected File projectDir

	protected String moduleName
	protected File sonarProjectFile

	protected static boolean didSonarStart

	protected static String SONAR_URL = "http://localhost:9000"
	protected static String JAR_DIR = 'build/libs/'
	protected static String PLUGIN_DIR = 'extensions/plugins/'
	protected static String PLUGIN_NAME_REGEX

	protected static SonarRunnerResult sonarRunnerResult
	protected static File logFile

	/**
	 * The setup method is run before each test and will provide a working directory
	 * with a sonar-project.properties file configured with sensible defaults.
	 *
	 * @return
	 */
	def setup() {
		moduleName = findModuleName()
		sonarRunnerResult = null
		logFile = null

		if (!sonarProjectFile) {
			sonarProjectFile = new File(projectDir, 'sonar-project.properties')
		}
		sonarProjectFile << "sonar.projectKey=$moduleName\n"
		sonarProjectFile << "sonar.projectName=$moduleName\n"
		sonarProjectFile << "sonar.projectVersion=1.0\n"
		sonarProjectFile << "sonar.sources=.\n"
		sonarProjectFile << "sonar.scm.disabled=true\n"

		log.info "Running test from ${projectDir.getAbsolutePath()}"
	}

	def setupSpec() {
		String sonarHome = System.getenv('SONAR_HOME')
		log.info "SONAR_HOME: $sonarHome"
	}

	def cleanupSpec() {
		if (didSonarStart) {
		}
	}

	private String findModuleName() {
		projectDir.getName().replaceAll(/_\d+/, '')
	}

	//
	// Test Setup Methods
	//

	/**
	 * Created a directory in the test and returns the file handle.
	 *
	 * @param path
	 * @param baseDir
	 * @return the new directory
	 */
	protected File directory(String path, File baseDir = projectDir) {
		new File(baseDir, path).with {
			mkdirs()
			it
		}
	}

	/**
	 * Creates a file in the test and returns the file handle.
	 *
	 * @param path
	 * @param baseDir
	 * @return the new file
	 */
	protected File file(String path, File baseDir = projectDir) {
		def splitted = path.split('/')
		def directory = splitted.size() > 1 ? directory(splitted[0..-2].join('/'), baseDir) : baseDir
		def file = new File(directory, splitted[-1])
		file.createNewFile()
		file
	}

	/**
	 * Copy a given set of files/directories
	 *
	 * @param srcDir
	 * @param destination
	 */
	protected void copyResources(String srcDir, String destination) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(srcDir);
		if (resource == null) {
			throw new RuntimeException("Could not find classpath resource: $srcDir")
		}

		File destinationFile = file(destination)
		File resourceFile = new File(resource.toURI())
		if (resourceFile.file) {
			FileUtils.copyFile(resourceFile, destinationFile)
		} else {
			FileUtils.copyDirectory(resourceFile, destinationFile)
		}
	}

	//
	// Test Helper Methods
	//

	/**
	 * Runs sonar-runner with no arguments and updates the test {@link SonarRunnerResult}
	 */
	void runSonarRunner(){
		sonarRunnerResult = SonarRunnerHelper.runSonarRunner('', projectDir)
		logFile = new File(projectDir, "$moduleName-analysis.log")
		logFile.write(sonarRunnerResult.output + sonarRunnerResult.error)
	}

	/**
	 * Runs sonar-runner with no arguments and updates the test {@link SonarRunnerResult}
	 */
	void runSonarRunnerWithArguments(String args){
		sonarRunnerResult = SonarRunnerHelper.runSonarRunner(args, projectDir)
		logFile = new File(projectDir, "$moduleName-analysis.log")
		logFile.write(sonarRunnerResult.output + sonarRunnerResult.error)
	}

	/**
	 * Checks the {@link SonarRunnerResult} to ensure sonar-runner exited successfully.
	 */
	void analysisFinishedSuccessfully(){
		assert sonarRunnerResult != null : "Result is null, have you run sonar runner first?"
		sonarRunnerResult.exitValue == 0
	}

	/**
	 * Checks the {@link SonarRunnerResult} to ensure sonar-runner exited successfully.
	 */
	void analysisFailed(){
		assert sonarRunnerResult != null : "Result is null, have you run sonar runner first?"
		sonarRunnerResult.exitValue != 0
	}

	/**
	 * Checks the logFile to see if it contains the given line.
	 *
	 * @param line
	 * @return True/False
	 */
	boolean analysisLogContains(String line){
		for(String s : logFile.readLines()){
			if(s.contains(line)){ return true }
		}
		false
	}

	/**
	 * Checks if the given metric/value keypair exists on the SonarQube server for a project.
	 *
	 * @param metrics_to_query
	 */
	void theFollowingProjectMetricsHaveTheFollowingValue(Map<String, Float> metrics_to_query){
		SonarWebServiceAPI.containsMetrics(SONAR_URL, moduleName, metrics_to_query.sort())
	}

	/**
	 * Checks if the given metric/value keypair exists on the SonarQube server for a file.
	 *
	 * @param file
	 * @param metrics_to_query
	 */
	void theFollowingFileMetricsHaveTheFollowingValue(String file ,Map<String, Float> metrics_to_query){
		SonarWebServiceAPI.containsMetrics(SONAR_URL, "$moduleName:$file", metrics_to_query.sort())
	}

	/**
	 * Deactivates all the rules for the given language and profile.
	 *
	 * @param language
	 * @param profile
	 */
	void deactivateAllRules(String language, String profile){
		SonarWebServiceAPI.deactivateAllRules(SONAR_URL, language, profile)
	}

	/**
	 * Restores the language default profiles.
	 *
	 * @param language
	 */
	void resetDefaultProfile(String language){
		SonarWebServiceAPI.resetDefaultProfile(SONAR_URL, language)
	}

	/**
	 * Activates all the rules for a given repository for the specified language/profile.
	 *
	 * @param language
	 * @param profile
	 * @param repository
	 */
	void activateRepositoryRules(String language, String profile, String repository){
		SonarWebServiceAPI.activateRepositoryRules(SONAR_URL, language, profile, repository)
	}
}