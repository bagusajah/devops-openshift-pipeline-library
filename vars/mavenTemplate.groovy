#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('maven')
    def label = parameters.get('label', defaultLabel)

    def mavenImage = parameters.get('mavenImage', 'vulcanhub/maven-builder:v1.0.0')
    def robotImage = parameters.get('robotImage', 'ascendcorphub/robot:v1.1.0')
    def jnlpImage = 'docker.io/openshift/jenkins-agent-maven-35-centos7:v3.10'
    def inheritFrom = parameters.get('inheritFrom', 'base')

    echo "=========================== Image building using buildconfig on openshift ==========================="
    podTemplate(
        cloud: 'openshift',
        label: label, 
        inheritFrom: "${inheritFrom}",
        serviceAccount: 'jenkins', 
        restartPolicy: 'OnFailure', 
        nodeSelector: 'deployment-nodegroup=vulcan',
        containers: [
            // [
            //     name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}', workingDir: '/home/jenkins/', resourceLimitMemory: '512Mi'
            // ],
            [
                name: 'maven', image: "${mavenImage}", command: '/bin/sh -c', args: 'cat', ttyEnabled: true, workingDir: '/home/jenkins/',
                envVars: [
                    envVar(key: 'MAVEN_OPTS', value: '-Duser.home=/root/ -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn')
                ],
                resourceLimitMemory: '1024Mi'
            ]
        ],
        volumes: [
            secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
            persistentVolumeClaim(claimName: 'jenkins-maven-pvc', mountPath: '/root/.mvnrepository'),
            persistentVolumeClaim(claimName: 'configuration-data-pvc', mountPath: '/app-configs')
        ]
    ) 
    {
        body()
    }
}
