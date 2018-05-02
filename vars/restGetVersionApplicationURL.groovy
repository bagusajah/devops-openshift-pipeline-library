#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurperClassic

def call(body) {

  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def url = config.url
  def result = null
  def mockResult = null

  mockResult = "{\"build\":{\"version\":\"Cannot get version because service not available\"}}"	

  try {
  	result = sh script: "curl -k -X GET ${url}", returnStdout: true
  	result = new JsonSlurperClassic().parseText(result)
  }
  catch(Exception e) {
  	result = new JsonSlurperClassic().parseText(mockResult)
  }

  return result 
}