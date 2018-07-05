#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import java.io.File

def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def appName = config.appName
  def appVersion = config.appVersion
  def countryCode = config.country_code
  def appScope = config.appScope
  def directory = config.directory
  def runwayName = config.runwayName
  def openshiftVersionFolder = config.openshift_version

  def gcrProjectName = ""
  def dockerRegistry = ""
  def fileName = appName + "-" + appVersion
  
  countryCode = countryCode.toLowerCase()
  appScope = appScope.toLowerCase()
  dockerRegistry = acnGetDockerRegistryServiceHost()

  gcrProjectName = "tmn-" + countryCode + "-ci"
  if ( countryCode == "th" && appScope == "equator" ) {
    gcrProjectName = "tmn-th-equator-ci"
  } 

  dir("${directory}/s3-pull-artifact") {
    withAWS(credentials:'openshift-s3-credential') {
      s3Download bucket: 'openshift-distributed-artifacts', file: "${fileName}.tar.gz", path: "${runwayName}/${appName}/${fileName}.tar.gz"
    }
    sh "tar -zxvf ${fileName}.tar.gz -C ${directory}/s3-pull-artifact"
    sh "rm -rf ${fileName}.tar.gz"
    sh "sed -i \"s~#GCR_PROJECT_NAME#~${gcrProjectName}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/push-image-buildconfig.yaml"
    sh "sed -i \"s~#APP_VERSION#~${appVersion}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/push-image-buildconfig.yaml"
    sh "sed -i \"s~#DOCKER_REGISTRY_SERVICE_IP#~${dockerRegistry}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/push-image-buildconfig.yaml"
    sh "tar -zcvf ${fileName}.tar.gz ${fileName}/"
    withAWS(credentials:'openshift-s3-credential') {
      s3Upload bucket: 'openshift-distributed-artifacts', file: "${fileName}.tar.gz", path: "${runwayName}/${appName}/${fileName}.tar.gz"
    }
  }
}


    