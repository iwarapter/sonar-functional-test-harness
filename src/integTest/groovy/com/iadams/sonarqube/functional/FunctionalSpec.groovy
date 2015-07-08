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

/**
 * @author iwarapter
 */
class FunctionalSpec extends FunctionalSpecBase {

	def "all directories/files are created"(){
		expect:
		projectDir.isDirectory()
		sonarProjectFile.text.contains('all-directories-files-are-created')
	}

	def "we can run sonar-runner as part of a test"(){
		when:
		runSonarRunner()

		then:
		analysisFinishedSuccessfully()

		when:
		runSonarRunnerWithArguments('-X')

		then:
		analysisFinishedSuccessfully()
		analysisLogContains('DEBUG - ')
	}

	def "we can run sonar-runner"(){
		when:
		runSonarRunner()

		then:
		analysisFinishedSuccessfully()
	}

	def "we can break an analysis"(){
		when: 'i add garbage to the sonar project properties file and run an analysis'
		sonarProjectFile << 'sonar.language=KDHFkjadfkjsdf'
		runSonarRunner()

		then:
		analysisFailed()
		analysisLogContains("ERROR: Caused by: You must install a plugin that supports the language 'KDHFkjadfkjsdf'")
	}

	def "we can query projects metrics as part of a test"(){
		given:
		runSonarRunner()

		expect:
		theFollowingProjectMetricsHaveTheFollowingValue([violations:0, sqale_index:0])
	}

	def "we can query a files metrics during a test"(){
		given:
		File helloWorld = new File(projectDir, 'HelloWorld.java')
		helloWorld << """public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello, World");
    }

}"""

		when:
		runSonarRunner()

		then:
		theFollowingFileMetricsHaveTheFollowingValue("HelloWorld.java", [ncloc:5, lines:7])
	}
}