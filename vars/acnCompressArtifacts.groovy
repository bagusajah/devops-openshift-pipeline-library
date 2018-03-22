#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def global_vars_files = config.global_vars
  def APP_VERSION = config.app_version
  def GIT_HASH = config.git_hash_application
  def directory = config.directory
  def git_hash_configuration = ""

  if ( global_vars_files['RUNWAY_NAME'] == "OPENSHIFT" ) {
    dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}") {
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"){
        sh "cp -rf ${directory}/update-config/${global_vars_files['COUNTRY_CODE']}/staging/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      }
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/pre-prod"){
        sh "cp -rf ${directory}/update-config/${global_vars_files['COUNTRY_CODE']}/pre-prod/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/pre-prod"
      }
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"){
        sh "cp -rf ${directory}/update-config/${global_vars_files['COUNTRY_CODE']}/prod/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      }
    }
    git_hash_configuration = ""
  } else if ( global_vars_files['RUNWAY_NAME'] == "ECS" ){
    dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}") {
      git credentialsId: 'bitbucket-credential', url: global_vars_files['GIT_ECS_CONFIGURATION']
      GIT_HASH_ECS_CONFIGURATION = sh script: "cd ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_ECS_CONFIGURATION = GIT_HASH_ECS_CONFIGURATION.trim()
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"){
        sh "cp -rf ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/staging/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      }
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"){
        sh "cp -rf ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/prod/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      }
    }
    git_hash_configuration = GIT_HASH_ECS_CONFIGURATION
  } else if ( global_vars_files['RUNWAY_NAME'] == "TESSERACT" ){
    // sh "mkdir -p ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration"
    dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}") {
      git credentialsId: 'bitbucket-credential', url: global_vars_files['GIT_TESSERACT_CONFIGURATION']
      GIT_HASH_TESSERACT_CONFIGURATION = sh script: "cd ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_TESSERACT_CONFIGURATION = GIT_HASH_TESSERACT_CONFIGURATION.trim()
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"){
        sh "cp -rf ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/staging/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      }
      dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"){
        sh "cp -rf ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/prod/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      }
      // sh "mkdir -p ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      // sh "cp -rf ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/staging/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      // sh "mkdir -p ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      // sh "cp -rf ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/prod/${global_vars_files['APP_NAME']}/* ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
    }
    sh "touch ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"package_version=${APP_VERSION}\" > ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"git_config_revision=${GIT_HASH_TESSERACT_CONFIGURATION}\" >> ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"app_name=${global_vars_files['APP_NAME']}\" >> ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"git_revision=${GIT_HASH}\" >> ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"country_code=th\" >> ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    git_hash_configuration = GIT_HASH_TESSERACT_CONFIGURATION
  } else if ( global_vars_files['RUNWAY_NAME'] == "EQUATOR" ) {
    sh "cp ${directory}/equator-variables.properties ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}"
    sh "sed -i \"s~#app_name#~${global_vars_files['APP_NAME']}~g\" ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/equator-variables.properties"
    sh "sed -i \"s~#app_version#~${APP_VERSION}~g\" ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/equator-variables.properties"
    sh "sed -i \"s~#git_hash#~${GIT_HASH}~g\" ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/equator-variables.properties"
    git_hash_configuration = ""
  } // End Condition copy artifact and config to path distributed-runway/runwayName/appName-appVersion
  sh "cp -rf ${directory}/pipeline ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}"
  sh "cd ${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']} && /bin/tar -zcvf \"${global_vars_files['APP_NAME']}-${APP_VERSION}.zip\" \"${global_vars_files['APP_NAME']}-${APP_VERSION}/\""
  dir("${directory}/distributed-runway/${global_vars_files['RUNWAY_NAME']}"){
    step([
      $class : 'S3BucketPublisher',
      profileName : 'openshift-profile-s3',
      entries: [[
        bucket: "openshift-distributed-artifacts/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}",
        selectedRegion: 'ap-southeast-1',
        showDirectlyInBrowser: true,
        sourceFile: "${global_vars_files['APP_NAME']}-${APP_VERSION}.zip",
        storageClass: 'STANDARD'
      ]]
    ])
  } // End directory for upload zip file to S3

  return git_hash_configuration

} // Compress artifacts