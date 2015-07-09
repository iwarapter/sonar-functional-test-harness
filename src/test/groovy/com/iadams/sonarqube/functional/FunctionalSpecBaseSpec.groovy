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
	}

	def "install plugin"(){
		when:
		spec.SONAR_HOME = new File('').absolutePath
		spec.PLUGIN_DIR = 'build/resources/main/pluginDir'
		spec.JAR_DIR = 'build/resources/main/jarDir'
		spec.PLUGIN_NAME_REGEX = ~/.*sonar-(old)?e?E?xample-plugin-[0-9.]*(-SNAPSHOT)?\.jar/
		spec.installPlugin()

		then:
		noExceptionThrown()
		new File(spec.PLUGIN_DIR, 'sonar-example-plugin-0.1.jar').isFile()
	}
}
