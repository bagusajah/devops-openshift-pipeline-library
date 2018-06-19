#!/usr/bin/groovy
import groovy.json.*
import java.text.SimpleDateFormat
import groovy.json.JsonSlurperClassic

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def urlWebhook = config.urlWebhook
    def envName = config.envName
    def stageCurrent = config.stageCurrent
    echo "appName ${config.appName}"
    def appName = config.appName

    String jsonAppnameTopic = "\"title\": \"Application Name\""
    String jsonAppnameValue = "\"subtitle\": \"${appName}\""
    String jsonImageJenkinsLogo = "\"imageUrl\": \"https://s3-ap-southeast-1.amazonaws.com/openshift-distributed-artifacts/scripts/jenkins.png\""
    String jsonJobnameTopic = "\"topLabel\": \"Job Name\""
    String jsonJobnameValue = "\"content\": \"${env.JOB_NAME}\""
    String jsonBuildNumberTopic = "\"topLabel\": \"Build Number\""
    String jsonBuildNumberValue = "\"content\": \"${env.BUILD_NUMBER}\""
    String jsonPipelineStageTopic = "\"topLabel\": \"Pipeline Stage\""
    String jsonPipelineStageValue = "\"content\": \"${stageCurrent}\""
    String jsonJenkinsJobUrlTopic = "\"text\": \"Click For More Information\""
    String jsonJenkinsJobUrlValue = "\"url\": \"${env.BUILD_URL}\""

    String jsonStr = "{ \"cards\": [ { \"header\": { ${jsonAppnameTopic}, ${jsonAppnameValue}, ${jsonImageJenkinsLogo} }, \"sections\": [ { \"widgets\": [ { \"keyValue\": { ${jsonJobnameTopic}, ${jsonJobnameValue} }}, { \"keyValue\": { ${jsonBuildNumberTopic}, ${jsonBuildNumberValue} }}, { \"keyValue\": { ${jsonPipelineStageTopic}, ${jsonPipelineStageValue} }} ] }, { \"widgets\": [{ \"buttons\": [{ \"textButton\": { ${jsonJenkinsJobUrlTopic}, \"onClick\": { \"openLink\": { ${jsonJenkinsJobUrlValue} } } } }] }] } ] } ] }"
    def object = new JsonBuilder(new JsonSlurper().parseText(jsonStr)).toPrettyString()

    sh "curl -kX POST \"${urlWebhook}\" -H \"content-type: application/json\" -d '${object}'"

} // End Function