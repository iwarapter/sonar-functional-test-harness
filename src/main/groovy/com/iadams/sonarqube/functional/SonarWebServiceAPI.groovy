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

import groovy.util.logging.Slf4j
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method

/**
 * @author iwarapter
 */
@Slf4j
final class SonarWebServiceAPI {

	/**
	 * Queries and asserts the contents of given metrics.
	 *
	 * @param url
	 * @param project
	 * @param metrics_to_query
	 * @throws FunctionalSpecException
	 */
	static void containsMetrics(String url, String project, Map<String, Float> metrics_to_query) throws FunctionalSpecException{

		log.info("Querying the following metrics: $metrics_to_query")

		try {
			def http = new HTTPBuilder(url)
			def resp = http.get(path: '/api/resources', query: [resource: project, metrics: metrics_to_query.keySet().join(',')])

			Map<String, Float> results = [:]
			resp.'msr'[0].each{
				results.put(it.key, it.val)
			}

			assert metrics_to_query.equals(results) : "Expected:\n$metrics_to_query\nReceived:\n$results\n\n" + metrics_to_query.minus(metrics_to_query.intersect(results))
		}
		catch( HttpResponseException e){
			throw new FunctionalSpecException("Cannot query the metrics, details: ${e.message}", e)
		}
	}

	/**
	 * Activates all the rules for a specified repository in a given language profile.
	 *
	 * @param url
	 * @param language
	 * @param profile
	 * @param repository
	 * @throws FunctionalSpecException
	 */
	static void activateRepositoryRules(String url, String language, String profile, String repository) throws FunctionalSpecException{

		log.info("Activate all rules in $language:$profile repository: $repository")

		try {
			String key = profileKey(url, language, profile)
			def http = new HTTPBuilder(url)
			http.request(Method.POST){ req->
				uri.path = '/api/qualityprofiles/activate_rules'
				uri.query = [ profile_key: key , repositories: repository]
				headers.'Authorization' =
						"Basic ${"admin:admin".bytes.encodeBase64().toString()}"
			}
			log.info("All rules in $language:$profile repository: $repository activated.")
		}
		catch( HttpResponseException e){
			throw new FunctionalSpecException("Cannot deactivate all the rules, details: ${e.message}", e)
		}
	}

	/**
	 * Deactivates all the rules in a given language profile.
	 *
	 * @param url
	 * @param language
	 * @param profile
	 */
	static void deactivateAllRules(String url, String language, String profile) throws FunctionalSpecException {
		log.info("Deactivate all rules in profile: $language:$profile")

		try {
			String key = profileKey(url, language, profile)
			def http = new HTTPBuilder(url)
			http.request(Method.POST){ req->
				uri.path = '/api/qualityprofiles/deactivate_rules'
				uri.query = [ profile_key: key ]
				headers.'Authorization' =
						"Basic ${"admin:admin".bytes.encodeBase64().toString()}"
			}
			log.info("All rules in $language:$profile deactivated.")
		}
		catch( HttpResponseException e){
			throw new FunctionalSpecException("Cannot deactivate all the rules, details: ${e.message}", e)
		}
	}

	/**
	 * Restores the default profiles for the given language.
	 *
	 * @param url
	 * @param language
	 * @throws FunctionalSpecException
	 */
	static void resetDefaultProfile(String url, String language) throws FunctionalSpecException {
		log.info("Resetting default profiles for: $language")
		try {
			def http = new HTTPBuilder(url)
			http.request(Method.POST){ req->
				uri.path = '/api/qualityprofiles/restore_built_in'
				uri.query = [ language: language ]
				headers.'Authorization' =
						"Basic ${"admin:admin".bytes.encodeBase64().toString()}"
			}
			log.info("Reset default profiles for: $language")
		}
		catch( HttpResponseException e){
			throw new FunctionalSpecException("Cannot restore built in profile, details: ${e.message}", e)
		}
	}

	/**
	 * Returns the key for the given language / profile.
	 *
	 * @param url
	 * @param language
	 * @param profile
	 * @return The profile key.
	 * @throws FunctionalSpecException
	 */
	static String profileKey(String url, String language, String profile) throws FunctionalSpecException {
		log.info("Finding profile key for $language:$profile")

		def http = new HTTPBuilder(url)
		def resp = http.get(path: '/api/rules/app')
		for(i in resp.qualityprofiles){
			if(i.lang == language && i.name == profile){
				return i.key
			}
		}

		throw new FunctionalSpecException("Unable to find default profile for: $language $profile")
	}
}
