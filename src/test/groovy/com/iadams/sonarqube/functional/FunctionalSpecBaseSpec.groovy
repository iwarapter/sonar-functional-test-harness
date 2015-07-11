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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author iwarapter
 */
class FunctionalSpecBaseSpec extends Specification {

	FunctionalSpecBase spec

	def setup(){
		spec = new FunctionalSpecBase()
		spec.SONAR_HOME = '/path/to/sonar'
	}

	def "StartScript"() {
		given:
		System.setProperty("os.name", "Mac OS X")
		System.setProperty("os.arch", "x86_64")

		expect:
		spec.startScript() == '/path/to/sonar/bin/macosx-universal-64/sonar.sh start'
	}

	def "StopScript"() {
		given:
		System.setProperty("os.name", "Mac OS X")
		System.setProperty("os.arch", "x86_64")

		expect:
		spec.stopScript() == '/path/to/sonar/bin/macosx-universal-64/sonar.sh stop'
	}

	@Unroll
	def "ScriptPath #os #arch"() {
		given:
		System.setProperty("os.name", os)
		System.setProperty("os.arch", arch)

		expect:
		spec.scriptPath() == output

		where:
		os	 		| arch 		| output
		"Mac OS X"	| "x86_64"	| 'bin/macosx-universal-64/sonar.sh'
		"Linux"		| "x86_64"	| 'bin/linux-x86-64/sonar.sh'
		"Mac OS X"	| "x86"		| 'bin/linux-x86-32/sonar.sh'
		"Linux"		| "x86"		| 'bin/linux-x86-32/sonar.sh'
		"Linux"		| "amd64"	| 'bin/linux-x86-64/sonar.sh'
	}

	def "isWebuiUp"(){
		given:
		SonarWebServiceAPI sonarAPI = Mock()
		spec.sonarAPI = sonarAPI

		when:
		1 * sonarAPI.getResponseCode() >> 200

		then:
		spec.isWebuiUp()

		when:
		sonarAPI.getResponseCode() >> 404

		then:
		!spec.isWebuiUp()
	}


	def "isWebuiDown"(){
		given:
		SonarWebServiceAPI sonarAPI = Mock()
		spec.sonarAPI = sonarAPI

		when:
		1 * sonarAPI.getResponseCode() >> 404

		then:
		spec.isWebuiDown()

		when:
		sonarAPI.getResponseCode() >> 200

		then:
		!spec.isWebuiDown()
	}

	@Unroll
	def "wait for sonar up"(){
		given:
		SonarWebServiceAPI sonarAPI = Mock()
		spec.sonarAPI = sonarAPI

		sonarAPI.getResponseCode() >> responseCode

		expect:
		spec.waitForSonar(timeout) == result

		where:
		result  | timeout 	| responseCode
		true	| 5			| 200
		false	| 5			| 404
	}

	@Unroll
	def "wait for sonar down"(){
		given:
		SonarWebServiceAPI sonarAPI = Mock()
		spec.sonarAPI = sonarAPI

		sonarAPI.getResponseCode() >> responseCode

		expect:
		spec.waitForSonarDown(timeout) == result

		where:
		result  | timeout 	| responseCode
		true	| 5			| 404
		false	| 5			| 200
	}

	def "no plugin is installed when cant find one"(){
		when:
		spec.SONAR_HOME = new File('').absolutePath
		spec.PLUGIN_DIR = 'build/resources/test/pluginDir'
		spec.JAR_DIR = 'build/resources/test/jarDir'
		spec.PLUGIN_NAME_REGEX = ~/wrong/
		spec.installPlugin()

		then:
		noExceptionThrown()
		!new File(spec.PLUGIN_DIR, 'sonar-example-plugin-0.1.jar').isFile()
	}

	def "install plugin"(){
		when:
		spec.SONAR_HOME = new File('').absolutePath
		spec.PLUGIN_DIR = 'build/resources/test/pluginDir'
		spec.JAR_DIR = 'build/resources/test/jarDir'
		spec.PLUGIN_NAME_REGEX = ~/.*sonar-(old)?e?E?xample-plugin-[0-9.]*(-SNAPSHOT)?\.jar/
		spec.installPlugin()

		then:
		noExceptionThrown()
		new File(spec.PLUGIN_DIR, 'sonar-example-plugin-0.1.jar').isFile()
	}

	def "check server log passes a good log"(){
		when:
		def mockFile = new File(getClass().getResource('/logs/good.log').toURI())
		FunctionalSpecBase spec2 = Spy(FunctionalSpecBase){ sonarLog() >> mockFile  }

		then:
		spec2.checkServerLogs()
		noExceptionThrown()
	}

	def "check server log fails a bad log"(){
		when:
		def mockFile = new File(getClass().getResource('/logs/bad.log').toURI())
		FunctionalSpecBase spec2 = Spy(FunctionalSpecBase){ sonarLog() >> mockFile  }
		spec2.checkServerLogs()

		then:
		thrown(AssertionError)
	}
}
