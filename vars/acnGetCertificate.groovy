#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def appScope = config.appScope
    def pathFile = config.pathFile

    def pathMounteCert = "/certs"
    
    sh "export LC_CTYPE=en_US.UTF-8"
    sh "export LC_ALL=en_US.UTF-8"
    sh "export CLIENT_KEY=\$(sed -E ':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g' ${pathMounteCert}/${appScope}-client-key.key) ; perl -p -i -e \'s/#CLIENT_KEY#/\$ENV{CLIENT_KEY}/g\' ${pathFile}"
    sh "export CLIENT_CERT=\$(sed -E ':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g' ${pathMounteCert}/${appScope}-client-cert.crt) ; perl -p -i -e \'s/#CLIENT_CERT#/\$ENV{CLIENT_CERT}/g\' ${pathFile}"
    sh "export CA_CERT=\$(sed -E ':a;N;\$!ba;s/\\r{0,1}\\n/\\\\r\\\\n/g' ${pathMounteCert}/${appScope}-client-cert.crt) ; perl -p -i -e \'s/#CA_CERT#/\$ENV{CA_CERT}/g\' ${pathFile}"


} // End Function
