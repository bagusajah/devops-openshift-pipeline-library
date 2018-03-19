#!/usr/bin/groovy
import com.ascendmoney.vulcan.Utils
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GLOBAL_VARS = config.global_vars
  def LIST_ENV = config.list_env
  def APP_VERSION = config.version
  def S3_CONFIG_URL = "https://s3-ap-southeast-1.amazonaws.com/acm-aws-openshift-configuration-repo"
  def GIT_HASH_FABRIC8_CONFIGURATION = ""

  dir("/home/jenkins/workspace/${env.JOB_NAME}/s3-pull-config") {
    def env_list = ""
    for(n = 0; n < LIST_ENV.size(); n++){
      env_list = LIST_ENV[n]
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/s3-pull-config/tmp/${env_list}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}"
      sh "cd /home/jenkins/workspace/${env.JOB_NAME}/s3-pull-config/tmp/${env_list}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}"
      sh "pwd"
      sh "curl -Ok ${S3_CONFIG_URL}/${GLOBAL_VARS['COUNTRY_CODE']}/${env_list}/${GLOBAL_VARS['APP_NAME']}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}.zip"
      sh "ls -lt"
      sh "pwd"
      sh "mkdir -p /app-config/${GLOBAL_VARS['COUNTRY_CODE']}/${env_list}/${GLOBAL_VARS['APP_NAME']}"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/s3-pull-config/tmp/${env_list}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}.zip  /app-config/${GLOBAL_VARS['COUNTRY_CODE']}/${env_list}/${GLOBAL_VARS['APP_NAME']}/"
       
    } 
  } 

  return "Sync Configure Server Complete!"

} 