#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){

  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GLOBAL_VARS = config.global_vars
  def directory_workspace = config.directory
  def openshiftVersion = config.openshift_version 
  def APP_VERSION = config.app_version
  def namespace_cicd = config.namespace
  def namespace_dev = config.namespaceDev
  def country_code = config.country_code
  def APPLICATION_MOUNTEBANK_EXISTING = config.applicationMountebankExisting
  def listFileCommitBoolean = config.listCommitBoolean
  def images = [ "app", "app_mb" ]
  def imageApplication = ""
  def imageApplicationMountebank = ""

  parallel 'Application': {
    if( GLOBAL_VARS['RUNWAY_NAME'] == "TESSERACT" ) {
      sh "cp -f ${directory_workspace}/startup.sh ${directory_workspace}/pipeline/script/tesseract-startup.sh"
    }
    imageApplication = acnImageBuild {
      global_vars = GLOBAL_VARS
      appScope = GLOBAL_VARS['APP_SCOPE']
      appLang = GLOBAL_VARS['APP_LANG']
      countryCode = country_code
      appName = GLOBAL_VARS['APP_NAME']
      packageExtension = GLOBAL_VARS['PACKAGE_EXTENSION']
      middlewareName = GLOBAL_VARS['MIDDLEWARE_NAME']
      directory = directory_workspace
      openshiftVersionFolder = openshiftVersion
      appVersion = APP_VERSION
      imageType = "application"
      namespace = namespace_cicd
      envNameImage = "dev"
    }
    images.set(0, imageApplication)
  }, 'Application-Mountebank': {
    if(APPLICATION_MOUNTEBANK_EXISTING == 'application-MB-Not-Existing' || listFileCommitBoolean.contains(true)){
      imageApplicationMountebank = acnImageBuild {
        global_vars = GLOBAL_VARS
        appScope = GLOBAL_VARS['APP_SCOPE']
        appLang = GLOBAL_VARS['APP_LANG']
        countryCode = country_code
        appName = GLOBAL_VARS['APP_NAME']
        packageExtension = GLOBAL_VARS['PACKAGE_EXTENSION']
        middlewareName = GLOBAL_VARS['MIDDLEWARE_NAME']
        directory = directory_workspace
        openshiftVersionFolder = openshiftVersion
        appVersion = APP_VERSION
        imageType = "mountebank"
        namespace = namespace_cicd
        envNameImage = "dev"
      }
      images.set(1, imageApplicationMountebank)
    } else {
      echo "http://${GLOBAL_VARS['APP_NAME']}-mountebank.${namespace_dev}.svc:2525 already existing and no change artifact"
    } // End condition for take action to build images mountebank
  } // End parallel build images

  return images

} // End Parallel build image