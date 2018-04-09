#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body){

  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GLOBAL_VARS = config.global_vars
  def openshiftVersion = config.openshift_version 
  def images = config.imagesList
  def namespace_cicd = config.namespace 
  def LIST_ENV = config.list_env
  def namespace_dev = config.namespaceDev
  def APPLICATION_MOUNTEBANK_EXISTING = config.applicationMountebankExisting
  def listFileCommitBoolean = config.listCommitBoolean
  def APP_VERSION = config.app_version
  def buildDetailList = config.buildDetailList

  parallel 'Application': {
    def rcDev = acnGetDeploymentResources { 
      versionOpenshift = openshiftVersion
      exposeApp = 'true'
      imageName = images[0]
      namespace = namespace_cicd
      appProtocal = GLOBAL_VARS['APP_PROTOCOL']
      appName = GLOBAL_VARS['APP_NAME']
      appVersion = APP_VERSION
      envName = LIST_ENV[0]
      replicaNum = GLOBAL_VARS['DEFAULT_NUM_REPLICA_DEV']
      routeHostname = GLOBAL_VARS['ROUTE_HOSTNAME_DEV']
      networkPolicy = GLOBAL_VARS['NETWORK_POLICY_ACCEPT_LABELS']
      namespace_env = namespace_dev
      gitHashApplication = buildDetailList[2]
      gitSourceBranch = buildDetailList[5]
      appScope = GLOBAL_VARS['APP_SCOPE']
    }
  }, 'Application-Mountebank': {
    if(APPLICATION_MOUNTEBANK_EXISTING == 'application-MB-Not-Existing' || listFileCommitBoolean.contains(true)){
      def rcDevMB = acnGetDeploymentResources { 
        versionOpenshift = openshiftVersion
        exposeApp = 'true'
        imageName = images[1]
        namespace = namespace_cicd
        appName = "${GLOBAL_VARS['APP_NAME']}-mountebank"
        appVersion = APP_VERSION
        envName = LIST_ENV[0]
        replicaNum = GLOBAL_VARS['DEFAULT_NUM_REPLICA_DEV']
        routeHostname = GLOBAL_VARS['ROUTE_HOSTNAME_MOUNTEBANK']
        networkPolicy = GLOBAL_VARS['NETWORK_POLICY_ACCEPT_LABELS']
        namespace_env = namespace_dev
        gitHashApplication = buildDetailList[2]
        gitSourceBranch = buildDetailList[5]
        appScope = GLOBAL_VARS['APP_SCOPE']
      }
    } else {
      sh "echo http://${GLOBAL_VARS['APP_NAME']}-mountebank.${namespace_dev}.svc:2525 already existing and no change artifact"
    }
  } // End Scopy Deploy Parallel

} // End Parallel deploy