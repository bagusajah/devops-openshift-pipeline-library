#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

    def dockerRegistryHost = sh script: "getent hosts docker-registry.default.svc | awk '{print \$1}'", returnStdout: true
  	return dockerRegistryHost

} // End Function
