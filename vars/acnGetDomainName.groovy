#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def appScope = config.appScope
    def domainNamePrefix = config.domainNamePrefix

    def domainName = ""
    def domainNameSuffix = ""

    // appScope = truemoney

    // /domains/
    // equator-domain.txt
    // truemoney-domain.txt
    domainNameSuffix = sh script: "cat /domains/${appScope}-domain.txt", returnStdout: true
    domainName = domainNamePrefix + "." + domainNameSuffix

  	return domainName

} // End Function
