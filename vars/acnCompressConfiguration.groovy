#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def git_openshift_configuration = config.git_openshift_configuration
  def app_name = config.app_name
  def country_code = config.country_code
  def LIST_ENV = ["dev", "qa", "performance", "staging", "production"]
  def APP_VERSION = config.version
  def directory = config.directory

  def GIT_HASH_OPENSHIFT_CONFIGURATION = ""

  git credentialsId: 'bitbucket-credential', url: git_openshift_configuration  
  GIT_HASH_OPENSHIFT_CONFIGURATION = sh script: "cd ${directory}/update-config && git rev-parse --verify HEAD", returnStdout: true
  GIT_HASH_OPENSHIFT_CONFIGURATION = GIT_HASH_OPENSHIFT_CONFIGURATION.trim()

  def env_list = ""
  for(n = 0; n < LIST_ENV.size(); n++){
    env_list = LIST_ENV[n]
    dir("${directory}/update-config/tmp/${env_list}/${app_name}-${APP_VERSION}"){
      sh "cp -rf ${directory}/update-config/${country_code}/${env_list}/${app_name}/* ${directory}/update-config/tmp/${env_list}/${app_name}-${APP_VERSION}/"
      sh "cd ${directory}/update-config/tmp/${env_list} && /bin/tar -zcvf \"${app_name}-${APP_VERSION}.tar.gz\" \"${app_name}-${APP_VERSION}/\""
      dir("${directory}/update-config/tmp/${env_list}"){
        withAWS(credentials:'openshift-s3-credential') {
          s3Upload bucket: 'acm-aws-openshift-configuration-repo', file: "${app_name}-${APP_VERSION}.tar.gz", path: "${country_code}/${env_list}/${app_name}/${app_name}-${APP_VERSION}.tar.gz"
        }
      } // End Upload zip file to s3
    } // End scope
  } // End Loop zip file and upload to s3

  return GIT_HASH_OPENSHIFT_CONFIGURATION

} // End Compress Configuration