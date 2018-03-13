#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('maven')
    def label = parameters.get('label', defaultLabel)

    mavenTemplate(parameters) {
        node(label) {
            body()
        }
    }
}

def buildId (prefix){
    def repo = getRepoName()
    sh "echo repo ${repo}"
    return "${prefix}${repo}_${env.BUILD_NUMBER}".replaceAll('-', '_').replaceAll('/', '_').replaceAll(' ', '_')
}

def getRepoName(){

  def jobName = env.JOB_NAME

  // job name from the org plugin
  if (jobName.count('/') > 1){
    return jobName.substring(jobName.indexOf('/')+1, jobName.lastIndexOf('/'))
  }
  // job name from the branch plugin
  if (jobName.count('/') > 0){
    return jobName.substring(0, jobName.lastIndexOf('/'))
  }
  // normal job name
  return jobName
}