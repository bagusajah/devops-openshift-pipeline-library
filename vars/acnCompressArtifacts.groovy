#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def APP_VERSION = config.app_version
  def GIT_HASH = config.git_hash_application
  def directory = config.directory
  def app_image = config.app_image
  def build_on_branch = config.build_on_branch
  def git_hash_openshift_configuration = config.git_hash_openshift_configuration
  def runway_name = config.runway_name
  def app_name = config.app_name
  def country_code = config.country_code
  def git_configuration = config.git_configuration
  def git_hash_configuration = ""

  if ( runway_name == "OPENSHIFT" ) {
    dir("${directory}/distributed-runway/${runway_name}") {
      // dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"){
      //   sh "cp -rf ${directory}/update-config/${country_code}/staging/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"
      // }
      // dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"){
      //   sh "cp -rf ${directory}/update-config/${country_code}/production/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"
      // }
      git credentialsId: 'bitbucket-credential', url: git_configuration
      GIT_HASH_OPENSHIFT_CONFIGURATION = sh script: "cd ${directory}/distributed-runway/${runway_name} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_OPENSHIFT_CONFIGURATION = GIT_HASH_OPENSHIFT_CONFIGURATION.trim()
      dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"){
        sh "cp -rf ${directory}/distributed-runway/${runway_name}/th/staging/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"
      }
      dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"){
        sh "cp -rf ${directory}/distributed-runway/${runway_name}/th/prod/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"
      }
    }
    sh "touch ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.properties"
    sh "echo \"APP_IMAGE=${app_image}\" > ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.properties"
    sh "echo \"BUILD_ON_BRANCH=${build_on_branch}\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.properties"
    sh "echo \"GIT_HASH_APPLICATION=${GIT_HASH}\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.properties"
    sh "echo \"GIT_HASH_OPENSHIFT_CONFIGURATION=${git_hash_openshift_configuration}\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.properties"
    git_hash_configuration = GIT_HASH_OPENSHIFT_CONFIGURATION
  } else if ( runway_name == "ECS" ){
    dir("${directory}/distributed-runway/${runway_name}") {
      git credentialsId: 'bitbucket-credential', url: git_configuration
      GIT_HASH_ECS_CONFIGURATION = sh script: "cd ${directory}/distributed-runway/${runway_name} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_ECS_CONFIGURATION = GIT_HASH_ECS_CONFIGURATION.trim()
      dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"){
        sh "cp -rf ${directory}/distributed-runway/${runway_name}/th/staging/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"
      }
      dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"){
        sh "cp -rf ${directory}/distributed-runway/${runway_name}/th/prod/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"
      }
    }
    git_hash_configuration = GIT_HASH_ECS_CONFIGURATION
  } else if ( runway_name == "TESSERACT" ){
    dir("${directory}/distributed-runway/${runway_name}") {
      git credentialsId: 'bitbucket-credential', url: git_configuration
      GIT_HASH_TESSERACT_CONFIGURATION = sh script: "cd ${directory}/distributed-runway/${runway_name} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_TESSERACT_CONFIGURATION = GIT_HASH_TESSERACT_CONFIGURATION.trim()
      dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"){
        sh "cp -rf ${directory}/distributed-runway/${runway_name}/th/staging/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/staging"
      }
      dir("${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"){
        sh "cp -rf ${directory}/distributed-runway/${runway_name}/th/prod/${app_name}/* ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/configuration/production"
      }
    }
    sh "touch ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.txt"
    sh "echo \"package_version=${APP_VERSION}\" > ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.txt"
    sh "echo \"git_config_revision=${GIT_HASH_TESSERACT_CONFIGURATION}\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.txt"
    sh "echo \"app_name=${app_name}\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.txt"
    sh "echo \"git_revision=${GIT_HASH}\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.txt"
    sh "echo \"country_code=th\" >> ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/build_info.txt"
    git_hash_configuration = GIT_HASH_TESSERACT_CONFIGURATION
  } else if ( runway_name == "EQUATOR" ) {
    sh "cp ${directory}/equator-variables.properties ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}"
    sh "sed -i \"s~#app_name#~${app_name}~g\" ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/equator-variables.properties"
    sh "sed -i \"s~#app_version#~${APP_VERSION}~g\" ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/equator-variables.properties"
    sh "sed -i \"s~#git_hash#~${GIT_HASH}~g\" ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}/equator-variables.properties"
    git_hash_configuration = ""
  } // End Condition copy artifact and config to path distributed-runway/runwayName/appName-appVersion
  sh "cp -rf ${directory}/pipeline ${directory}/distributed-runway/${runway_name}/${app_name}-${APP_VERSION}"
  sh "cd ${directory}/distributed-runway/${runway_name} && /bin/tar -zcvf \"${app_name}-${APP_VERSION}.tar.gz\" \"${app_name}-${APP_VERSION}/\""
  dir("${directory}/distributed-runway/${runway_name}"){
    withAWS(credentials:'openshift-s3-credential') {
      s3Upload bucket: 'openshift-distributed-artifacts', file: "${app_name}-${APP_VERSION}.tar.gz", path: "${runway_name}/${app_name}/${app_name}-${APP_VERSION}.tar.gz"
    }
  } // End directory for upload .tar.gz file to S3

  return git_hash_configuration

} // Compress artifacts