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

/**
 * @author iwarapter
 */
class SonarWebServiceAPIIntegSpec extends Specification {

	SonarWebServiceAPI sonarAPI = new SonarWebServiceAPI()

	def "DeactivateAllRules"() {
		expect:
		sonarAPI.deactivateAllRules('java','Sonar way')
	}

	def "ActivateRepositoryRules"() {
		expect:
		sonarAPI.activateRepositoryRules('java','Sonar way', 'common-java')
	}

	def "ResetDefaultProfile"() {
		expect:
		sonarAPI.resetDefaultProfile('java')
	}

	def "cannot find profile key"(){
		when:
		sonarAPI.profileKey('monkey', 'wrench')

		then:
		thrown( FunctionalSpecException )
	}

	def "we can post to the project delete web service"(){
		when:
		sonarAPI.deleteProject('we-can-query-files-projects-as-part-of-a-test')

		then:
		noExceptionThrown()
	}
}
