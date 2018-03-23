#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurperClassic

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def url = config.url
  def result = sh script: "curl -k -X GET ${url}", returnStdout: true
  return result
}