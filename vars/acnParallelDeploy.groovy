#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){

  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def app_name = config.app_name
  def replica_num = config.replicaNum
  def route_hostname = config.routeHostname
  def route_hostname_mountebank = config.routeHostnameMountebank
  def network_policy = config.networkPolicy
  def app_scope = config.appScope
  def route_tls_enable = config.routeTLSEnable
  def openshiftVersion = config.openshift_version 
  def images = config.imagesList
  def namespace_cicd = config.namespace 
  def LIST_ENV = config.list_env
  def namespace_dev = config.namespaceDev
  def APPLICATION_MOUNTEBANK_EXISTING = config.applicationMountebankExisting
  def listFileCommitBoolean = config.listCommitBoolean
  def APP_VERSION = config.app_version
  def buildDetailList = config.buildDetailList
  def directory = config.directory

  echo "START PARALLEL"
  echo "openshiftVersion ${openshiftVersion}"
  echo "images[0] ${images[0]}"
  echo "app_name ${app_name}"
  echo "APP_VERSION ${APP_VERSION}"
  echo "LIST_ENV[0] ${LIST_ENV[0]}"
  echo "replica_num ${replica_num}"
  echo "route_hostname ${route_hostname}"
  echo "network_policy ${network_policy}"
  echo "namespace_dev ${namespace_dev}"
  echo "buildDetailList[2] ${buildDetailList[2]}"
  echo "buildDetailList[5] ${buildDetailList[5]}"
  echo "app_scope ${app_scope}"
  echo "route_tls_enable ${route_tls_enable}"
  echo "buildDetailList ${buildDetailList}"
  echo "directory ${directory}"

  acnGetDeploymentResources { 
    versionOpenshift = openshiftVersion
    imageName = images[0]
    appName = app_name
    appVersion = APP_VERSION
    envName = LIST_ENV[0]
    replicaNum = replica_num
    routeHostname = route_hostname
    networkPolicy = network_policy
    namespace_env = namespace_dev
    gitHashApplication = buildDetailList[2]
    gitSourceBranch = buildDetailList[5]
    appScope = app_scope
    routeTLSEnable = route_tls_enable
    forceDeployList = buildDetailList
    directoryWorkspace = directory
  }

  // parallel "Application": {
  //   def rcDev = acnGetDeploymentResources { 
  //     versionOpenshift = openshiftVersion
  //     imageName = images[0]
  //     appName = app_name
  //     appVersion = APP_VERSION
  //     envName = LIST_ENV[0]
  //     replicaNum = replica_num
  //     routeHostname = route_hostname
  //     networkPolicy = network_policy
  //     namespace_env = namespace_dev
  //     gitHashApplication = buildDetailList[2]
  //     gitSourceBranch = buildDetailList[5]
  //     appScope = app_scope
  //     routeTLSEnable = route_tls_enable
  //     forceDeployList = buildDetailList
  //     directoryWorkspace = directory
  //   }
  // }, "Application-Mountebank": {
  //   if(APPLICATION_MOUNTEBANK_EXISTING == "application-MB-Not-Existing" || listFileCommitBoolean.contains(true)){
  //     def rcDevMB = acnGetDeploymentResources { 
  //       versionOpenshift = openshiftVersion
  //       imageName = images[1]
  //       appName = "${app_name}-mountebank"
  //       appVersion = APP_VERSION
  //       envName = LIST_ENV[0]
  //       replicaNum = replica_num
  //       routeHostname = route_hostname_mountebank
  //       networkPolicy = network_policy
  //       namespace_env = namespace_dev
  //       gitHashApplication = buildDetailList[2]
  //       gitSourceBranch = buildDetailList[5]
  //       appScope = app_scope
  //       routeTLSEnable = route_tls_enable
  //       forceDeployList = buildDetailList
  //       directoryWorkspace = directory
  //     }
  //   } else {
  //     sh "echo http://${app_name}-mountebank.${namespace_dev}.svc:2525 already existing and no change artifact"
  //   }
  // } // End Scopy Deploy Parallel

} // End Parallel deploy
