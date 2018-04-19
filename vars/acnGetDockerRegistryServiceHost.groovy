#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

    def dockerRegistryHost = sh script: "getent hosts docker-registry.default.svc | awk '{print \$1}'", returnStdout: true
    dockerRegistryHost = dockerRegistryHost.trim()
  	return dockerRegistryHost

} // End Function
