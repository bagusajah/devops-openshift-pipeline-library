#!/usr/bin/groovy
import com.ascendmoney.vulcan.Utils

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    def appName = config.APPNAME
    def newVersion = config.VERSION
    def pushArtifactToNexus = config.pushArtifactToNexus
    
    def command_maven = "package"
    if ( pushArtifactToNexus == "true" ) {
        command_maven = "deploy"
    } // End Condition command mvn for push to nexus

    def skipTests = config.skipTests ?: false

    // echo "=============== TEMP : WAITING SONARQUBE SERVER ==============="
    // sh "mvn versions:set -DnewVersion=${newVersion}"
    // sh "mvn clean ${command_maven}"

    // echo "=============== DONT FORGET ENABLE SONARQUBE AFTER SONARQUBE SERVER IS ACTIVE ==============="
    withSonarQubeEnv('sonarqube') {
        sh "mvn versions:set -DnewVersion=${newVersion}"
        sh 'mvn clean ${command_maven} sonar:sonar ' +
        "-Dsonar.host.url=${config.SONARQUBE_URL} " +
        "-Dsonar.projectKey=${appName} " +
        "-Dsonar.projectName=${appName} " +
        "-Dsonar.projectVersion=${newVersion} " +
        '-Dsonar.language=java ' +
        '-Dsonar.sources=src/ '+
        '-Dsonar.tests=src/test/ '+
        '-Dsonar.test.inclusions=**/*Test*/** '+
        '-Dsonar.exclusions=**/*Test*/**'+
        '-Dsonar.java.binaries=target/classes '
    }
  } // End Function
