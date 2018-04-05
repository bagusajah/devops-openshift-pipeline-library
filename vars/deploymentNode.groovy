#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('deployment')
    def label = parameters.get('label', defaultLabel)

    deploymentTemplate(parameters) {
        node(label) {
            body()
        }
    }
}