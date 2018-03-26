#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils
import java.io.File



def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def utils = new Utils()

   def artifact_data = config.artifact_data
   def namespace =  config.namespace
   def applicationType =  config.applicationType

   writeFile encoding: 'UTF-8', file: 'tmp_artifact_data_${applicationType}.yaml', text: artifact_data
   sh "ls -lt"
   sh "oc apply -f tmp_artifact_data_${applicationType}.yaml -n ${namespace}"
}