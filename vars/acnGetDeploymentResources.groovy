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
    deploymentYaml = deploymentYaml.replaceAll(/#APP_VERSION#/, config.appVersion)
    if ( applicationType != 'mountebank') {
    deploymentYaml = deploymentYaml.replaceAll(/#NUM_OF_REPLICA#/, config.replicaNum)
    } else {
    deploymentYaml = deploymentYaml.replaceAll(/#DEFAULT_NUM_REPLICA_MB#/, config.replicaNum)
    }
    deploymentYaml = deploymentYaml.replaceAll(/#IMAGE_URL#/, imageName)
    deploymentYaml = deploymentYaml.replaceAll(/#RUNWAY_NAME#/, runwayName) + """

"""
    sh "echo replace service"
    def serviceYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/"  + versionOpenshift + '/' + applicationType + '/service.yaml'
    serviceYaml = serviceYaml.replaceAll(/#ENV_NAME#/, config.envName) + """

"""
    sh "echo replace route"
    def routeYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + '/' + applicationType + '/' + routeType +'.yaml'
    routeYaml = routeYaml.replaceAll(/#ENV_NAME#/, config.envName)
    routeYaml = routeYaml.replaceAll(/#ROUTE_HOSTNAME#/, config.routeHostname) + """
"""
    sh "echo replace networkpolicy"
    if (networkPolicy != "ALL") {
    sh "echo test if"
    def networkpolicyYaml = readFile encoding: 'UTF-8', file: "pipeline/" + platformType + "/" + versionOpenshift + '/application/networkpolicy.yaml'
    networkpolicyYaml = networkpolicyYaml.replaceAll(/#ENV_NAME#/, config.envName) 
    networkpolicyYaml = routeYaml.replaceAll(/#ENV_NAME#/, config.envName) + """
"""
    } //End replace networkpolicy

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
        namespaceEnv = namespace_env
        application  = applicationType
    }
    

} // End Main Method

def applyResource(body){

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def artifact = config.artifact_data
    def namespaceEnv = config.namespaceEnv
    def application = config.application

    container(name: 'jnlp'){
        acnApplyResources { 
            artifact_data = artifact
            namespace = namespaceEnv
            applicationType = application
        }
    }
}