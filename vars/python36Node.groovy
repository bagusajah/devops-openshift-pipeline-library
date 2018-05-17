#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('python36')
    def label = parameters.get('label', defaultLabel)

    python36Template(parameters) {
        node(label) {
            body()
        }
    }
}