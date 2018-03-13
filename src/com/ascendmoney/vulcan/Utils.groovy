package com.ascendmoney.vulcan

import com.cloudbees.groovy.cps.NonCPS

def sayHello() {
    return "Hi, I am vulcan"
}

def environmentNamespace() {
	String ns = getNamespace()
	return ns
}
