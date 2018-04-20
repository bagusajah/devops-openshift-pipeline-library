#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    def GLOBAL_VARS = config.global_vars
    def directory = config.directory
    def openshiftVersionFolder = config.openshiftVersionFolder
    def appVersion = config.appVersion
    def namespace_cicd = config.namespace

    def appScope = GLOBAL_VARS['APP_SCOPE']
    def appLang = GLOBAL_VARS['APP_LANG']
    def countryCode = GLOBAL_VARS['COUNTRY_CODE']
    def appName = GLOBAL_VARS['APP_NAME']
    def packageExtension = GLOBAL_VARS['PACKAGE_EXTENSION']
    def middlewareName = GLOBAL_VARS['MIDDLEWARE_NAME']
    // jar, war, tar.gz
    def imageType = config.imageType
    def nameImageType = "app"
    def nameBuildconfig = "${appScope}-${appName}-build-${nameImageType}-image-buildconfig"
    if ( imageType == "mountebank" ) {
        nameImageType = "mb"
        nameBuildconfig = "${appScope}-${appName}-build-${nameImageType}-image-buildconfig"
        appName = appName + "-mountebank"
    }
    // application, mountebank
    def imageName = ""

    def dockerRegistry = acnGetDockerRegistryServiceHost()

    sh "sed -i \"s~'#APP_VERSION#'~${appVersion}~g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s~'#DOCKER_REGISTRY_SERVICE_IP#'~${dockerRegistry}~g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"

    dir("${directory}/ocp-artifact-${imageType}") {
        if ( imageType == "application" ) {
            sh "sed -i \"s~'#APP_NAME#'~${appName}~g\" ${directory}/pipeline/dockerfiles/${appLang}/dockerfiles/application/Dockerfile"
            sh "sed -i \"s~'#APP_VERSION#'~${appVersion}~g\" ${directory}/pipeline/dockerfiles/${appLang}/dockerfiles/application/Dockerfile"
            sh "sed -i \"s~'#MIDDLEWARE_NAME#'~${middlewareName}~g\" ${directory}/pipeline/dockerfiles/${appLang}/dockerfiles/application/Dockerfile"
            sh "sed -i \"s~'#PACKAGE_EXTENSION#'~${packageExtension}~g\" ${directory}/pipeline/dockerfiles/${appLang}/dockerfiles/application/Dockerfile"
            sh "sed -i \"s~'#COUNTRY_CODE#'~${countryCode}~g\" ${directory}/pipeline/dockerfiles/${appLang}/dockerfiles/application/Dockerfile" 
            sh "cp -rf ${directory}/pipeline/dockerfiles/${appLang}/script ${directory}/ocp-artifact-${imageType}/"
            sh "cp ${directory}/pipeline/dockerfiles/${appLang}/dockerfiles/application/Dockerfile ${directory}/ocp-artifact-${imageType}/"
            if ( appLang == "springboot" ) {
                sh "ls -lt ${directory}/target/"
                sh "cp ${directory}/target/${appName}-${appVersion}.${packageExtension} ${directory}/ocp-artifact-${imageType}/"
            }
        } else if ( imageType == "mountebank" ) {
            acnPrepareFileMountebank{
                global_vars = GLOBAL_VARS
                directory_workspace = directory
            }
        }
    	
  	} // End directory for prepare buildconfig

    container(name: 'jnlp'){
        sh "oc apply -f ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"
        sh "oc start-build ${nameBuildconfig} --from-dir=${directory}/ocp-artifact-${imageType}/ --follow"
    }

    imageName = "${dockerRegistry}:5000/${namespace_cicd}/${appName}:${appVersion}"
    
    return imageName

} // End Function