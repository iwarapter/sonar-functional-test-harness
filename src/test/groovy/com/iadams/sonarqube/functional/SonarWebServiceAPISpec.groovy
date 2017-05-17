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

import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.slf4j.Logger
import spock.lang.Specification

class SonarWebServiceAPISpec extends Specification {

  MockWebServer server
  SonarWebServiceAPI api

  def setup() {
    server = new MockWebServer()
  }

  def cleanup() {
    server.shutdown()
  }

  def "we can get a OK response"() {
    given:
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())

    expect:
    api.getResponseCode() == 200
  }

  def "we can handle a malformed url"() {
    given:
    server.start()
    api = new SonarWebServiceAPI('wrong')

    expect:
    api.getResponseCode() == 400
  }

  def "we can request against a not found endpoint"() {
    given:
    server.start()
    api = new SonarWebServiceAPI('http://localhost:12345')

    expect:
    api.getResponseCode() == 404
  }

  def "we can delete a project"() {
    given:
    server.enqueue(new MockResponse().setResponseCode(204))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.deleteProject('my_proj')

    then:
    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS)

    request.getHeader('Authorization').equals('Basic YWRtaW46YWRtaW4=')
    request.path.contains('api/projects/delete?key=my_proj')
    1 * api.log.info('Attempting to delete project: my_proj')
    1 * api.log.info('Successfully deleted project: my_proj')
  }

  def "if we try and delete a non-existant project"() {
    given:
    server.enqueue(new MockResponse().setResponseCode(404))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.deleteProject('my_proj')

    then:
    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS)

    request.getHeader('Authorization').equals('Basic YWRtaW46YWRtaW4=')
    request.path.contains('api/projects/delete?key=my_proj')
    1 * api.log.info('Attempting to delete project: my_proj')
    1 * api.log.info('Project not found to delete: my_proj')
  }

  def "we can handle other error response codes"() {
    given:
    server.enqueue(new MockResponse().setResponseCode(401))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.deleteProject('my_proj')

    then:
    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS)

    request.getHeader('Authorization').equals('Basic YWRtaW46YWRtaW4=')
    request.path.contains('api/projects/delete?key=my_proj')
    1 * api.log.info('Attempting to delete project: my_proj')
    1 * api.log.info('Unable to delete project: my_proj', 401)
  }

  def "we can return the correct profile key"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/quality-profiles.json').text))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    String key = api.profileKey('java', 'Sonar way')

    then:
    key.equals('java-sonar-way-40787')
  }

  def "we handle errors when requesting profile key"() {
    given:
    server.enqueue(new MockResponse().setBody('{}'))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())

    when:
    api.profileKey('java', 'Sonar way')

    then:
    thrown(FunctionalSpecException)
  }

  def "we can reset the default profile"() {
    given:
    server.enqueue(new MockResponse().setResponseCode(204))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.resetDefaultProfile('Java')

    then:
    noExceptionThrown()
    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS)
    request.getHeader('Authorization').equals('Basic YWRtaW46YWRtaW4=')
    request.path.contains('api/qualityprofiles/restore_built_in?language=Java')
  }

  def "handle errors when resetting the default profile"() {
    given:
    server.enqueue(new MockResponse().setResponseCode(401))
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.resetDefaultProfile('Java')

    then:
    def e = thrown(FunctionalSpecException)
    e.message.equals('Unable to reset default profile: Client Error')
    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS)
    request.getHeader('Authorization').equals('Basic YWRtaW46YWRtaW4=')
    request.path.contains('api/qualityprofiles/restore_built_in?language=Java')
  }

  def "we can deactivate all rules"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/quality-profiles.json').text))
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.deactivateAllRules('java', 'Sonar way')

    then:
    noExceptionThrown()
    RecordedRequest request1 = server.takeRequest(10, TimeUnit.SECONDS)
    request1.path.contains('/api/qualityprofiles/search?format=json')

    RecordedRequest request2 = server.takeRequest(10, TimeUnit.SECONDS)
    request2.path.contains('api/qualityprofiles/deactivate_rules?profile_key=java-sonar-way-40787')
  }

  def "we can deactivate a rule"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/quality-profiles.json').text))
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.deactivateRule('squid:StringEqualityComparisonCheck', 'java', 'Sonar way')

    then:
    noExceptionThrown()
    1 * api.log.info('Finding profile key for java:Sonar way')
    1 * api.log.info('Deactivate rule squid:StringEqualityComparisonCheck in java:Sonar way')
    1 * api.log.info('Rule squid:StringEqualityComparisonCheck in java:Sonar way deactivated.')
  }

  def "we can activate a rule"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/quality-profiles.json').text))
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.activateRule('squid:StringEqualityComparisonCheck', 'java', 'Sonar way')

    then:
    noExceptionThrown()
    1 * api.log.info('Finding profile key for java:Sonar way')
    1 * api.log.info('Activate rule squid:StringEqualityComparisonCheck in java:Sonar way')
    1 * api.log.info('Rule squid:StringEqualityComparisonCheck in java:Sonar way activated.')
  }

  def "we can activate all rules"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/quality-profiles.json').text))
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.activateRepositoryRules('java', 'Sonar way')

    then:
    noExceptionThrown()
    1 * api.log.info('Finding profile key for java:Sonar way')
    1 * api.log.info('Activate all rules in java:Sonar way')
    1 * api.log.info('All rules in java:Sonar way activated.')
  }

  def "we can query a component"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/component.json').text))
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.containsMetrics('puppetlabs-apache', [violations:174,lines:5563])

    then:
    noExceptionThrown()
  }


  def "we can assert a component has incorrect measures"() {
    given:
    server.enqueue(new MockResponse().setBody(new File('src/test/resources/responses/component.json').text))
    server.enqueue(new MockResponse())
    server.start()
    api = new SonarWebServiceAPI(server.url('').toString())
    api.log = Mock(Logger)

    when:
    api.containsMetrics('puppetlabs-apache', [violations:174,lines:5562])

    then:
    thrown(AssertionError)
    1 * api.log.info('Querying the following metrics: [violations:174, lines:5562]')
  }
}
