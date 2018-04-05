#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('deployment')
    def label = parameters.get('label', defaultLabel)

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
            ]
        ],
        volumes: [
            persistentVolumeClaim(claimName: 'configuration-data-pvc', mountPath: '/app-configs')
        ]
    ) 
    {
        body()
    }
}
