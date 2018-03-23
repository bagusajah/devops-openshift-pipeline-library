#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

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

  def scriptRunExisting = ""
  def scriptRunExistingList = []

  if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 0 ) {
    currentBuild.result = 'UNSTABLE'
    slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FFFF66', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} UNSTABLE step Run Integration Test on ${environmentForWorkspace} environment. Because no git to execute'. ${env.BUILD_URL}")
    error "No git to execute"
  } else {
    if ( rerun_condition_action == conditionForGetVersion ){
      // def result = restGetURL{
      //     authString = ""
      //     url = app_url_type_service
      // }
      // app_version = result.build.version + "-retest"
      echo "======= mock version ======="
      version_mock = "1.0.1-85"
      app_version = version_mock + "-retest"
    }
    if ( test_tools == 'robot' ) {
        dir("${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"){
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
              scriptRunExisting = fileExists "${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run_smoke.sh"
              // scriptRunExisting = sh script: "[ -f ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run_smoke.sh ] && echo \"Found\" || echo \"Not_Found\"", returnStdout: true
              if ( !scriptRunExisting ) {
                sh "echo ${GIT_INTEGRATION_TEST_NAME} DONT HAVE FILE RUN_SMOKE.SH"
                scriptRunExistingList.add("not_have")
              } else {
                sh "echo ${GIT_INTEGRATION_TEST_NAME} HAVE FILE RUN_SMOKE.SH"
                scriptRunExistingList.add("have")
                sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run_smoke.sh"
                sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run_smoke.sh"
                sh "cp -rf ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}_smoke/* ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
              }
            }
        } else if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() > 1 ) {
            def cmd_mrg = "rebot --nostatusrc --outputdir ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --output output.xml --merge"
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
                  scriptRunExisting = fileExists "${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run_smoke.sh"
                  // scriptRunExisting = sh script: "[ -f ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run_smoke.sh ] && echo \"Found\" || echo \"Not_Found\"", returnStdout: true
                  if ( !scriptRunExisting ) {
                    sh "echo ${GIT_INTEGRATION_TEST_NAME} DONT HAVE FILE RUN_SMOKE.SH"
                    scriptRunExistingList.add("not_have")
                  } else {
                    sh "echo ${GIT_INTEGRATION_TEST_NAME} HAVE FILE RUN_SMOKE.SH"
                    scriptRunExistingList.add("have")
                    sh "chmod +x ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run_smoke.sh"
                    sh "cd ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run_smoke.sh"
                    sh "rsync -av --progress ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}_smoke/ ${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
                    cmd_mrg = cmd_mrg + " ${directory}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}_smoke/output.xml"
                  }
              } // End directory pull git
            } // End loop git more than 1
            if ( scriptRunExistingList.contains("have") ) {
              sh "${cmd_mrg}"
            }
        } // End condition git equal 1 or more than 1

        if ( scriptRunExistingList.contains("have") ) {
          step([
            $class : 'RobotPublisher', 
            outputPath : "${directory}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}",
            passThreshold : 100,
            unstableThreshold: 100, 
            otherFiles : "*.png",
            outputFileName: "output.xml", 
            disableArchiveOutput: false, 
            reportFileName: "report.html", 
            logFileName: "log.html",
            onlyCritical: false,
            enableCache: false
          ]) // End robot plug-in
          sh "cd ${directory}/robot/results/${environmentForWorkspace}_smoke && /bin/zip -r \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/\""
          def bucket = ""
          if ( environmentForWorkspace == "dev" ) {
            bucket = global_vars['BUCKET_TEST_RESULT_DEV']
          } else if ( environmentForWorkspace == "qa" ) {
            bucket = global_vars['BUCKET_TEST_RESULT_QA']
          } else if ( environmentForWorkspace == "staging" ) {
            bucket = global_vars['BUCKET_TEST_RESULT_STAGING']
          }
          if( currentBuild.result == 'UNSTABLE' || currentBuild.result == 'FAILURE' ){
            dir("${directory}/robot/results/${environmentForWorkspace}_smoke"){
              step([
                $class : 'S3BucketPublisher',
                profileName : 'openshift-profile-s3',
                entries: [[
                  bucket: "${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}",
                  selectedRegion: 'ap-southeast-1',
                  showDirectlyInBrowser: true,
                  sourceFile: "${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip",
                  storageClass: 'STANDARD'
                ]]
              ])
            } // End upload zip file to S3
            sh "echo BUCKET S3 result ${environmentForWorkspace} is https://s3.console.aws.amazon.com/s3/buckets/${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
            sh "curl -k -H \"Authorization: ${authorizationTMTId}\" https://ascendtmt.tmn-dev.net/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
            slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FFFF66', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} FAIL step Run Smoke Test on ${environmentForWorkspace} environment. ${env.BUILD_URL}")
            error "Pipeline aborted due to ${env.JOB_NAME} run system integration test ${env.BUILD_NUMBER} is FAILURE"
          } // End Condition RobotPublisher is Fail
        } // End condition have run_smoke.sh
    } else if ( test_tools == 'jmeter' ) {
      sh "echo jmeter not run smoke test"
    } // End Condition robot or jmeter
  } // End Condition global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']
} // End Method Runtest