#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import groovy.json.JsonSlurperClassic

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def app_name = = config.app_name
  def tmt_test_result_url_performance = config.tmt_test_result_url_performance
  def tmt_url = config.tmt_url
  def GCHAT_NOTIFIER_WEBHOOK = config.GCHAT_NOTIFIER_WEBHOOK
  def app_version = config.app_version
  def authorizationTMTId = config.authorizationTMTId
  def jobTMTId = config.jobTMTId
  def rerun_condition_action = config.rerun_condition_action
  def app_url_type_service = config.app_url_type_service
  def conditionForGetVersion = config.conditionForGetVersion
  def test_tools = config.test_tools
  def directory = config.directory

  if ( rerun_condition_action == conditionForGetVersion ){
    def result = restGetVersionApplicationURL{
      url = app_url_type_service
    }
    app_version = result.build.version + "-retest"
  }

  dir("${directory}/system_integration_test"){
    git credentialsId: 'bitbucket-credential', url: 'https://bitbucket.org/ascendcorp/acm-sit-robot.git'
  }
  dir("${directory}/system_integration_test/tmp/${app_name}-${app_version}-build-${env.BUILD_NUMBER}"){
    sh "touch init.txt"
    sh "rm -rf init.txt"
  }
  if ( test_tools == 'robot' ) {
    sh "chmod +x ${directory}/system_integration_test/scripts/${app_name}/run.sh"
    sh "cd ${directory}/system_integration_test/scripts/${app_name} && ./run.sh ${app_name}"
    sh "cp -rf ${directory}/system_integration_test/results/${app_name}/* ${directory}/system_integration_test/tmp/${app_name}-${app_version}-build-${env.BUILD_NUMBER}"
    sh "cd ${directory}/system_integration_test/tmp && /bin/zip -r \"${app_name}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${app_name}-${app_version}-build-${env.BUILD_NUMBER}/\""
    dir("${directory}/system_integration_test/tmp"){
      withAWS(credentials:'openshift-s3-credential') {
        s3Upload bucket: tmt_test_result_url_performance, file: "${app_name}-${app_version}-build-${env.BUILD_NUMBER}.zip", path: "robot-result/${app_name}/${env.BUILD_NUMBER}/${app_name}-${app_version}-build-${env.BUILD_NUMBER}.zip"
      }
    }
    sh "echo BUCKET S3 result SIT is https://s3.console.aws.amazon.com/s3/buckets/${tmt_test_result_url_performance}/robot-result/${app_name}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
    sh "curl -k -H \"Authorization: ${authorizationTMTId}\" ${tmt_url}/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
    step([
      $class : 'RobotPublisher', 
      outputPath : "${directory}/system_integration_test/results/${app_name}",
      passThreshold : 100,
      unstableThreshold: 100, 
      otherFiles : "*.png",
      outputFileName: "output.xml", 
      disableArchiveOutput: false, 
      reportFileName: "report.html", 
      logFileName: "log.html",
      onlyCritical: false,
      enableCache: false
    ])
    if( currentBuild.result == 'UNSTABLE' || currentBuild.result == 'FAILURE' ){
      acnSendAlertToWebhook {
        urlWebhook = GCHAT_NOTIFIER_WEBHOOK
        envName = environmentForWorkspace
        stageCurrent = "FAIL step Run System Integration Test"
        appName = app_name
      }
      error "Pipeline aborted due to ${env.JOB_NAME} run system integration test ${env.BUILD_NUMBER} is FAILURE"
    } // End Condition RobotPublisher
  } else if ( test_tools == 'jmeter' ) {
    sh "echo available in next release"
  } // End Condition robot or jmeter

} // End System Integration Test