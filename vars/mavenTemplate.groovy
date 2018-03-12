#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('maven')
    def label = parameters.get('label', defaultLabel)

    def mavenImage = parameters.get('mavenImage', 'ascendcorphub/maven-builder:v1.0.0')
    def robotImage = parameters.get('robotImage', 'ascendcorphub/robot:v1.1.0')
    def jnlpImage = (flow.isOpenShift()) ? 'fabric8/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'
    def inheritFrom = parameters.get('inheritFrom', 'base')

    def cloud = flow.getCloudConfig()

    // 0.13 introduces a breaking change when defining pod env vars so check version before creating build pod
    if (utils.isKubernetesPluginVersion013()) {
        echo "Kubernetes Plugin Version 013"
        if (utils.isUseOpenShiftS2IForBuilds()) {
            podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}", serviceAccount: 'jenkins',
                    containers: [
                            containerTemplate(
                                    name: 'jnlp',
                                    image: "${jnlpImage}",
                                    args: '${computer.jnlpmac} ${computer.name}',
                                    workingDir: '/home/jenkins/',
                                    resourceLimitMemory: '512Mi')],
                    volumes: [
                            secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
                            persistentVolumeClaim(claimName: 'jenkins-mvn-local-repo', mountPath: '/root/.mvnrepository'),
                            secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                            secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                            secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                            secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git')]) {

                body(

                )
            }
        } else {
            echo "building using the docker socket"

            podTemplate(cloud: cloud,
                    label: label,
                    inheritFrom: "${inheritFrom}",
                    containers: [
                            containerTemplate (
                                    //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}'],
                                    name: 'maven',
                                    image: "${mavenImage}",
                                    command: '/bin/sh -c',
                                    args: 'cat',
                                    ttyEnabled: true,
                                    alwaysPullImage: false,
                                    workingDir: '/home/jenkins/',
                                    //resourceLimitMemory: '640Mi',
                                    envVars: [
                                            //envVar(key: '_JAVA_OPTIONS', value: '-Duser.home=/root/ -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dsun.zip.disableMemoryMapping=true -XX:+UseParallelGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xms10m -Xmx192m'),
                                        envVar(key: 'MAVEN_OPTS', value: '-Duser.home=/root/ -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'),
                                        envVar(key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/')
                                    ]
                            )
                    ],
                    volumes: [
                            secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
                            secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                            secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                            secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                            secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                            secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git'),
                            hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                            persistentVolumeClaim(mountPath: '/root/data/pvc', claimName: 'nfs-robot', readOnly: false),
                            nfsVolume(mountPath: '/home/jenkins/workspace', serverAddress: '35.200.192.59', serverPath: '/var/nfsshare', readOnly: false),
                            hostPathVolume(hostPath: '/home/jenkins/workspace', mountPath: '/home/jenkins/workspace')])
                    {

                        body(

                        )
                    }
        }
    } else {
        if (utils.isUseOpenShiftS2IForBuilds()) {
            echo "=========================== Image building using the docker socket on openshift ==========================="
            podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}", serviceAccount: 'jenkins', restartPolicy: 'OnFailure', nodeSelector: 'deployment-nodegroup=fabric8',
                    containers: [
                            [name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}', workingDir: '/home/jenkins/',
                             resourceLimitMemory: '512Mi'], // needs to be high to work on OSO
                            [name: 'maven', image: "${mavenImage}", command: '/bin/sh -c', args: 'cat', ttyEnabled: true, workingDir: '/home/jenkins/', privileged: true,
                             envVars: [
                                     [key: '_JAVA_OPTIONS', value: '-Duser.home=/root/ -XX:+UseParallelGC -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xmx256m'],
                                     [key: 'MAVEN_OPTS', value: '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn']
                                     ],
                             resourceLimitMemory: '1024Mi']],
                    volumes: [secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
                              persistentVolumeClaim(claimName: 'jenkins-mvn-local-repo', mountPath: '/root/.mvnrepository'),
                              secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                              secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                              secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                              secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                              secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git'),
                              hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
                    envVars: [[key: 'GIT_COMMITTER_EMAIL', value: 'fabric8@googlegroups.com'], [key: 'GIT_COMMITTER_NAME', value: 'fabric8'], [key: 'DOCKER_HOST', value: 'unix:/var/run/docker.sock'], [key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/']]) {

                body(

                )
            }
        } else {
            echo "=========================== Image building using the docker socket ==========================="

            podTemplate(
                    cloud: cloud, 
                    label: label, 
                    inheritFrom: "${inheritFrom}",
                    serviceAccount: 'jenkins',
                    containers: [
                            [
                                name: 'maven', 
                                image: "${mavenImage}", 
                                command: '/bin/sh -c', 
                                args: 'cat', 
                                ttyEnabled: true,
                                envVars: [
                                    [
                                        key: 'MAVEN_OPTS', 
                                        value: '-Duser.home=/root/ -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
                                    ]

                                ],
                                alwaysPullImage: true,
                                privileged: true
                            ],
                            [
                                name: 'robot', 
                                image: "${robotImage}", 
                                command: '/bin/sh -c', 
                                args: 'cat', 
                                ttyEnabled: true,
                                alwaysPullImage: true
                            ]
                    ],
                    volumes: [
                        secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
                        persistentVolumeClaim(claimName: 'jenkins-mvn-local-repo', mountPath: '/root/.mvnrepository'),
                        secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                        secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                        secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                        secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                        secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git'),
                        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
                    ],
                    envVars: [
                        [key: 'DOCKER_HOST', value: 'unix:/var/run/docker.sock'], [key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/']
                    ]
            ) {

                body(

                )
            }
        }
    }
}
