#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import java.io.File

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

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
    def app_scope = config.appScope
    def route_hostname = config.routeHostname
    def envName = config.envName
    def appVersion = config.appVersion
    def imageName = config.imageName
    def replicaNum = config.replicaNum
    def countryCode = config.country_code
    def pathFileRoute = ""
    def responseDeploy = ""
    def routeType = ""

    def certList = []
    // ["CLIENT_KEY", "CLIENT_CERT", "CA_CERT"]

    def domainName = acnGetDomainName{
        appScope = app_scope
        domainNamePrefix = route_hostname
    }

    if ( envName == "staging" || envName == "production" ) {
        // Check PVC existing
        responseGetPVC = ""
        container(name: 'jnlp'){
            responseGetPVC = sh script: "oc get persistentvolumeclaim -l appName=${appName} -n ${namespace_env}", returnStdout: true
        }
        if ( !responseGetPVC.contains(appName) ) {
            responseDeploy = applyResourceYaml {
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/pvc.yaml"
                namespaceEnv = namespace_env
                kind = "pvc"
                app_name = appName
            }
            if ( responseDeploy == "error" ) {
                error "Pipeline failure stage: DEPLOY PVC"
            } 

        } else {
            // Check network policy existing
            responseGetNetworkPolicy = ""
            container(name: 'jnlp'){
                responseGetNetworkPolicy = sh script: "oc get networkpolicy -l appName=${appName} -n ${namespace_env}", returnStdout: true
            }
            if ( !responseGetNetworkPolicy.contains(appName) ) {
                sh "sed -i \"s~'#ENV_NAME#'~${envName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                responseDeploy = applyResourceYaml {
                    pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                    namespaceEnv = namespace_env
                    kind = "networkpolicy"
                    app_name = appName
                }
            }

            // Check autoscaling existing
            // responseGetAutoscale = sh script: "oc get networkpolicy -l appName=${appName} -n ${namespace_env}", returnStdout: true
            // if ( !responseGetNetworkPolicy.contains(appName) ) {
            //     responseDeploy = applyResourceYaml {
            //         pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
            //         namespaceEnv = namespace_env
            //         kind = "networkpolicy"
            //         app_name = appName
            //     }
            // }

            // Check route existing
            routeType = "route"
            responseGetRoute = ""
            container(name: 'jnlp'){
                responseGetRoute = sh script: "oc get route -l appName=${appName} -n ${namespace_env}", returnStdout: true
            }
            if ( !responseGetRoute.contains(appName) ) {
                if ( routeTLSEnable == "true" ) {
                    routeType = "route-tls"
                }
                responseDeploy = applyResourceYaml {
                    pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                    namespaceEnv = namespace_env
                    kind = "route"
                    app_name = appName
                }
            }
        }
    } else {
        // Deploy PVC
        if ( applicationType != 'mountebank' && forceDeployList[7] == "true" ) {
            responseDeploy = applyResourceYaml {
                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/pvc.yaml"
                namespaceEnv = namespace_env
                kind = "pvc"
                app_name = appName
            }
            if ( responseDeploy == "error" ) {
                error "Pipeline failure stage: DEPLOY PVC"
            }
        }
        
        if ( responseDeploy == "success" || forceDeployList[7] == "false" ) {
            // Deploy AUTOSCALING
            if ( applicationType != 'mountebank' && forceDeployList[8] == "true" ) {
                echo "WAITING"
                // responseDeploy = applyResourceYaml {
                //     pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/autoscaling.yaml"
                //     namespaceEnv = namespace_env
                //     kind = "WAITNG"
                //     app_name = appName
                // }
            }

            // Deploy NETWORK POLICY
            if ( applicationType != 'mountebank' && forceDeployList[6] == "true" ) {
                sh "sed -i \"s~'#ENV_NAME#'~${envName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                sh "cat ${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                responseDeploy = applyResourceYaml {
                    pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                    namespaceEnv = namespace_env
                    kind = "networkpolicy"
                    app_name = appName
                }
            } else if ( applicationType != 'mountebank' && forceDeployList[6] == "false" ) {
                responseGetNetworkPolicy = ""
                container(name: 'jnlp'){
                    responseGetNetworkPolicy = sh script: "oc get networkpolicy -l appName=${appName} -n ${namespace_env}", returnStdout: true
                }
                if ( !responseGetNetworkPolicy.contains(appName) ) {
                    responseDeploy = applyResourceYaml {
                        pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                        namespaceEnv = namespace_env
                        kind = "networkpolicy"
                        app_name = appName
                    }
                }
            }

            // Deploy ROUTE
            echo "start route"
            routeType = "route"
            if ( applicationType != 'mountebank' && forceDeployList[9] == "true" ) {
                if ( routeTLSEnable == "true" ) {
                    routeType = "route-tls"
                }
                certList = acnGetCertificate{
                    appScope = app_scope
                    pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                }
                sh "sed -i \"s~'#COUNTRY_CODE#'~${countryCode}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                sh "sed -i \"s~#ENV_NAME#~${envName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                sh "sed -i \"s~'#ROUTE_HOSTNAME#'~${domainName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                responseDeploy = applyResourceYaml {
                    pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                    namespaceEnv = namespace_env
                    kind = "route"
                    app_name = appName
                }
            } else if ( applicationType != 'mountebank' && forceDeployList[9] == "false" ) {
                responseGetRoute = ""
                container(name: 'jnlp'){
                    responseGetRoute = sh script: "oc get route -l appName=${appName} -n ${namespace_env}", returnStdout: true
                }
                if ( !responseGetRoute.contains(appName) ) {
                    if ( routeTLSEnable == "true" ) {
                        routeType = "route-tls"
                    }
                    certList = acnGetCertificate{
                        appScope = app_scope
                        pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                    }
                    sh "sed -i \"s~'#COUNTRY_CODE#'~${countryCode}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                    sh "sed -i \"s~#ENV_NAME#~${envName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                    sh "sed -i \"s~'#ROUTE_HOSTNAME#'~${domainName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/${routeType}.yaml"
                    responseDeploy = applyResourceYaml {
                        pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/application/networkpolicy.yaml"
                        namespaceEnv = namespace_env
                        kind = "route"
                        app_name = appName
                    }
                }
            } else if ( applicationType == 'mountebank' ) {
                if ( namespace_env.contains("dev") ) {
                    sh "sed -i \"s~#ENV_NAME#~${envName}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/route.yaml"
                    sh "sed -i \"s~'#MB_ROUTE_HOSTNAME#'~${domainName}~g\" pipeline/${platformType}/${versionOpenshift}/mountebank/route.yaml"
                    sh "sed -i \"s~'#COUNTRY_CODE#'~${countryCode}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/route.yaml"
                    container(name: 'jnlp'){
                        responseGetRoute = sh script: "oc get route -l appName=${appName} -n ${namespace_env}", returnStdout: true
                        if ( !responseGetRoute.contains(appName) ) {
                            echo "Route ${appName} is not existing"
                            responseDeploy = applyResourceYaml {
                                pathFile = "${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/route.yaml"
                                namespaceEnv = namespace_env
                                kind = "route"
                                app_name = appName
                            }
                        } else {
                            echo "Route ${appName} is existing"
                        }
                    }
                }
            }
        } // Condition do only PVC not fail or not deploy PVC
    } // Condition [dev, qa, performance], [staging, production]
        
    def rollingUpdateSurge = replicaNum.toInteger() * 2
    def rollingUpdateUnavailable = 0
    if ( replicaNum.toInteger() > 1 ) {
        rollingUpdateUnavailable = replicaNum.toInteger() / 2
    }
    
    if ( applicationType != 'mountebank') {
        sh "sed -i \"s~'#NUM_OF_REPLICA#'~${replicaNum}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
        sh "sed -i \"s~'#ROLLING_UPDATE_SURGE#'~${rollingUpdateSurge}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
        sh "sed -i \"s~'#ROLLING_UPDATE_UNAVAILABLE#'~${rollingUpdateUnavailable}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/application/deploymentconfig.yaml"
    } else {
        sh "sed -i \"s~'#DEFAULT_NUM_REPLICA_MB#'~${replicaNum}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
        sh "sed -i \"s~'#MOUNTEBANK_SURGE#'~${rollingUpdateSurge}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
        sh "sed -i \"s~'#MOUNTEBANK_UNAVAILABLE#'~${rollingUpdateUnavailable}~g\" ${directory}/pipeline/${platformType}/${versionOpenshift}/mountebank/deploymentconfig.yaml"
    }

    def list = """
---
apiVersion: v1
kind: List
items:
"""
    def deploymentYaml = readFile encoding: 'UTF-8', file: directory + "/pipeline/" + platformType + "/" + versionOpenshift + "/" + applicationType + "/" + "deploymentconfig.yaml"
    deploymentYaml = deploymentYaml.replaceAll(/'#ENV_NAME#'/, envName)
    deploymentYaml = deploymentYaml.replaceAll(/'#APP_VERSION#'/, appVersion)
    deploymentYaml = deploymentYaml.replaceAll(/'#IMAGE_URL#'/, imageName)
    deploymentYaml = deploymentYaml.replaceAll(/'#BUILD_HASH#'/, gitHashApplication)
    deploymentYaml = deploymentYaml.replaceAll(/'#GIT_SOURCE_BRANCH#'/, gitSourceBranch)
    deploymentYaml = deploymentYaml.replaceAll(/'#COUNTRY_CODE#'/, countryCode)
    deploymentYaml = deploymentYaml.replaceAll(/'#RUNWAY_NAME#'/, runwayName) + """

"""
    def serviceYaml = readFile encoding: 'UTF-8', file: directory + "/pipeline/" + platformType + "/"  + versionOpenshift + '/' + applicationType + '/service.yaml'
    serviceYaml = serviceYaml.replaceAll(/'#COUNTRY_CODE#'/, countryCode)
    serviceYaml = serviceYaml.replaceAll(/'#ENV_NAME#'/, envName) + """

"""
    echo "merge atifacts"
    yaml = list + serviceYaml + deploymentYaml

    echo 'using resources:\n' + yaml

    applyResourceNonYaml {
        artifact_data = yaml
        namespaceEnv = namespace_env
        application  = applicationType
    }
    
} // End Main Method

def applyResourceYaml(body){

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def pathFile = config.pathFile
    def namespaceEnv = config.namespaceEnv
    def kind = config.kind
    def app_name = config.app_name
    def responseDeploy = ""

    container(name: 'jnlp'){
        sh "oc delete ${kind} -l appName=${app_name} -n ${namespaceEnv}"
        responseDeploy = sh script: "oc apply -f ${pathFile} -n ${namespaceEnv}", returnStdout: true
    }

    responseDeployConclude = "success"
    if ( responseDeploy.contains("error") ) {
        responseDeployConclude = "error"
    }
    
    return responseDeployConclude;
}

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
