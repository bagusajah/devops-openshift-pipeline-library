package com.ascendmoney.vulcan

import com.cloudbees.groovy.cps.NonCPS

def sayHello() {
    return "Hi, I am vulcan"
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

def getCloudConfig() {
    def openshiftCloudConfig = Jenkins.getInstance().getCloud('openshift')
    return (openshiftCloudConfig) ? 'openshift' : 'kubernetes'
}