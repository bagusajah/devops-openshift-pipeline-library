#!/usr/bin/groovy
import java.io.File
import groovy.json.*
import java.text.SimpleDateFormat
import groovy.json.JsonSlurperClassic

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def namespace = config.namespace
  def trackingVersionList = config.dataTrackingVersionList
  def globalVariablesList = config.globalVariablesList
  def envName = config.envName
  def appVersion = config.appVersion
  def gitHashOpenshiftConfiguration = config.gitHashOpenshiftConfiguration ?: "waiting"
  def gitHashEcsConfiguration = config.gitHashEcsConfiguration ?: "waiting"
  def gitHashTesseractConfiguration = config.gitHashTesseractConfiguration ?: "waiting"
  def resultPipeline = config.resultPipeline
  def startTime = config.startTime
  def endTime = config.endTime
  def envOpenshift = config.envOpenshift

  def countSendToElastic = "1"
  def envList = ["dev", "qa", "performance"]
  def flagFail = ""

  if ( appVersion == "FAIL" ) {
    // FAIL_dev
    flagFail = "FAIL"
    countSendToElastic = envName == "dev" ? "2" : envName == "qa" ? "1" : envName == "performance" ? "0" : "waiting"
    def APP_URL_TYPE_SERVICE = new URL("${globalVariablesList['APP_PROTOCOL']}://${globalVariablesList['APP_NAME']}.${envOpenshift}.svc${globalVariablesList['PATH_INFO']}")
    def rs = restGetVersionApplicationURL{
      url = APP_URL_TYPE_SERVICE
    }
    appVersion = rs.build.version
  }

  // ["git-tag-application", "git-author-application", "git-hash-application", "rerun-condition", "build-artifacts-only-and-skip-openshift-deploy", "build-on-branch"]
  if ( trackingVersionList[3] == "ignore" && trackingVersionList[4] == "true" ) {
    appVersion = "Service not deploy (build artifacts only and skip openshift deploy)"
  }

  if ( trackingVersionList[3] == "ignore" || trackingVersionList[3] == "dev" ) {
    if ( globalVariablesList['DEPLOY_DEV_ONLY'] == "true" && ( envName == "qa" || envName == "performance" ) ) {
      appVersion = "Service not deploy (deploy dev only)"
    }
  }

  String jsonAppname = "\"app-name\": \"${globalVariablesList['APP_NAME']}\""
  String jsonJobname = "\"job-name\": \"${env.JOB_NAME}\""
  String jsobBuildnumber = "\"build-number\": ${env.BUILD_NUMBER}"
  String jsonEnvname = "\"env-name\": \"${envName}\""
  String jsonReruncondition = "\"rerun-condition\": \"${trackingVersionList[3]}\""
  String jsonBuildartifactsonlyandskipopenshiftdeploy = "\"build-artifacts-only-and-skip-openshift-deploy\": \"${trackingVersionList[4]}\""
  String jsonDeploydevonly = "\"deploy-dev-only\": \"${globalVariablesList['DEPLOY_DEV_ONLY']}\""
  String jsonGittag = "\"git-tag-application\": \"${trackingVersionList[0]}\""  
  String jsonGitauthorapplication = "\"git-author-application\": \"${trackingVersionList[1]}\""
  String jsonGithashapplication = "\"git-hash-application\": \"${trackingVersionList[2]}\""
  String jsonRunwayname = "\"runway-name\": \"${globalVariablesList['RUNWAY_NAME']}\""
  String jsonAppversion = "\"app-version\": \"${appVersion}\""
  String jsonGithashopenshiftconfig = "\"git-hash-openshift-configuration\": \"${gitHashOpenshiftConfiguration}\""
  String jsonGithashecsconfig = "\"git-hash-ecs-configuration\": \"${gitHashEcsConfiguration}\""
  String jsonGithashtesseractconfig = "\"git-hash-tesseract-configuration\": \"${gitHashTesseractConfiguration}\""
  String jsonResult = "\"result\": \"${resultPipeline}\""
  String jsonStarttime = "\"start-time\": \"${startTime}\""
  String jsonEndtime = "\"end-time\": \"${endTime}\""
  String jsonBuildOnBranch = "\"build-on-branch\": \"${trackingVersionList[5]}\""
  String jsonStr = "{ ${jsonAppname}, ${jsonJobname}, ${jsobBuildnumber}, ${jsonEnvname}, ${jsonReruncondition}, ${jsonBuildartifactsonlyandskipopenshiftdeploy}, ${jsonDeploydevonly}, ${jsonGittag}, ${jsonGitauthorapplication}, ${jsonGithashapplication}, ${jsonRunwayname}, ${jsonAppversion}, ${jsonGithashopenshiftconfig}, ${jsonGithashecsconfig}, ${jsonGithashtesseractconfig}, ${jsonResult}, ${jsonStarttime}, ${jsonEndtime}, ${jsonBuildOnBranch} }"
  def object = new JsonBuilder(new JsonSlurper().parseText(jsonStr)).toPrettyString()
  
  sh "curl -X POST http://elasticsearch.${namespace}.svc:9200/tracking-version/tweet -H \"content-type: application/json\" -d '${object}'"

  if ( flagFail == "FAIL" ) {
    def n = 2
    for ( i = 0; i < countSendToElastic.toInteger(); i++ ) {
      if ( trackingVersionList[3] == "ignore" || trackingVersionList[3] == "dev" ) {
        if ( globalVariablesList['DEPLOY_DEV_ONLY'] == "true" && ( envList[n] == "qa" || envList[n] == "performance" ) ) {
          appVersion = "Service not deploy (deploy dev only)"
        }
      }
      jsonAppname = "\"app-name\": \"${globalVariablesList['APP_NAME']}\""
      jsonJobname = "\"job-name\": \"${env.JOB_NAME}\""
      jsobBuildnumber = "\"build-number\": ${env.BUILD_NUMBER}"
      jsonEnvname = "\"env-name\": \"${envList[n]}\""
      jsonReruncondition = "\"rerun-condition\": \"${trackingVersionList[3]}\""
      jsonBuildartifactsonlyandskipopenshiftdeploy = "\"build-artifacts-only-and-skip-openshift-deploy\": \"${trackingVersionList[4]}\""
      jsonDeploydevonly = "\"deploy-dev-only\": \"${globalVariablesList['DEPLOY_DEV_ONLY']}\""
      jsonGittag = "\"git-tag-application\": \"${trackingVersionList[0]}\"" 
      jsonGitauthorapplication = "\"git-author-application\": \"${trackingVersionList[1]}\""
      jsonGithashapplication = "\"git-hash-application\": \"${trackingVersionList[2]}\""
      jsonRunwayname = "\"runway-name\": \"${globalVariablesList['RUNWAY_NAME']}\""
      jsonAppversion = "\"app-version\": \"${appVersion}\""
      jsonGithashopenshiftconfig = "\"git-hash-openshift-configuration\": \"${gitHashOpenshiftConfiguration}\""
      jsonGithashecsconfig = "\"git-hash-ecs-configuration\": \"${gitHashEcsConfiguration}\""
      jsonGithashtesseractconfig = "\"git-hash-tesseract-configuration\": \"${gitHashTesseractConfiguration}\""
      jsonResult = "\"result\": \"${resultPipeline}\""
      jsonStarttime = "\"start-time\": \"${startTime}\""
      jsonEndtime = "\"end-time\": \"${endTime}\""
      jsonBuildOnBranch = "\"build-on-branch\": \"${trackingVersionList[5]}\""
      jsonStr = "{ ${jsonAppname}, ${jsonJobname}, ${jsobBuildnumber}, ${jsonEnvname}, ${jsonReruncondition}, ${jsonBuildartifactsonlyandskipopenshiftdeploy}, ${jsonDeploydevonly}, ${jsonGittag}, ${jsonGitauthorapplication}, ${jsonGithashapplication}, ${jsonRunwayname}, ${jsonAppversion}, ${jsonGithashopenshiftconfig}, ${jsonGithashecsconfig}, ${jsonGithashtesseractconfig}, ${jsonResult}, ${jsonStarttime}, ${jsonEndtime}, ${jsonBuildOnBranch} }"
      object = new JsonBuilder(new JsonSlurper().parseText(jsonStr)).toPrettyString()
      
      sh "curl -X POST http://elasticsearch.${namespace}.svc:9200/tracking-version/tweet -H \"content-type: application/json\" -d '${object}'"
      n--
    }
  }

} // End Elastic Search