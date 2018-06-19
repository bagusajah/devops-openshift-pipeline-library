#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('mountebank')
    def label = parameters.get('label', defaultLabel)

    def mountebankImage = parameters.get('mountebankImage', 'vulcanhub/mountebank:v1.1.0')
    def jnlpImage = 'docker.io/openshift/jenkins-agent-maven-35-centos7:v3.10'

    podTemplate(
        cloud: 'openshift',
        label: label,
        serviceAccount: 'jenkins', 
        restartPolicy: 'OnFailure', 
        nodeSelector: 'deployment-nodegroup=openshift-cicd',
        containers: [
            [
                name: 'jnlp', 
                image: "${jnlpImage}", 
                args: '${computer.jnlpmac} ${computer.name}', 
                workingDir: '/home/jenkins/', 
                resourceLimitMemory: '512Mi'
            ],
            [
                name: 'mountebank', 
                image: "${mountebankImage}", 
                command: '/bin/sh -c', 
                args: 'cat', 
                ttyEnabled: true,
                resourceLimitMemory: '512Mi',
                alwaysPullImage: true
            ]
        ],
        volumes: [
        ]
    ) 
    {
        body()
    }
}
