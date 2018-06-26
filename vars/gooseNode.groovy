#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('goose')
    def label = parameters.get('label', defaultLabel)

    gooseTemplate(parameters) {
        node(label) {
            body()
        }
    }
}
