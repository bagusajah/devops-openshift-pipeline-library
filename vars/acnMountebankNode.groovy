#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('mountebank')
    def label = parameters.get('label', defaultLabel)

    mountebankTemplate(parameters) {
        node(label) {
            body()
        }
    }
}
