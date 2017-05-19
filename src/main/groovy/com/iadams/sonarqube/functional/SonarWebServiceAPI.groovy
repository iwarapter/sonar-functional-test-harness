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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iadams.sonarqube.functional.beans.ComponentResponse
import com.iadams.sonarqube.functional.beans.QualityProfile
import com.iadams.sonarqube.functional.beans.SearchQualityProfileResponse
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

class SonarWebServiceAPI {

  Logger log = LoggerFactory.getLogger(SonarWebServiceAPI.class);

  OkHttpClient client
  String url = 'http://localhost:9000/'

  String sonarUser = 'admin'
  String sonarPasswd = 'admin'


  SonarWebServiceAPI() {
    this.client = new OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  SonarWebServiceAPI(String url) {
    this.url = url
    this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
  }

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8")
  /**
   * Returns the response code for a given URL.
   * @return
   */
  int getResponseCode() {

    try {
      Response response = client.newCall(new Request.Builder().url(url).build()).execute()

      return response.code()
    }
    catch (IOException e) {
      log.debug("Unable to GET from url: $url", e)
      return 404
    }
    catch (IllegalArgumentException e) {
      log.debug("Malformed url: $url", e)
      return 400
    }
  }

  /**
   * Queries and asserts the contents of given metrics.
   *
   * @param url
   * @param project
   * @param metrics_to_query
   * @throws FunctionalSpecException
   */
  void containsMetrics(String component, Map<String, Float> metrics_to_query) throws FunctionalSpecException {
    log.info("Querying the following metrics: $metrics_to_query")

    HttpUrl httpUrl = HttpUrl.parse(url).newBuilder(url + 'api/measures/component')
      .addQueryParameter('metricKeys', metrics_to_query.keySet().join(','))
      .addQueryParameter('componentKey', component)
      .build()

    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .build()

    try {
      Response response = client.newCall(request).execute()

      if (response.code() == 200) {
        String body = response.body().string()
        ComponentResponse comp = new Gson().fromJson(body, new TypeToken<ComponentResponse>() {
        }.getType())

        Map<String, Float> results = [:]
        comp.component.measures.each {
          results.put(it.metric, it.value.toInteger())
        }
        assert metrics_to_query.equals(results): "Expected:\n$metrics_to_query\nReceived:\n$results\n\n" + metrics_to_query.minus(metrics_to_query.intersect(results))
        return
      }

      throw new FunctionalSpecException("Cannot query the metrics, details: ${response.message()}")
    }
    catch (IOException e) {
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
  void activateRepositoryRules(String language, String profile) throws FunctionalSpecException {

    log.info("Activate all rules in $language:$profile")

    String key = profileKey(language, profile)

    String credential = Credentials.basic(sonarUser, sonarPasswd)

    HttpUrl httpUrl = HttpUrl.parse(url).newBuilder(url + 'api/qualityprofiles/activate_rules')
      .addQueryParameter('profile_key', key)
      .build()
    log.debug(httpUrl.toString())

    RequestBody body = RequestBody.create(JSON, '')
    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .header("Authorization", credential)
      .post(body)
      .build()

    try {
      Response response = client.newCall(request).execute()

      if (response.code() == 200) {
        log.info("All rules in $language:$profile activated.")
        return
      }

      throw new FunctionalSpecException("Cannot deactivate all the rules, details: ${response.message()}")
    }
    catch (IOException e) {
      throw new FunctionalSpecException("Cannot deactivate all the rules, details: ${e.message}", e)
    }
  }

  /**
   * Activate a specific rule in a given language profile.
   *
   * @param rule
   * @param language
   * @param profile
   * @throws FunctionalSpecException
   */
  void activateRule(String rule, String language, String profile) throws FunctionalSpecException {
    log.info("Activate rule $rule in $language:$profile")

    String key = profileKey(language, profile)

    String credential = Credentials.basic(sonarUser, sonarPasswd)

    HttpUrl httpUrl = HttpUrl.parse(url).newBuilder(url + 'api/qualityprofiles/activate_rule')
      .addQueryParameter('profile_key', key)
      .addQueryParameter('rule_key', rule)
      .build()

    RequestBody body = RequestBody.create(JSON, '')
    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .header("Authorization", credential)
      .post(body)
      .build()

    try {
      Response response = client.newCall(request).execute()

      if (response.code() == 200) {
        log.info("Rule $rule in $language:$profile activated.")
        return
      }

      throw new FunctionalSpecException("Cannot activate the rule: $rule, details:${response.message()}")
    }
    catch (IOException e) {
      throw new FunctionalSpecException("Cannot activate the rule: $rule, details: ${e.message}", e)
    }
  }

  /**
   * Deactivate a specific rule in a given language profile.
   *
   * @param rule
   * @param language
   * @param profile
   * @throws FunctionalSpecException
   */
  void deactivateRule(String rule, String language, String profile) throws FunctionalSpecException {
    log.info("Deactivate rule $rule in $language:$profile")

    String key = profileKey(language, profile)

    String credential = Credentials.basic(sonarUser, sonarPasswd)

    HttpUrl httpUrl = HttpUrl.parse(url).newBuilder(url + 'api/qualityprofiles/deactivate_rule')
      .addQueryParameter('profile_key', key)
      .addQueryParameter('rule_key', rule)
      .build()

    RequestBody body = RequestBody.create(JSON, '')
    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .header("Authorization", credential)
      .post(body)
      .build()

    try {
      Response response = client.newCall(request).execute()

      if (response.code() == 200) {
        log.info("Rule $rule in $language:$profile deactivated.")
        return
      }

      throw new FunctionalSpecException("Cannot deactivate the rule: $rule, details:${response.message()}")
    }
    catch (IOException e) {
      throw new FunctionalSpecException("Cannot deactivate the rule: $rule, details: ${e.message}", e)
    }
  }

  /**
   * Deactivates all the rules in a given language profile.
   *
   * @param url
   * @param language
   * @param profile
   */
  void deactivateAllRules(String language, String profile) throws FunctionalSpecException {
    log.info("Deactivate all rules in profile: $language:$profile")

    String key = profileKey(language, profile)

    String credential = Credentials.basic(sonarUser, sonarPasswd)

    HttpUrl httpUrl = HttpUrl.parse(url)
      .newBuilder(url + 'api/qualityprofiles/deactivate_rules')
      .addQueryParameter('profile_key', key)
      .build()
    log.debug(httpUrl.toString())

    RequestBody body = RequestBody.create(JSON, '')
    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .header("Authorization", credential)
      .post(body)
      .build()

    try {
      Response response = client.newCall(request).execute()

      if (response.code() == 200) {
        log.info("All rules in $language:$profile deactivated.")
        return
      }

      throw new FunctionalSpecException("Unable to reset default profile: ${response.message}")
    }
    catch (IOException e) {
      throw new FunctionalSpecException("Cannot restore built in profile, details: ${e.message}", e)
    }
  }

  /**
   * Restores the default profiles for the given language.
   *
   * @param url
   * @param language
   * @throws FunctionalSpecException
   */
  void resetDefaultProfile(String language) throws FunctionalSpecException {

    log.info("Resetting default profiles for: $language")

    String credential = Credentials.basic(sonarUser, sonarPasswd)

    HttpUrl httpUrl = HttpUrl.parse(url).newBuilder(url + 'api/qualityprofiles/restore_built_in').addQueryParameter('language', language).build()

    RequestBody body = RequestBody.create(JSON, '')
    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .header("Authorization", credential)
      .post(body)
      .build()

    try {
      Response response = client.newCall(request).execute()

      if (response.code() == 204) {
        log.info("Reset default profiles for: $language")
        return
      }

      throw new FunctionalSpecException("Unable to reset default profile: ${response.message}")
    }
    catch (IOException e) {
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
  String profileKey(String language, String profile) throws FunctionalSpecException {
    log.info("Finding profile key for $language:$profile")

    HttpUrl httpUrl = HttpUrl.parse(url)
      .newBuilder(url + 'api/qualityprofiles/search')
      .addQueryParameter('format', 'json')
      .build()
    log.debug(httpUrl.toString())

    try {
      Response response = client.newCall(new Request.Builder().get().url(httpUrl).build()).execute()
      SearchQualityProfileResponse qualityProfileResponse = new Gson().fromJson(response.body().string(),SearchQualityProfileResponse)

      for (QualityProfile p : qualityProfileResponse.profiles) {
        if (p.name.equals(profile) && p.language.equals(language)) {
          return p.key
        }
      }
      throw new FunctionalSpecException("Unable to find default profile for: $language $profile")
    }
    catch (IOException e) {
      throw new FunctionalSpecException("Unable to find default profile for: $language $profile")
    }
  }

  /**
   * Attempts to delete a project, returns the statusCode.
   *
   * @param project
   * @return
   */
  void deleteProject(String project) {

    log.info "Attempting to delete project: $project"

    String credential = Credentials.basic(sonarUser, sonarPasswd)

    HttpUrl httpUrl = HttpUrl.parse(url).newBuilder(url + 'api/projects/delete').addQueryParameter('key', project).build()

    RequestBody body = RequestBody.create(JSON, '')
    Request request = new Request.Builder()
      .url(httpUrl.toString())
      .header("Authorization", credential)
      .post(body)
      .build()

    try {
      Response response = client.newCall(request).execute()

      switch (response.code()) {
        case 404:
          log.info("Project not found to delete: $project")
          break
        case 204:
          log.info("Successfully deleted project: $project")
          break
        default:
          log.info("Unable to delete project: $project", response.code())
      }
    }
    catch (IOException e) {
      log.warn("Unable to delete project: $project", e)
    }
  }
}
