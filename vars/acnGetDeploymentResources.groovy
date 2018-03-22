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
    def certName = config.certName ?: "None"
    if ( config.tls_enable == "true" ){
        routeType = 'route-tls'
    }else{
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

    //sh "sed -i \"s/#ROLLING_UPDATE_SURGE#/${rollingUpdateSurge}/g\" pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
    //sh "sed -i \"s/#ROLLING_UPDATE_UNAVAILABLE#/${rollingUpdateUnavailable}/g\" pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"

    def namespace = "acm-cicd" //fix namespaces
    def imageName = "${dockerRegistry}:${dockerRegistryPort}/${namespace}/${config.appName}:${config.appVersion}"
    sh "cat pipeline/${platformType}/${versionOpenshift}/${applicationType}/deploymentconfig.yaml"
    sh "echo replace deployment"
    sh "echo ${imageName}"
    sh "echo ${namespace}"
    def list = """
---
apiVersion: v1
kind: List
items:
"""
    
    //def namespace = utils.getNamespace()
    
    def deploymentYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + "/" + applicationType + "/" + "deploymentconfig.yaml"
    //def deploymentYaml = readFile('pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml')
    // deploymentYaml = deploymentYaml.replaceAll(/#ENV_NAME#/, config.envName)
    // deploymentYaml = deploymentYaml.replaceAll(/#APP_SCOPE#/, config.appScope)
    // deploymentYaml = deploymentYaml.replaceAll(/#APP_LANG#/, config.appLang)
    // deploymentYaml = deploymentYaml.replaceAll(/#SVC_GROUP#/, config.svcGroup)
    // deploymentYaml = deploymentYaml.replaceAll(/#PIPELINE_VERSION#/, config.pipelineVersion)
    // deploymentYaml = deploymentYaml.replaceAll(/#COUNTRY_CODE#/, config.countryCode)
    // deploymentYaml = deploymentYaml.replaceAll(/#APP_VERSION#/, config.appVersion)
    // deploymentYaml = deploymentYaml.replaceAll(/#NUM_OF_REPLICA#/, config.replicaNum)
    // deploymentYaml = deploymentYaml.replaceAll(/#IMAGE_URL#/, imageName)
    // deploymentYaml = deploymentYaml.replaceAll(/#TIMEZONE#/, timeZone)
    // deploymentYaml = deploymentYaml.replaceAll(/#APP_STARTUP_ARGS#/, config.appStartupArgs)
    // deploymentYaml = deploymentYaml.replaceAll(/#VAULT_SITE#/, vaultSite)
    // deploymentYaml = deploymentYaml.replaceAll(/#TOKEN_SITE#/, tokenSite) 
    // deploymentYaml = deploymentYaml.replaceAll(/#RUNWAY_NAME#/, runwayName) + """
    echo deploymentYaml
    echo sh "echo replicaNum ${config.envName}"

    deploymentYaml = deploymentYaml.replaceAll(/#ENV_NAME#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#APP_SCOPE#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#APP_LANG#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#SVC_GROUP#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#PIPELINE_VERSION#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#COUNTRY_CODE#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#APP_VERSION#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#NUM_OF_REPLICA#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#IMAGE_URL#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#TIMEZONE#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#APP_STARTUP_ARGS#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#VAULT_SITE#/, 'xxx')
    deploymentYaml = deploymentYaml.replaceAll(/#TOKEN_SITE#/, 'xxx') 
    deploymentYaml = deploymentYaml.replaceAll(/#RUNWAY_NAME#/, 'xxx') + """
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
    routeYaml = routeYaml.replaceAll(/#ROUTE_HOSTNAME#/, config.routeHostname) 
    routeYaml = routeYaml.replaceAll(/#ROUTE_PATH#/, config.routePath) + """
"""
    sh "echo replace networkpolicy"
    if (networkPolicy != "ALL") {
        def networkpolicyYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + '/application/networkpolicy.yaml'
        networkpolicyYaml = networkpolicyYaml.replaceAll(/#ENV_NAME#/, config.envName) + """
    """
    }


    if (flow.isOpenShift()){
        if (networkPolicy != "ALL") {
            yaml = list + serviceYaml + deploymentYaml + routeYaml + networkpolicyYaml
        } else {
            yaml = list + serviceYaml + deploymentYaml + routeYaml
        }
    } else {
        if (networkPolicy != "ALL") {
            yaml = list + serviceYaml + deploymentYaml + routeYaml + networkpolicyYaml
        } else {
            yaml = list + serviceYaml + deploymentYaml + routeYaml
        }
    }

    echo 'using resources:\n' + yaml
    return yaml

  }
