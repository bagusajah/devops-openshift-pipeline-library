#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def appScope = config.appScope

    def pathMounteCert = "/certs"
    def CLIENT_KEY = ""
    def CLIENT_CERT = ""
    def CA_CERT = ""
    def certificates = []
    // ["CLIENT_KEY", "CLIENT_CERT", "CA_CERT"]
    sh "touch ./client_key.txt ; echo \"#cert#\" > ./client_key.txt"
    sh "touch ./client_cert.txt ; echo \"#cert#\" > ./client_cert.txt"
    sh "touch ./ca_cert.txt ; echo \"#cert#\" > ./ca_cert.txt"
    sh "export LC_CTYPE=en_US.UTF-8"
    sh "export LC_ALL=en_US.UTF-8"
    CLIENT_KEY = sh script: "export CLIENT_KEY=\$(sed -E ':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g' ${pathMounteCert}/${appScope}-client-key.key) ; perl -p -i -e \'s/#cert#/\$ENV{CLIENT_KEY}/g\' ./client_key.txt ; cat ./client_key.txt", returnStdout: true
    CLIENT_CERT = sh script: "export CLIENT_CERT=\$(sed -E ':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g' ${pathMounteCert}/${appScope}-client-cert.crt) ; perl -p -i -e \'s/#cert#/\$ENV{CLIENT_CERT}/g\' ./client_cert.txt ; cat ./client_cert.txt", returnStdout: true
    CA_CERT = sh script: "export CA_CERT=\$(sed -E ':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g' ${pathMounteCert}/${appScope}-client-cert.crt) ; perl -p -i -e \'s/#cert#/\$ENV{CA_CERT}/g\' ./ca_cert.txt ; cat ./ca_cert.txt", returnStdout: true
    certificates.add(CLIENT_KEY)
    certificates.add(CLIENT_CERT)
    certificates.add(CA_CERT)

    return certificates

} // End Function
