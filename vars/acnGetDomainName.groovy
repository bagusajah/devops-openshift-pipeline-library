#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def appScope = config.appScope
    def domainNamePrefix = config.domainNamePrefix
    def routerSharding = config.routerSharding

    def domainName = ""
    def domainNameSuffix = ""

    // ---------------------------------------- available only openshift on AWS ----------------------------------------
    domainNameSuffix = sh script: "cat /domains/${appScope}-domain.txt", returnStdout: true
    // ---------------------------------------- available only openshift on AWS ----------------------------------------

    // ---------------------------------------- waiting openshift on PBI ----------------------------------------
    // domainNameSuffix = sh script: "cat /${routerSharding}-domains/domain.txt", returnStdout: true
    // ---------------------------------------- waiting openshift on PBI ----------------------------------------

    domainNameSuffix = domainNameSuffix.trim()
    domainName = domainNamePrefix + "." + domainNameSuffix

  	return domainName

} // End Function
