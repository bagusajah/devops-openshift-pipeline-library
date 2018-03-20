#!/usr/bin/groovy
import com.ascendmoney.vulcan.Utils
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def APP_NAME = config.app_name
  def LIST_ENV = config.list_env
  def APP_VERSION = config.version
  def COUNTRY_CODE =  config.country_code
  def DIRECTORY_WORKSPACE = config.directory
  def CONFIG_PATH = config.config_path
  def S3_CONFIG_URL = "https://s3-ap-southeast-1.amazonaws.com/acm-aws-openshift-configuration-repo"
  def GIT_HASH_FABRIC8_CONFIGURATION = ""


  
    def env_list = ""
    for(n = 0; n < LIST_ENV.size(); n++){
      env_list = LIST_ENV[n]
      dir("${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}") {
      sh "curl -Ok ${S3_CONFIG_URL}/${COUNTRY_CODE}/${env_list}/${APP_NAME}/${APP_NAME}-${APP_VERSION}.zip"
      sh "mkdir -p /${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}"
      sh "cp -rf ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}/${APP_NAME}-${APP_VERSION}.zip  /${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}/"
    } 
  } 

  return "Sync Configure Server Complete!"

} 