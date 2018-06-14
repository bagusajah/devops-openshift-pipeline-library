#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    def appName = config.APPNAME
    def newVersion = config.VERSION
    def sonarqubeUrl = config.SONARQUBE_URL
    def scannerHome = tool 'sonarqube-scanner';

    withSonarQubeEnv('sonarqube') {
      sh "${scannerHome}/bin/sonar-scanner " +
      "-Dsonar.host.url=${sonarqubeUrl} " +
      "-Dsonar.projectKey=${appName} " +
      "-Dsonar.projectName=${appName} " +
      "-Dsonar.projectVersion=${newVersion} " +
      "-Dsonar.language=py " +
      "-Dsonar.sources=. "
      "-Dsonar.tests=**/tests/** "+
      "-Dsonar.exclusions=**/tests/**"
    }
    
} // End Function
