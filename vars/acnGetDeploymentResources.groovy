#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import java.io.File

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def utils = new Utils()

    def expose = config.exposeApp ?: 'true'
    def yaml
    def platformType = 'openshift-artifacts'

    def applicationType = 'application'
    def appName = config.appName
    if ( appName.contains("mountebank") ){
        applicationType = 'mountebank'
    }
    def dockerRegistryHost = acnGetDockerRegistryServiceHost()
    def dockerRegistryPort = 5000
    def versionOpenshift = config.versionOpenshift
    def networkPolicy = config.networkPolicy
    def timeZone = config.timeZone ?: "Etc/UTC"
    def runwayName = config.runwayName ?: "OPENSHIFT"
    def namespace_env = config.namespace_env
    if ( applicationType != 'mountebank' ) {
        if ( config.appProtocal == "https" ){
            routeType = 'route-tls'
        }else{
            routeType = 'route'
        }
    }
    else
    {
        routeType = 'route'
    }
    def replicaNum = config.replicaNum
    sh "echo replicaNum ${replicaNum}"
    def rollingUpdateSurge = replicaNum.toInteger() * 2
    sh "echo rollingUpdateSurge ${rollingUpdateSurge}"
    def rollingUpdateUnavailable = 0
    if ( replicaNum.toInteger() > 1 ) {
        rollingUpdateUnavailable = replicaNum.toInteger() / 2
    }
    sh "echo rollingUpdateUnavailable ${rollingUpdateUnavailable}"
    def vaultSite = config.vaultSite ?: "consul.service.th-aws-alpha.consul"
    def tokenSite = config.tokenSite ?: "alp-token.tmn-dev.com"
    def appStartupArgs = config.appStartupArgs ?: "unknown"
    
    if ( applicationType != 'mountebank') {
    sh "sed -i \"s/#ROLLING_UPDATE_SURGE#/${rollingUpdateSurge}/g\" pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
    sh "sed -i \"s/#ROLLING_UPDATE_UNAVAILABLE#/${rollingUpdateUnavailable}/g\" pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
    } else {
    sh "sed -i \"s/#MOUNTEBANK_SURGE#/${rollingUpdateSurge}/g\" pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
    sh "sed -i \"s/#MOUNTEBANK_UNAVAILABLE#/${rollingUpdateUnavailable}/g\" pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
    }
    sh "echo replace deployment"
    def list = """
---
apiVersion: v1
kind: List
items:
"""
    
    def namespace = config.namespace //fix namespaces
    def imageName = "${dockerRegistryHost}:${dockerRegistryPort}/${namespace}/${config.appName}:${config.appVersion}"
    def deploymentYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + "/" + applicationType + "/" + "deploymentconfig.yaml"

    deploymentYaml = deploymentYaml.replaceAll(/#ENV_NAME#/, config.envName)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_SCOPE#/, config.appScope)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_LANG#/, config.appLang)
    deploymentYaml = deploymentYaml.replaceAll(/#SVC_GROUP#/, config.svcGroup)
    deploymentYaml = deploymentYaml.replaceAll(/#PIPELINE_VERSION#/, config.pipelineVersion)
    deploymentYaml = deploymentYaml.replaceAll(/#COUNTRY_CODE#/, config.countryCode)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_VERSION#/, config.appVersion)
    if ( applicationType != 'mountebank') {
    deploymentYaml = deploymentYaml.replaceAll(/#NUM_OF_REPLICA#/, config.replicaNum)
    } else {
    deploymentYaml = deploymentYaml.replaceAll(/#DEFAULT_MOUNTEBANK_NUM_REPLICA#/, config.replicaNum)
    }
    deploymentYaml = deploymentYaml.replaceAll(/#IMAGE_URL#/, imageName)
    deploymentYaml = deploymentYaml.replaceAll(/#TIMEZONE#/, timeZone)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_STARTUP_ARGS#/, config.appStartupArgs)
    deploymentYaml = deploymentYaml.replaceAll(/#VAULT_SITE#/, vaultSite)
    deploymentYaml = deploymentYaml.replaceAll(/#TOKEN_SITE#/, tokenSite) 
    deploymentYaml = deploymentYaml.replaceAll(/#RUNWAY_NAME#/, runwayName) + """

"""
    sh "echo replace service"
    def serviceYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/"  + versionOpenshift + '/' + applicationType + '/service.yaml'
    serviceYaml = serviceYaml.replaceAll(/#ENV_NAME#/, config.envName)
    serviceYaml = serviceYaml.replaceAll(/#APP_SCOPE#/, config.appScope)
    serviceYaml = serviceYaml.replaceAll(/#APP_LANG#/, config.appLang)
    serviceYaml = serviceYaml.replaceAll(/#SVC_GROUP#/, config.svcGroup)
    serviceYaml = serviceYaml.replaceAll(/#PIPELINE_VERSION#/, config.pipelineVersion)
    serviceYaml = serviceYaml.replaceAll(/#COUNTRY_CODE#/, config.countryCode) + """

"""
    sh "echo replace route"
    def routeYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + '/' + applicationType + '/' + routeType +'.yaml'
    routeYaml = routeYaml.replaceAll(/#ENV_NAME#/, config.envName)
    routeYaml = routeYaml.replaceAll(/#APP_SCOPE#/, config.appScope)
    routeYaml = routeYaml.replaceAll(/#APP_LANG#/, config.appLang)
    routeYaml = routeYaml.replaceAll(/#SVC_GROUP#/, config.svcGroup)
    routeYaml = routeYaml.replaceAll(/#PIPELINE_VERSION#/, config.pipelineVersion)
    routeYaml = routeYaml.replaceAll(/#COUNTRY_CODE#/, config.countryCode)
    if ( applicationType != 'mountebank') {
    routeYaml = routeYaml.replaceAll(/#ROUTE_PATH#/, config.routePath)
     }
    routeYaml = routeYaml.replaceAll(/#ROUTE_HOSTNAME#/, config.routeHostname) + """
"""
    sh "echo replace networkpolicy"
    if (networkPolicy != "ALL") {
    sh "echo test if"
    def networkpolicyYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + '/application/networkpolicy.yaml'
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#ENV_NAME#/, config.envName) 
    networkpolicyYaml = routeYaml.replaceAll(/#ENV_NAME#/, config.envName)
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#APP_SCOPE#/, config.appScope)
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#APP_LANG#/, config.appLang)
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#SVC_GROUP#/, config.svcGroup)
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#PIPELINE_VERSION#/, config.pipelineVersion)
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#COUNTRY_CODE#/, config.countryCode)+ """
"""
    } //End replace netwockpolicy

    sh "echo merge atifacts"
        if (networkPolicy != "ALL") {
            yaml = list + serviceYaml + deploymentYaml + routeYaml + networkpolicyYaml
        } else {
            yaml = list + serviceYaml + deploymentYaml + routeYaml
        }
    

    echo 'using resources:\n' + yaml
    // return yaml

    applyResource {
        artifact_data = yaml
        namespace_env = namespace_env
        applicationType  = applicationType
    }
    

} // End Main Method

def applyResource(body){

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def artifact = config.artifact_data
    def namespace = config.namespace_env
    def application = config.applicationType

    container(name: 'jnlp'){
        acnApplyResources { 
            artifact_data = artifact
            namespace_env = namespace
            applicationType = application
        }
    }
}