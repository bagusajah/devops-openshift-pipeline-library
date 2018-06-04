#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
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
  def runway_name = config.runway_name
  
  def env_list = ""
  for(n = 0; n < LIST_ENV.size(); n++){
    env_list = LIST_ENV[n]
    // dir("${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}") {
    //   // withAWS(credentials:'openshift-s3-credential') {
    //   //   s3Download bucket: 'acm-aws-openshift-configuration-repo', file: "${APP_NAME}-${APP_VERSION}.tar.gz", path: "${COUNTRY_CODE}/${env_list}/${APP_NAME}/${APP_NAME}-${APP_VERSION}.tar.gz"
    //   // }
    //   // sh "mkdir -p ${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}"
    //   // sh "cp -rf ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}/${APP_NAME}-${APP_VERSION}.tar.gz  ${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}/"
    //   withAWS(credentials:'openshift-s3-credential') {
    //     s3Download bucket: 'openshift-distributed-artifacts', file: "${APP_NAME}-${APP_VERSION}.tar.gz", path: "${runway_name}/${APP_NAME}/${APP_NAME}-${APP_VERSION}.tar.gz"
    //   }
    //   sh "mkdir -p ${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}"
    //   sh "cp -rf ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}/${APP_NAME}-${APP_VERSION}.tar.gz  ${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}/"
    // } 
    dir("${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}") {
      withAWS(credentials:'openshift-s3-credential') {
        s3Download bucket: 'openshift-distributed-artifacts', file: "${APP_NAME}-${APP_VERSION}.tar.gz", path: "${runway_name}/${APP_NAME}/${APP_NAME}-${APP_VERSION}.tar.gz"
      }
      sh "tar -zxvf ${APP_NAME}-${APP_VERSION}.tar.gz -C ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}/"
      dir("${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}"){
        sh "cp -rf ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}/${APP_NAME}-${APP_VERSION}/configuration/* ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}/"
        sh "/bin/tar -zcvf \"${APP_NAME}-${APP_VERSION}.tar.gz\" \"${APP_NAME}-${APP_VERSION}/\""
        sh "mkdir -p ${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}"
        sh "cp -rf ${DIRECTORY_WORKSPACE}/s3-pull-config/tmp/${env_list}/${APP_NAME}-${APP_VERSION}/${APP_NAME}-${APP_VERSION}.tar.gz  ${CONFIG_PATH}/${COUNTRY_CODE}/${env_list}/${APP_NAME}/"
      }
    } 
  } 

  return "Sync Configure Server Complete!"

} 