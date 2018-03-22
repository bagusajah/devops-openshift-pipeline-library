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

    def appScope = GLOBAL_VARS['APP_SCOPE']
    def appLang = GLOBAL_VARS['APP_LANG']
    def countryCode = GLOBAL_VARS['COUNTRY_CODE']
    def appName = GLOBAL_VARS['APP_NAME']
    def packageExtension = GLOBAL_VARS['PACKAGE_EXTENSION']
    // jar, war, tar.gz
    def imageType = config.imageType
    // application, mountebank

    def nameBuildconfig = "${appScope}-${appName}-docker-buildconfig"

    echo "in ACNIMAGEBUILD"

    def dockerRegistry = acnGetDockerRegistryServiceHost()

    sh "sed -i \"s/#APP_SCOPE#/${appScope}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s/#APP_LANG#/${appLang}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s/#APP_VERSION#/${appVersion}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s/#DOCKER_REGISTRY_SERVICE_HOST#/${dockerRegistry}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"

    dir("${directory}/ocp-artifact-${imageType}") {
        if ( imageType == "application" ) {
            sh "sed -i \"s/#APP_VERSION#/${appVersion}/g\" ${directory}/pipeline/dockerfiles/application/Dockerfile"
            sh "sed -i \"s/#COUNTRY_CODE#/${countryCode}/g\" ${directory}/pipeline/dockerfiles/application/Dockerfile"
            sh "cp -rf ${directory}/pipeline/script ${directory}/ocp-artifact-${imageType}/"
            sh "cp ${directory}/pipeline/dockerfiles/application/Dockerfile ${directory}/ocp-artifact-${imageType}/"
            if ( appLang == "springboot" ) {
                sh "cp ${directory}/target/${appName}-${appVersion}.${packageExtension} ${directory}/ocp-artifact-${imageType}/"
            }
        } else if ( imageType == "mountebank" ) {
            acnPrepareFileMountebank{
                global_vars = GLOBAL_VARS
                directory_workspace = directory
            }
            nameBuildconfig = "${appScope}-${appName}-mountebank-docker-buildconfig"
        }
    	
  	} // End directory for prepare buildconfig

    container(name: 'jnlp'){
        sh "oc apply -f ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/${imageType}/buildconfig-docker-image-from-dir.yaml"
        sh "oc start-build ${nameBuildconfig} --from-dir=${directory}/ocp-artifact-${imageType}/ --follow"
    }

} // End Function