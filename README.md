Sonar Functional Test Harness
=============================
 [![Build Status](https://travis-ci.org/iwarapter/sonar-functional-test-harness.svg?branch=master)](https://travis-ci.org/iwarapter/sonar-functional-test-harness)
 [ ![Download](https://api.bintray.com/packages/iwarapter/sonar-plugins/sonar-functional-test-harness/images/download.svg) ](https://bintray.com/iwarapter/sonar-plugins/sonar-functional-test-harness/_latestVersion)
 
 [![Stories in Ready](https://badge.waffle.io/iwarapter/sonar-functional-test-harness.svg?label=ready&title=Ready)](http://waffle.io/iwarapter/sonar-functional-test-harness)
 [![Stories in Ready](https://badge.waffle.io/iwarapter/sonar-functional-test-harness.svg?label=In Progress&title=In Progress)](http://waffle.io/iwarapter/sonar-functional-test-harness)

Description
-----------
This library is an extension on top on [Spock] to enable easy functional testing of [SonarQube] plugins using a simple specification.

Usage
-----------
- Include the library in the build.

Gradle:
```groovy
testCompile 'com.iadams.sonarqube:sonar-functional-test-harness:0.1.0'
```
Maven:
```xml
<dependency>
    <groupId>com.iadams.sonarqube</groupId>
    <artifactId>sonar-functional-test-harness</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```
- Write some tests!

See the [wiki] for examples!

Running Tests
-------------
The library is capable of starting a SonarQube server is one is not up when the tests are started. To use this you must set the SONAR_HOME environment variable. Otherwise the tests will be ran against the available SonarQube server.

[Spock]:http://spockframework.org/
[SonarQube]:http://www.sonarqube.org/
[wiki]:https://github.com/iwarapter/sonar-functional-test-harness/wiki