#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def APP_URL_OPENSHIFT_FORMAT = config.app_url_openshift_format
  def APP_VERSION = config.version
  def app_name = config.app_name
  def url_webhook_google_chat_notification = config.url_webhook_google_chat_notification

  def rs = ""
  def resultVersionApplication = null

  try {
    timeout(time: 10, unit: 'MINUTES'){
      waitUntil {
        rs = restGetVersionApplicationURL{
          url = APP_URL_OPENSHIFT_FORMAT
        }
        resultVersionApplication = rs.build.version
        echo "expect ${APP_VERSION} but application version is ${resultVersionApplication}"
        if (resultVersionApplication == APP_VERSION){
          return true
        } else {
          return false
        }
      } // End waitUntil
    }
  }
  catch(e) {
    acnSendAlertToWebhook {
      urlWebhook = url_webhook_google_chat_notification
      envName = APP_URL_OPENSHIFT_FORMAT
      stageCurrent = "FAIL step Verify version application has changed"
      appName = app_name
    }
    error "Pipeline aborted due to ${env.JOB_NAME} can not deploy version ${env.BUILD_NUMBER}"
  }

} // End Verify Version