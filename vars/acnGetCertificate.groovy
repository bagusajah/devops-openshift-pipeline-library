#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def appScope = config.appScope

    // /certs
    // truemoney-ca-cert.crt
    // truemoney-client-cert.crt
    // truemoney-client-key.key

    def CLIENT_KEY = ""
    def CLIENT_CERT = ""
    def CA_CERT = ""

    // CLIENT_KEY = sh script: "cat /certs/${appScope}-client-key.key", returnStdout: true
    // CLIENT_CERT = sh script: "cat /certs/${appScope}-client-cert.crt", returnStdout: true
    // CA_CERT = sh script: "cat /certs/${appScope}-ca-cert.crt", returnStdout: true

    sh "touch ./tmp/cert.txt"

    sh "echo \"#cert#\" > ./tmp/cert.txt"

    sh "export LC_CTYPE=en_US.UTF-8"
    sh "export LC_ALL=en_US.UTF-8"
    sh "export CLIENT_KEY=$(sed -E \':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g\' /certs/${appScope}-client-key.key)"
    sh "perl -p -i -e \'s/#cert#/$ENV{CLIENT_KEY}/g\' ./tmp/client_key.txt"
    sleep(10000)
  	// key: "#CLIENT_KEY#"
   //    certificate: "#CLIENT_CERT#"
   //    caCertificate: "#CA_CERT#"

    // return domainName

} // End Function
