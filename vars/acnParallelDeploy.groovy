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

  parallel 'Application': {
    def rcDev = acnGetDeploymentResources { 
      versionOpenshift = openshiftVersion
      exposeApp = 'true'
      imageName = images[0]
      namespace = namespace_cicd
      appProtocal = GLOBAL_VARS['PROTOCAL_APPLICATION']
      timeZone = GLOBAL_VARS['TIMEZONE']
      appName = GLOBAL_VARS['APP_NAME']
      appVersion = APP_VERSION
      appStartupArgs = GLOBAL_VARS['APP_STARTUP_ARGS']
      appScope = GLOBAL_VARS['APP_SCOPE']
      svcGroup = GLOBAL_VARS['SERVICE_GROUP']
      appLang = GLOBAL_VARS['APP_LANG']
      pipelineVersion = GLOBAL_VARS['PIPELINE_VERSION']
      envName = LIST_ENV[0]
      countryCode = GLOBAL_VARS['COUNTRY_CODE']
      replicaNum = GLOBAL_VARS['DEFAULT_DEV_NUM_REPLICA']
      routeHostname = GLOBAL_VARS['DEV_ROUTE_HOSTNAME']
      routePath = GLOBAL_VARS['ROUTE_PATH']
      networkPolicy = GLOBAL_VARS['NETWORK_POLICY_ACCEPT_LABELS']
      vaultSite = GLOBAL_VARS['DEV_VAULT_URL']
      tokenSite = GLOBAL_VARS['DEV_TOKEN_URL']
      namespace_env = namespace_dev
    }
  }, 'Application-Mountebank': {
    if(APPLICATION_MOUNTEBANK_EXISTING == 'application-MB-Not-Existing' || listFileCommitBoolean.contains(true)){
      def rcDevMB = acnGetDeploymentResources { 
        versionOpenshift = openshiftVersion
        exposeApp = 'true'
        imageName = images[1]
        namespace = namespace_cicd
        timeZone = GLOBAL_VARS['TIMEZONE']
        appName = "${GLOBAL_VARS['APP_NAME']}-mountebank"
        appVersion = APP_VERSION
        appStartupArgs = GLOBAL_VARS['APP_STARTUP_ARGS']
        appScope = GLOBAL_VARS['APP_SCOPE']
        svcGroup = GLOBAL_VARS['SERVICE_GROUP']
        appLang = GLOBAL_VARS['APP_LANG']
        pipelineVersion = GLOBAL_VARS['PIPELINE_VERSION']
        envName = LIST_ENV[0]
        countryCode = GLOBAL_VARS['COUNTRY_CODE']
        replicaNum = GLOBAL_VARS['DEFAULT_DEV_NUM_REPLICA']
        routeHostname = GLOBAL_VARS['ROUTE_MOUNTEBANK_HOSTNAME']
        routePath = "/"
        networkPolicy = GLOBAL_VARS['NETWORK_POLICY_ACCEPT_LABELS']
        vaultSite = GLOBAL_VARS['DEV_VAULT_URL']
        tokenSite = GLOBAL_VARS['DEV_TOKEN_URL']
        namespace_env = namespace_dev
      }
    } else {
      sh "echo http://${GLOBAL_VARS['APP_NAME']}-mountebank.${namespace_dev}.svc:2525 already existing and no change artifact"
    }
  } // End Scopy Deploy Parallel

} // End Parallel deploy