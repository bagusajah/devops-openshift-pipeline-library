#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('python36')
    def label = parameters.get('label', defaultLabel)

    def python36Image = parameters.get('python36Image', 'vulcanhub/python36-builder-poc:v1.0.0')
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
                name: 'python36', 
                image: "${python36Image}", 
                command: '/bin/sh -c', 
                args: 'cat', 
                ttyEnabled: true, 
                workingDir: '/home/jenkins/',
                resourceLimitMemory: '1024Mi',
                alwaysPullImage: true
            ]
        ],
        volumes: [
            persistentVolumeClaim(claimName: 'pvc-configuration-data', mountPath: '/app-configs'),
            configMapVolume(configMapName: 'global-domain-configmap', mountPath: '/domains'),
            configMapVolume(configMapName: 'global-countrycode-configmap', mountPath: '/country-code'),
            secretVolume(secretName: 'global-certificate-secret', mountPath: '/certs')
        ]
    ) 
    {
        body()
    }
}
