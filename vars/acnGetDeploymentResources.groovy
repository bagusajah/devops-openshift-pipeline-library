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
    
    def forceDeployList = config.forceDeployList
    def directory = config.directoryWorkspace
    def versionOpenshift = config.versionOpenshift
    def networkPolicy = config.networkPolicy
    def runwayName = config.runwayName ?: "OPENSHIFT"
    def namespace_env = config.namespace_env
    def gitHashApplication = config.gitHashApplication
    def gitSourceBranch = config.gitSourceBranch
    def routeTLSEnable = config.routeTLSEnable
    def pathFileRoute = ""
    def responseDeploy = ""

    echo "START DEPLOYMENT"

    def certList = []
    // ["CLIENT_KEY", "CLIENT_CERT", "CA_CERT"]

    def domainName = acnGetDomainName{
        appScope = config.appScope
        domainNamePrefix = config.routeHostname
    }

    // Deploy PVC
    if ( applicationType != 'mountebank' && forceDeployList[7] == "true" ) {
        responseDeploy = applyResourceYaml {
            pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/pvc.yaml"
            namespaceEnv = namespace_env
        }
        if ( responseDeploy == "error" ) {
            error "Pipeline failure stage: DEPLOY PVC"
        }
    }
    
    if ( responseDeploy == "success" || forceDeployList[7] == "false" ) {
        // Deploy NETWORK POLICY
        if ( applicationType != 'mountebank' && forceDeployList[6] == "true" ) {
            responseDeploy = applyResourceYaml {
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                namespaceEnv = namespace_env
            }
        }
        // Deploy AUTOSCALING
        if ( applicationType != 'mountebank' && forceDeployList[8] == "true" ) {
            responseDeploy = applyResourceYaml {
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/autoscaling.yaml"
                namespaceEnv = namespace_env
            }
        }
        // Deploy ROUTE
        routeType = 'route'
        if ( applicationType != 'mountebank' && forceDeployList[9] == "true" ) {
            routeType = "route-tls"
            certList = acnGetCertificate{
                appScope = config.appScope
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
            }
            sh "sed -i \"s~#ENV_NAME#~${config.envName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
            sh "sed -i \"s~'#ROUTE_HOSTNAME#'~${domainName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
            responseDeploy = applyResourceYaml {
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                namespaceEnv = namespace_env
            }
        } else if ( applicationType == 'mountebank' ) {
            sh "sed -i \"s~'#MB_ROUTE_HOSTNAME#'~${domainName}~g\" pipeline/${platformType}/${versionOpenshift}/mountebank/route.yaml"
            responseDeploy = applyResourceYaml {
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/route.yaml"
                namespaceEnv = namespace_env
            }
        }
    }

    def replicaNum = config.replicaNum
    def rollingUpdateSurge = replicaNum.toInteger() * 2
    def rollingUpdateUnavailable = 0
    if ( replicaNum.toInteger() > 1 ) {
        rollingUpdateUnavailable = replicaNum.toInteger() / 2
    }
    
    if ( applicationType != 'mountebank') {
        sh "sed -i \"s~'#ROLLING_UPDATE_SURGE#'~${rollingUpdateSurge}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
        sh "sed -i \"s~'#ROLLING_UPDATE_UNAVAILABLE#'~${rollingUpdateUnavailable}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
    } else {
        sh "sed -i \"s~'#MOUNTEBANK_SURGE#'~${rollingUpdateSurge}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
        sh "sed -i \"s~'#MOUNTEBANK_UNAVAILABLE#'~${rollingUpdateUnavailable}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
    }
    sh "echo replace deployment"

    def list = """
---
apiVersion: v1
kind: List
items:
"""
    
    def imageName = config.imageName
    def deploymentYaml = readFile encoding: 'UTF-8', file: directory + "/pipeline/" + platformType + "/" + versionOpenshift + "/" + applicationType + "/" + "deploymentconfig.yaml"
    deploymentYaml = deploymentYaml.replaceAll(/'#ENV_NAME#'/, config.envName)
    deploymentYaml = deploymentYaml.replaceAll(/'#APP_VERSION#'/, config.appVersion)
    if ( applicationType != 'mountebank') {
    deploymentYaml = deploymentYaml.replaceAll(/'#NUM_OF_REPLICA#'/, config.replicaNum)
    } else {
    deploymentYaml = deploymentYaml.replaceAll(/'#DEFAULT_NUM_REPLICA_MB#'/, config.replicaNum)
    }
    deploymentYaml = deploymentYaml.replaceAll(/'#IMAGE_URL#'/, imageName)
    deploymentYaml = deploymentYaml.replaceAll(/'#BUILD_HASH#'/, gitHashApplication)
    deploymentYaml = deploymentYaml.replaceAll(/'#GIT_SOURCE_BRANCH#'/, gitSourceBranch)
    deploymentYaml = deploymentYaml.replaceAll(/'#RUNWAY_NAME#'/, runwayName) + """

"""
    sh "echo replace service"
    def serviceYaml = readFile encoding: 'UTF-8', file: directory + "/pipeline/" + platformType + "/"  + versionOpenshift + '/' + applicationType + '/service.yaml'
    serviceYaml = serviceYaml.replaceAll(/'#ENV_NAME#'/, config.envName) + """

"""
    } //End replace networkpolicy

    sh "echo merge atifacts"
    yaml = list + serviceYaml + deploymentYaml

    echo 'using resources:\n' + yaml

    applyResourceNonYaml {
        artifact_data = yaml
        namespaceEnv = namespace_env
        application  = applicationType
    }
    
} // End Main Method

def applyResourceNonYaml(body){

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

def applyResourceYaml(body){

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def pathFile = config.pathFile
    def namespaceEnv = config.namespaceEnv

    container(name: 'jnlp'){
        responseDeploy = sh script: "oc apply -f ${pathFile} -n ${namespaceEnv}", returnStdout: true
    }

    String error = String.valueOf(responseDeploy.contains("error"));
    
    responseDeployConclude = error == "true" ? "error" : "success"
    return responseDeployConclude;
}