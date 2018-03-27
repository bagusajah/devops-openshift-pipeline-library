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
  def appLang = config.appLang
  def result = null
  def mockResult = null

  if ( appLang == "springboot" ) {
  	mockResult = "{\"build\":{\"version\":\"Cannot get version because service not available\"}}"	
  }

  try {
  	result = sh script: "curl -k -X GET ${url}", returnStdout: true
  	result = new JsonSlurperClassic().parseText(result)
  }
  catch(Exception e) {
  	result = new JsonSlurperClassic().parseText(mockResult)
  }

  return result 
}