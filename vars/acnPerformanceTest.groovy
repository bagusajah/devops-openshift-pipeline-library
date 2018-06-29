#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import groovy.json.JsonSlurperClassic

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def git_performance_repo_url = config.git_performance_repo_url
  def tmt_test_result_url_performance = config.tmt_test_result_url_performance
  def tmt_url = config.tmt_url
  def app_name = config.app_name
  def authorizationTMTId = config.authorizationTMTId
  def jobTMTId = config.jobTMTId
  def directory = config.directory

  dir("${directory}/performance_test") {
    git credentialsId: 'bitbucket-credential', url: git_performance_repo_url
  }
  sh "chmod +x ${directory}/performance_test/${app_name}/scripts/pipeline_integrated/run.sh"
  sh "cd ${directory}/performance_test/${app_name}/scripts/pipeline_integrated && ./run.sh"
  sh "cd ${directory}/performance_test/${app_name}/scripts/pipeline_integrated && /bin/zip -r \"results.zip\" \"results\""
  dir("${directory}/performance_test/${app_name}/scripts/pipeline_integrated"){
    withAWS(credentials:'openshift-s3-credential') {
      s3Upload bucket: tmt_test_result_url_performance, file: "results.zip", path: "performance-result/${app_name}/${env.BUILD_NUMBER}/results.zip"
    }
  } // End Upload results to s3
  sh "echo BUCKET S3 result Performance Test is https://s3.console.aws.amazon.com/s3/buckets/${tmt_test_result_url_performance}/performance-result/${app_name}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
  sh "curl -k -H \"Authorization: ${authorizationTMTId}\" ${tmt_url}/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
} // End Performance Test