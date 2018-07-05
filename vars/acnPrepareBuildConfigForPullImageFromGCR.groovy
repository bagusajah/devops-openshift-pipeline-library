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
  def openshiftVersionFolder = config.openshiftVersion
  def envName = config.envName
  def directory = config.directory

  echo "countryCode ${countryCode}"

  def GLOBAL_VARS = ""
  def appScope = ""

  def gcrProjectName = ""
  def dockerRegistry = ""
  def fileName = appName + "-" + appVersion
  
  dockerRegistry = acnGetDockerRegistryServiceHost()

  dir("${directory}/s3-pull-artifact") {
    withAWS(credentials:'openshift-s3-credential') {
      s3Download bucket: 'openshift-distributed-artifacts', file: "${fileName}.tar.gz", path: "OPENSHIFT/${appName}/${fileName}.tar.gz"
    }
    sh "tar -zxvf ${fileName}.tar.gz -C ${directory}/s3-pull-artifact"
    sh "rm -rf ${fileName}.tar.gz"
    script{
      GLOBAL_VARS = readProperties  file:"${directory}/s3-pull-artifact/${fileName}/pipeline/variables.properties"
      namespace_cicd = "${GLOBAL_VARS['APP_SCOPE']}-cicd"
      namespace_dev = "${GLOBAL_VARS['APP_SCOPE']}-${GLOBAL_VARS['APP_SVC_GROUP']}-dev"
      namespace_qa = "${GLOBAL_VARS['APP_SCOPE']}-${GLOBAL_VARS['APP_SVC_GROUP']}-qa"
      namespace_performance = "${GLOBAL_VARS['APP_SCOPE']}-${GLOBAL_VARS['APP_SVC_GROUP']}-performance"
    }
    appScope = GLOBAL_VARS['APP_SCOPE']
    appScope = appScope.toLowerCase()
    countryCode = countryCode.toLowerCase()
    gcrProjectName = "tmn-" + countryCode + "-ci"
    if ( countryCode == "th" && appScope == "equator" ) {
      gcrProjectName = "tmn-th-equator-ci"
    } 
    sh "sed -i \"s~#ENV_NAME#~${envName}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/pull-image-buildconfig.yaml"
    sh "sed -i \"s~#GCR_PROJECT_NAME#~${gcrProjectName}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/pull-image-buildconfig.yaml"
    sh "sed -i \"s~#APP_VERSION#~${appVersion}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/pull-image-buildconfig.yaml"
    sh "sed -i \"s~#DOCKER_REGISTRY_SERVICE_IP#~${dockerRegistry}~g\" ${directory}/s3-pull-artifact/${fileName}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfigs/pull-image-buildconfig.yaml"
    sh "tar -zcvf ${fileName}.tar.gz ${fileName}/"
    withAWS(credentials:'openshift-s3-credential') {
      s3Upload bucket: 'openshift-distributed-artifacts', file: "${fileName}.tar.gz", path: "OPENSHIFT/${appName}/${fileName}.tar.gz"
    }
  }

  return appScope;
}


    