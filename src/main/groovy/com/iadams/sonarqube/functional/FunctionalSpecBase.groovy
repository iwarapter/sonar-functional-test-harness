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

	@TempDirectory(clean=false) protected File projectDir

	protected String moduleName
	protected File sonarProjectFile

	protected static boolean didSonarStart

	protected static String SONAR_URL = "http://localhost:9000"
	protected static String JAR_DIR = 'build/libs/'
	protected static String PLUGIN_DIR = 'extensions/plugins/'
	protected static String PLUGIN_NAME_REGEX = ''
	protected static String SONAR_HOME = ''

	protected static SonarRunnerResult sonarRunnerResult
	protected static File analysisLogFile

	private static final String SONAR_ERROR = ".* ERROR .*"
	private static final String SONAR_WARN = ".* WARN .*"
	private static final String SONAR_WARN_TO_IGNORE_RE = ".*H2 database should.*|.*Starting search|.*Starting web|.*WEB DEVELOPMENT MODE IS ENABLED.*"

	/**
	 * The setup method is run before each test and will provide a working directory
	 * with a sonar-project.properties file configured with sensible defaults.
	 *
	 * @return
	 */
	def setup() {
		moduleName = findModuleName()
		sonarRunnerResult = null
		analysisLogFile = null

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

		if(isWebuiUp()){
			log.info "SonarQube is already running."
		}
		else {
			log.info "SonarQube is not running."
			SONAR_HOME = System.getenv('SONAR_HOME')
			log.info "SONAR_HOME: $SONAR_HOME"
			if (SONAR_HOME) {
				if (isInstalled()) {
					cleanServerLog()
					installPlugin()
					assert startSonar() : "Cannot start SonarQube from $SONAR_HOME exiting."
					didSonarStart = true
					checkServerLogs()
				} else {
					throw new FunctionalSpecException("The folder $SONAR_HOME does not exist.")
				}
			} else {
				throw new FunctionalSpecException("The environment variable SONAR_HOME is null and the webui is not available.")
			}
		}
	}

	def cleanupSpec() {
		if (didSonarStart) {
			stopSonar()
		}
	}

	private String findModuleName() {
		projectDir.getName().replaceAll(/_\d+/, '')
	}

	
	//
	// SERVER SETUP START
	//


	/**
	 * Checks if the sonarHome exists.
	 *
	 * @return
	 */
	boolean isInstalled(){
		log.debug("Check directory exists: $SONAR_HOME")
		return new File(SONAR_HOME).exists()
	}

	/**
	 * Return file handle to the sonar server log.
	 * @return
	 */
	File sonarLog(){
		new File(SONAR_HOME, 'logs/sonar.log')
	}

	/**
	 * Deletes the server log for the given path.
	 *
	 * @param sonarhome
	 */
	void cleanServerLog(){
		sonarLog().delete()
	}

	/**
	 * Copies a plugin from the {@link #JAR_DIR} to the {@link #PLUGIN_DIR}
	 *
	 * @param sonarhome
	 */
	void installPlugin(){
		File jarDir = new File( JAR_DIR)
		File[] jarFiles = findFiles(jarDir, PLUGIN_NAME_REGEX)

		if(jarFiles != null || jarFiles.size()){

			log.info("Installing plugin")
			File pluginDir = new File(SONAR_HOME, PLUGIN_DIR)

			for(File f : findFiles(pluginDir, PLUGIN_NAME_REGEX)){
				log.info("Removing: " + f.getAbsoluteFile())
				f.delete()
			}

			for(File f : jarFiles) {
				log.info("Copying ${f.name} to $pluginDir")
				FileUtils.copyFileToDirectory(f, pluginDir)
			}
		}
		else {
			log.warn("No plugin detected to install")
		}
	}

	/**
	 * Helper method to return all files matching a given regex pattern.
	 *
	 * @param directory
	 * @param regex
	 * @return
	 */
	File[] findFiles(File directory, String regex){
		directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(regex)
			}
		})
	}

	/**
	 * Returns the fully qualified command to start script.
	 *
	 * @return
	 */
	String startScript(){
		return SONAR_HOME + "/" + scriptPath() + " start";
	}

	/**
	 * Returns the fully qualified command to stop script.
	 *
	 * @return
	 */
	String stopScript(){
		return SONAR_HOME + "/" + scriptPath() + " stop";
	}

	/**
	 * Based on system property attempts to work out the correct scipt path.
	 *
	 * @return
	 */
	String scriptPath(){
		//Just mac/linux atm
		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");

		if( os.equals("Linux") && arch.equals("x86_64") ){
			return "bin/linux-x86-64/sonar.sh";
		}
		else if( os.equals("Mac OS X") && arch.equals("x86_64") ){
			return "bin/macosx-universal-64/sonar.sh";
		}
		return "bin/linux-x86-32/sonar.sh";
	}

	/**
	 * Executes the {@link #startScript} method to run sonar and asserts the command exits 0.
	 * It then waits for the sonar webui to respond by calling {@link #waitForSonar}.
	 *
	 * @param sonarHome
	 */
	boolean startSonar(){
		log.info "Starting SonarQube\n ${startScript()}"
		def cmd = startScript().execute()
		cmd.waitFor()
		cmd.exitValue() == 0
		waitForSonar(60)
	}


	/**
	 * Executes the {@link #stopScript} method to run sonar and asserts the command exits 0.
	 * It then waits for the sonar webui to stop responding by calling {@link #waitForSonarDown}.
	 *
	 * @param sonarHome
	 */
	boolean stopSonar(){
		log.info "Stopping SonarQube\n ${startScript()}"
		def cmd = stopScript().execute()
		cmd.waitFor()
		cmd.exitValue() == 0
		waitForSonarDown(300)
	}

	/**
	 * Polls the sonar webui to see if it is responding or it times out.
	 *
	 * @param timeout
	 * @return
	 */
	boolean waitForSonar(int timeout){
		for (i in 0..timeout){
			if(isWebuiUp()){
				return true
			}
			sleep(1000)
		}
		return false
	}

	/**
	 * Polls the sonar webui to see if it has stopped responding or it times out.
	 *
	 * @param timeout
	 * @return
	 */
	boolean waitForSonarDown(int timeout){
		for (i in timeout){
			if(isWebuiDown()){
				return true
			}
			sleep(1000)
		}
		return false
	}

	/**
	 * Asserts the {@link #sonarLog} has no warning or errors.
	 * @return
	 */
	boolean checkServerLogs(){
		LogAnalysisResult result = analyseLog(sonarLog())
		assert result.badlines.size() == 0 : ("Found following errors and/or warnings lines in the logfile:\n"
				+ result.badlines.join("\n")
				+ "For details see ${sonarLog()}")
	}

	//
	// SERVER SETUP FINISH
	//



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
			throw new FunctionalSpecException("Could not find classpath resource: $srcDir")
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
		analysisLogFile = new File(projectDir, "$moduleName-analysis.log")
		analysisLogFile.write(sonarRunnerResult.output + sonarRunnerResult.error)
	}

	/**
	 * Runs sonar-runner with no arguments and updates the test {@link SonarRunnerResult}
	 */
	void runSonarRunnerWithArguments(String args){
		sonarRunnerResult = SonarRunnerHelper.runSonarRunner(args, projectDir)
		analysisLogFile = new File(projectDir, "$moduleName-analysis.log")
		analysisLogFile.write(sonarRunnerResult.output + sonarRunnerResult.error)
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
	 * Checks the analysisLogFile to see if it contains the given line.
	 *
	 * @param line
	 * @return True/False
	 */
	boolean analysisLogContains(String line){
		for(String s : analysisLogFile.readLines()){
			if(s.matches(line)){ return true }
		}
		false
	}

	/**
	 * Asserts if the analysis log contains warnings or errors.
	 */
	void analysisLogDoesNotContainErrorsOrWarnings(){
		LogAnalysisResult result = analyseLog(analysisLogFile)
		assert result.badlines.size() == 0 : ("Found following errors and/or warnings lines in the logfile:\n"
				+ result.badlines.join("\n")
				+ "For details see $analysisLogFile")
	}

	/**
	 * Asserts that the analysis log contains NO warnings OR errors.
	 */
	void analysisLogContainsErrorsOrWarnings() {
		LogAnalysisResult result = analyseLog(analysisLogFile)
		assert result.badlines.size() != 0: ("Found zero instances of a warning or error.")
	}

	/**
	 * Helper method to check all the lines of a given log file and update
	 * @return
	 */
	LogAnalysisResult analyseLog(File logFile){
		LogAnalysisResult result = new LogAnalysisResult()
		logFile.eachLine {
			if(isSonarError(it)){
				result.errors++
				result.badlines.add(it)
			}

			if(isSonarWarning(it)){
				result.warnings++
				result.badlines.add(it)
			}
		}
		result
	}

	/**
	 * Matches if the given lines contains the defined error regex
	 *
	 * @param line
	 * @return
	 */
	boolean isSonarError(String line){
		return line.matches(SONAR_ERROR)
	}

	/**
	 * Matches if the given lines contains the defined warning regex and excludes certain warnings.
	 *
	 * @param line
	 * @return
	 */
	boolean isSonarWarning(String line){
		return line.matches(SONAR_WARN) && !line.matches(SONAR_WARN_TO_IGNORE_RE)
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

	/**
	 * Checks if the webui for the given URL gives a response code 200
	 *
	 * @return
	 */
	boolean isWebuiUp(){
		try {
			SonarWebServiceAPI.getResponseCode(SONAR_URL) == 200
		} catch (ConnectException e){ false }
	}

	/**
	 * Checks if the webui for the given URL gives a response not equal to 200
	 *
	 * @return
	 */
	boolean isWebuiDown(){
		try {
			return SonarWebServiceAPI.getResponseCode(SONAR_URL) != 200
		}
		catch (ConnectException e){ true }
	}
}