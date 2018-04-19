#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('maven')
    def label = parameters.get('label', defaultLabel)

    def mavenImage = parameters.get('mavenImage', 'vulcanhub/maven-builder:v1.0.0')
    def robotImage = parameters.get('robotImage', 'vulcanhub/robot:v1.1.0')
    def jmeterImage = parameters.get('jmeterImage', 'vulcanhub/jmeter:v1.0.0')
    def jnlpImage = 'docker.io/openshift/jenkins-agent-maven-35-centos7:v3.10'
    //def inheritFrom = parameters.get('inheritFrom', 'base')

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
                resourceLimitMemory: '64Mi'
            ],
            [
                name: 'maven', 
                image: "${mavenImage}", 
                command: '/bin/sh -c', 
                args: 'cat', 
                ttyEnabled: true, 
                workingDir: '/home/jenkins/',
                envVars: [
                    envVar(key: 'MAVEN_OPTS', value: '-Xmx512m -XX:MaxPermSize=350m -Duser.home=/root/ -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn')
                ],
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
            secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
            persistentVolumeClaim(claimName: 'jenkins-mvn-local-repo', mountPath: '/root/.mvnrepository'),
            persistentVolumeClaim(claimName: 'pvc-configuration-data', mountPath: '/app-configs'),
            configMapVolume(configMapName: 'global-domain-configmap', mountPath: '/domains'),
            secretVolume(secretName: 'global-certificate-secret', mountPath: '/certs')
        ]
    ) 
    {
        body()
    }
}
