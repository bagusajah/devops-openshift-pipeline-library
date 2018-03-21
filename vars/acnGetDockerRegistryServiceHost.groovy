#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

    def dockerRegistryHost = sh script: "getent hosts docker-registry.default.svc", returnStdout: true
  	dockerRegistryHost = dockerRegistryHost.split("      ")[0]
  	return dockerRegistryHost

} // End Function
