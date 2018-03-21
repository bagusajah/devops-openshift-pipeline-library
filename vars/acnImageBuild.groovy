#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    def appScope = config.appScope
    def directory = config.directory
    def openshiftVersionFolder = config.openshiftVersion
    def appLang = config.appLang
    def appVersion = config.appVersion
    def countryCode = config.countryCode
    def appName = config.appName
    def dockerRegistry = config.dockerRegistry

    def packageExtension = config.packageExtension
    // jar, war, tar.gz
    def imageType = config.imageType
    // application, mountebank

    def nameBuildconfig = "${GLOBAL_VARS['APP_SCOPE']}-${GLOBAL_VARS['APP_NAME']}-docker-buildconfig"

    sh "sed -i \"s/#APP_SCOPE#/${appScope}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s/#APP_LANG#/${appLang}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s/#APP_VERSION#/${appVersion}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfig-docker-image-from-dir.yaml"
    sh "sed -i \"s/#DOCKER_REGISTRY_SERVICE_HOST#/${dockerRegistry}/g\" ${directory}/pipeline/openshift-artifacts/${openshiftVersionFolder}/buildconfig-docker-image-from-dir.yaml"

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
            acnPrepareFileMountebank()
            nameBuildconfig = "${GLOBAL_VARS['APP_SCOPE']}-${GLOBAL_VARS['APP_NAME']}-mountebank-docker-buildconfig"
        }
    	
  	} // End directory for prepare buildconfig

    container(name: 'jnlp'){
        sh "oc apply -f ${directory}/pipeline/openshift-artifacts/${openshiftVersion}/${imageType}/buildconfig-docker-image-from-dir.yaml"
        sh "oc start-build ${nameBuildconfig} --from-dir=${directory}/ocp-artifact-${imageType}/ --follow"
    }

} // End Function