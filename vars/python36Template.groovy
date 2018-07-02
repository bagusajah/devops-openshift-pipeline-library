#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('python36')
    def label = parameters.get('label', defaultLabel)

    def python36Image = parameters.get('python36Image', 'vulcanhub/python36-builder-poc:v1.0.0')
    def robotImage = parameters.get('robotImage', 'vulcanhub/robot:v1.1.0')
    def jmeterImage = parameters.get('jmeterImage', 'vulcanhub/jmeter:v1.0.0')
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
            ],
            [
                name: 'robot', 
                image: "${robotImage}", 
                command: '/bin/sh -c', 
                args: 'cat', 
                ttyEnabled: true,
                resourceLimitMemory: '512Mi',
                alwaysPullImage: true
            ],
            [
                name: 'jmeter', 
                image: "${jmeterImage}", 
                command: '/bin/sh -c', 
                args: 'cat', 
                ttyEnabled: true,
                resourceLimitMemory: '512Mi',
                alwaysPullImage: true
            ]
        ],
        volumes: [
            persistentVolumeClaim(claimName: 'pvc-configuration-data', mountPath: '/app-configs'),
            configMapVolume(configMapName: 'global-countrycode-configmap', mountPath: '/country-code'),
            
            // ---------------------------------------- available only openshift on AWS ----------------------------------------
            configMapVolume(configMapName: 'global-domain-configmap', mountPath: '/domains'),
            secretVolume(secretName: 'global-certificate-secret', mountPath: '/certs')
            // ---------------------------------------- available only openshift on AWS ----------------------------------------

            // ---------------------------------------- waiting openshift on PBI ----------------------------------------
            // --------- partner ---------
            // configMapVolume(configMapName: 'partner-domain-configmap', mountPath: '/partner-domains'),
            // secretVolume(secretName: 'partner-certificate-secret', mountPath: '/partner-certs'),
            // --------- public ---------
            // configMapVolume(configMapName: 'public-domain-configmap', mountPath: '/public-domains'),
            // secretVolume(secretName: 'public-certificate-secret', mountPath: '/public-certs'),
            // --------- private ---------
            // configMapVolume(configMapName: 'private-domain-configmap', mountPath: '/private-domains'),
            // secretVolume(secretName: 'private-certificate-secret', mountPath: '/private-certs')
            // ---------------------------------------- waiting openshift on PBI ----------------------------------------
        ]
    ) 
    {
        body()
    }
}
