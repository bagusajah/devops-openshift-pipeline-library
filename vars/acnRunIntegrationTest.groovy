#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import groovy.json.JsonSlurperClassic

def call(body) {

  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def conditionForGetVersion = config.conditionForGetVersion
  def environmentForWorkspace = config.environmentForWorkspace
  def global_vars = config.global_vars
  def rerun_condition_action = config.rerun_condition_action
  def app_version = config.app_version
  def app_url_type_service = config.app_url_type_service
  def jobTMTId = config.jobTMTId
  def authorizationTMTId = config.authorizationTMTId
  def test_tools = config.test_tools
  def directory = config.directory

  if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 0 ) {
    currentBuild.result = 'UNSTABLE'
    acnSendAlertToWebhook {
      urlWebhook = GLOBAL_VARS['URL_WEBHOOK_GOOGLE_CHAT_NOTIFICATION']
      envName = environmentForWorkspace
      stageCurrent = "FAIL step Run Integration Test Because no git to execute"
      appName = GLOBAL_VARS['APP_NAME']
    }
    error "No git to execute"
  } else {
    if ( rerun_condition_action == conditionForGetVersion ){
      def result = restGetVersionApplicationURL{
        url = app_url_type_service
      }
      app_version = result.build.version + "-retest"
    }

    def file_run_smoke_test_result = fileExists "${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/output.xml"

    if ( test_tools == "robot" ) {
      sh "echo START RUN INTEGRATION TEST"
      dir("${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"){
        sh "touch init.txt"
        sh "rm -rf init.txt"
      }
      dir("${directory}/robot/results/${environmentForWorkspace}_integration/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"){
        sh "touch init.txt"
        sh "rm -rf init.txt"
      }
      def cmd_mrg = "rebot --nostatusrc --outputdir ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --output output.xml --merge"
      if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 1 ) {
        sh "echo 1 GIT"
        git_integration_test = "GIT_INTEGRATION_TEST_LIST_0"
        GIT_TEST = global_vars[git_integration_test]
        GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
        GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
        if ( environmentForWorkspace == "qa" ) {
          if ( file_run_smoke_test_result ) {
            sh "echo HAVE RUN_SMOKE.SH"
            sh "rsync -av --progress ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/ ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            sh "ls -la ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
            cmd_mrg = cmd_mrg + " ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/output.xml"
            sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
            sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
            sh "rsync -av --progress ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            sh "ls -la ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
            cmd_mrg = cmd_mrg + " ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/output.xml"
            sh "${cmd_mrg}"
          } else {
            sh "echo DONT HAVE RUN_SMOKE.SH"
            sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
            sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
            sh "cp -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/* ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
          } // End condition run smoke is exist --> merge result
        } else {
          echo "NOT QA env is ${environmentForWorkspace}"
          sh "rm -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}"
          dir("${directory}/robot/${GIT_INTEGRATION_TEST_NAME}") {
            git credentialsId: 'bitbucket-credential', url: GIT_TEST
            sh "chmod +x scripts/${environmentForWorkspace}/run.sh"
            sh "cd scripts/${environmentForWorkspace} && ./run.sh" 
            sh "cp -rf results/${environmentForWorkspace}/* ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
          }
        } // End condition if run smoke test
      } else if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() > 1 ) {
        sh "echo more than 1 GIT"
        if ( environmentForWorkspace == "qa" ) {
          if ( file_run_smoke_test_result ) {
            sh "echo HAVE RUN_SMOKE.SH"
            sh "rsync -av --progress ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/ ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            cmd_mrg = cmd_mrg + " ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/output.xml"
            for ( i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++ ) {
              sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
              git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
              GIT_TEST = global_vars[git_integration_test]
              GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
              GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
              sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
              sh "rsync -av --progress ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
              cmd_mrg = cmd_mrg + " ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/output.xml"
            }
          } else {
            sh "echo DONT HAVE RUN_SMOKE.SH"
            for ( i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++ ) {
              sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
              git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
              GIT_TEST = global_vars[git_integration_test]
              GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
              GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
              sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
              sh "rsync -av --progress ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
              cmd_mrg = cmd_mrg + " ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/output.xml"
            }
          }
        } else {
          echo "NOT QA env is ${environmentForWorkspace}"
          for (i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++) {
            sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
            git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
            GIT_TEST = global_vars[git_integration_test]
            GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
            GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
            if ( i == 0 ) {
              sh "rm -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}"
            }
            dir("${directory}/robot/${GIT_INTEGRATION_TEST_NAME}") {
              git credentialsId: 'bitbucket-credential', url: GIT_TEST
              sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
            }
            sh "rsync -av --progress ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            cmd_mrg = cmd_mrg + " ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/output.xml"
          }
        } // End condition if run smoke test
        sh "${cmd_mrg}"
      } // End conditon count of git run integration test
      sh "cd ${directory}/robot/results/${environmentForWorkspace} && /bin/zip -r \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/\""
      def bucket = ""
      if ( environmentForWorkspace == "dev" ) {
        bucket = global_vars['TMT_TEST_RESULT_URL_DEV']
      } else if ( environmentForWorkspace == "qa" ) {
        bucket = global_vars['TMT_TEST_RESULT_URL_QA']
      } else if ( environmentForWorkspace == "performance" ) {
        bucket = global_vars['TMT_TEST_RESULT_URL_PERFORMANCE']
      }
      dir("${directory}/robot/results/${environmentForWorkspace}"){
        withAWS(credentials:'openshift-s3-credential') {
          s3Upload bucket: bucket, file: "${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip", path: "robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip"
        }
      }
      sh "echo BUCKET S3 result ${environmentForWorkspace} is https://s3.console.aws.amazon.com/s3/buckets/${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
      sh "curl -k -H \"Authorization: ${authorizationTMTId}\" ${global_vars['TMT_URL']}/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
      step([
        $class : 'RobotPublisher', 
        outputPath : "${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}",
        passThreshold : global_vars['INTEGRATE_TEST_PASS_THRESHOLD'].toInteger(),
        unstableThreshold: global_vars['INTEGRATE_TEST_UNSTABLE_THRESHOLD'].toInteger(), 
        otherFiles : "*.png",
        outputFileName: "output.xml", 
        disableArchiveOutput: false, 
        reportFileName: "report.html", 
        logFileName: "log.html",
        onlyCritical: false,
        enableCache: false
      ])
      if( currentBuild.result == 'UNSTABLE' ){
        acnSendAlertToWebhook {
          urlWebhook = GLOBAL_VARS['URL_WEBHOOK_GOOGLE_CHAT_NOTIFICATION']
          envName = environmentForWorkspace
          stageCurrent = "UNSTABLE Because result threshold less than ${global_vars['ROBOT_UNSTABLE_THRESHOLD']}"
          appName = GLOBAL_VARS['APP_NAME']
        }
        error "Pipeline aborted due to ${env.JOB_NAME} run test ${env.BUILD_NUMBER} is Unstable"
      } else if(currentBuild.result == 'FAILURE'){
        acnSendAlertToWebhook {
          urlWebhook = GLOBAL_VARS['URL_WEBHOOK_GOOGLE_CHAT_NOTIFICATION']
          envName = environmentForWorkspace
          stageCurrent = "FAILURE Because result threshold less than ${global_vars['ROBOT_PASS_THRESHOLD']}"
          appName = GLOBAL_VARS['APP_NAME']
        }
        error "Pipeline aborted due to ${env.JOB_NAME} run test ${env.BUILD_NUMBER} is FAILURE"
      } // End Condition RobotPublisher
    } else if ( test_tools == "jmeter" ) {
      container(name: 'jmeter'){
        dir("${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"){
          sh "touch init.txt"
          sh "rm -rf init.txt"
        }
        if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 1 ) {
          git_integration_test = "GIT_INTEGRATION_TEST_LIST_0"
          GIT_TEST = global_vars[git_integration_test]
          GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
          GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
          sh "rm -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}"
          dir("${directory}/robot/${GIT_INTEGRATION_TEST_NAME}") {
            git credentialsId: 'bitbucket-credential', url: GIT_TEST
            sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
            sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh ${global_vars['APP_NAME']}"
            sh "cp -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/* ${directory}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
          }
        } else if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() > 1 ) {
          for ( i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++ ) {
            sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
            git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
            GIT_TEST = global_vars[git_integration_test]
            GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
            GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
            if ( i == 0 ) {
                sh "rm -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}"
            }
            dir("${directory}/robot/${GIT_INTEGRATION_TEST_NAME}") {
              git credentialsId: 'bitbucket-credential', url: GIT_TEST
              sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh ${global_vars['APP_NAME']}"
              sh "cp -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}run/* ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
            } // End directory pull git
          } // End loop git more than 1
          sh "cd ${directory}/robot/results/${environmentForWorkspace} && /bin/zip -r \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/\""
          def bucket = ""
          if ( environmentForWorkspace == "dev" ) {
            bucket = global_vars['TMT_TEST_RESULT_URL_DEV']
          } else if ( environmentForWorkspace == "qa" ) {
            bucket = global_vars['TMT_TEST_RESULT_URL_QA']
          } else if ( environmentForWorkspace == "performance" ) {
            bucket = global_vars['TMT_TEST_RESULT_URL_PERFORMANCE']
          }
          dir("${directory}/robot/results/${environmentForWorkspace}"){
            withAWS(credentials:'openshift-s3-credential') {
              s3Upload bucket: bucket, file: "${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip", path: "performance-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip"
            }
          } // End upload zip file to S3
          sh "echo BUCKET S3 result ${environmentForWorkspace} is https://s3.console.aws.amazon.com/s3/buckets/${bucket}/performance-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
          sh "curl -k -H \"Authorization: ${authorizationTMTId}\" ${global_vars['TMT_URL']}/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
        } // End condition git equal 1 or more than 1
      } // End container jmeter
    } // End condition robot or jmeter
  } // End Condition global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']
} // End Method Runtest