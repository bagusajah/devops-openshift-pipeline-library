#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('goose')
    def label = parameters.get('label', defaultLabel)

    def gooseImage = parameters.get('gooseImage', 'ascendcorphub/goose:v1.0.0')
    def jnlpImage = 'docker.io/openshift/jenkins-agent-maven-35-centos7:v3.10'

    echo "=========================== Image building using buildconfig on openshift ==========================="
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
                name: 'goose', 
                image: "${gooseImage}", 
                command: '/bin/sh -c', 
                args: 'cat', 
                ttyEnabled: true,
                resourceLimitMemory: '512Mi',
                alwaysPullImage: true
            ]
        ],
        volumes: [
            persistentVolumeClaim(claimName: 'pvc-configuration-data', mountPath: '/app-configs'),
            configMapVolume(configMapName: 'global-domain-configmap', mountPath: '/domains'),
            secretVolume(secretName: 'global-certificate-secret', mountPath: '/certs'),
            configMapVolume(configMapName: 'global-countrycode-configmap', mountPath: '/country-code')
        ]
    ) 
    {
        body()
    }
}
