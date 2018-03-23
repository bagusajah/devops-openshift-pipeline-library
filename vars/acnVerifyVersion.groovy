#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def APP_URL_OPENSHIFT_FORMAT = config.app_url_openshift_format
  def APP_VERSION = config.version
  def GLOBAL_VARS = config.global_vars
  def envList = config.envList

  // def responseVersion = ""
  def rs = ""
  def version_mock = "1.0.1-74"

  try {
    timeout(time: 10, unit: 'MINUTES'){
      waitUntil {
        // rs = restGetURL{
        //   authString = ""
        //   url = APP_URL_OPENSHIFT_FORMAT
        // }
        // echo "expect ${APP_VERSION} but application version is ${rs.build.version}"
        // if (rs.build.version == APP_VERSION){
        //   return true
        // }else {
        //   return false
        // }
        echo "===== mock version ======"
        echo "expect ${APP_VERSION} but application version is ${version_mock}"
        if (version_mock == APP_VERSION){
          return true
        }else {
          return false
        }
      } // End waitUntil
    }
  }
  catch(e) {
    slackSend (channel: "${GLOBAL_VARS['CHANNEL_SLACK_NOTIFICATION']}", color: '#FF9900', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} FAIL step \"Verify version application has changed\" on ${envList} environment. ${env.BUILD_URL}")
    error "Pipeline aborted due to ${env.JOB_NAME} can not deploy version ${env.BUILD_NUMBER}"
  }

} // End Verify Version