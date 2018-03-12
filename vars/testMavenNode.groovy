#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    //def defaultLabel = buildId('maven')
    //def label = parameters.get('label', defaultLabel)

    node('maven') {
      stage('Build a Maven project') {
         git 'https://github.com/jenkinsci/kubernetes-plugin.git'
         container('maven') {
           sh 'mvn -B clean package'  
         }
      }
    }
}
