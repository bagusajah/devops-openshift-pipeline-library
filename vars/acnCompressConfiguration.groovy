#!/usr/bin/groovy
import com.ascendmoney.vulcan.Utils
// import io.fabric8.Fabric8Commands
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GLOBAL_VARS = config.global_vars
  def LIST_ENV = config.list_env
  def APP_VERSION = config.version
  def directory = config.directory

  def GIT_HASH_FABRIC8_CONFIGURATION = ""

  def test_pwd = pwd()
  echo "TEST_PWD ${test_pwd}"

  git credentialsId: 'bitbucket-credential', url: GLOBAL_VARS['GIT_FABRIC8_CONFIGURATION']  
  GIT_HASH_FABRIC8_CONFIGURATION = sh script: "cd ${directory}/update-config && git rev-parse --verify HEAD", returnStdout: true
  GIT_HASH_FABRIC8_CONFIGURATION = GIT_HASH_FABRIC8_CONFIGURATION.trim()

  def env_list = ""
  for(n = 0; n < LIST_ENV.size(); n++){
    env_list = LIST_ENV[n]
    dir("${directory}/update-config/tmp/${env_list}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}"){
      sh "cp -rf ${directory}/update-config/${GLOBAL_VARS['COUNTRY_CODE']}/${env_list}/${GLOBAL_VARS['APP_NAME']}/* ${directory}/update-config/tmp/${env_list}/${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}/"
      sh "cd ${directory}/update-config/tmp/${env_list} && /bin/tar -zcvf \"${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}.zip\" \"${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}/\""
      dir("${directory}/update-config/tmp/${env_list}"){
        step([
          $class : 'S3BucketPublisher',
          profileName : 'openshift-profile-s3',
          entries: [[
            bucket: "acm-aws-fabric8-configuration-repo/${GLOBAL_VARS['COUNTRY_CODE']}/${env_list}/${GLOBAL_VARS['APP_NAME']}",
            selectedRegion: 'ap-southeast-1',
            showDirectlyInBrowser: true,
            sourceFile: "${GLOBAL_VARS['APP_NAME']}-${APP_VERSION}.zip",
            storageClass: 'STANDARD'
          ]]
        ])
      } // End Upload zip file to s3
    } // End scope
  } // End Loop zip file and upload to s3

  return GIT_HASH_FABRIC8_CONFIGURATION

} // End Compress Configuration