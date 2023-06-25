#!groovy

pipeline {
    environment {
        JAVA_HOME = "/usr/lib/jvm/java-17-openjdk-amd64/"
        CI = "false"
        MY_ENV = sh(
            returnStdout: true,
            script: 'if [[ $BRANCH_NAME =~ "release-" ]]; then echo "prod"; elif [[ $BRANCH_NAME =~ (sprint-|autotest-) ]]; then echo "qa"; else echo "dev"; fi'
        ).trim()
        MY_VERSION = sh(
            returnStdout: true, 
            script: 'if [[ $BRANCH_NAME =~ (release-|sprint-|autotest-) ]]; then echo "${BRANCH_NAME}"; else echo "${BRANCH_NAME}.${BUILD_ID}-SNAPSHOT"; fi'
        ).trim()
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(artifactNumToKeepStr: '3'))
        copyArtifactPermission('deployment')
    }
    agent any
    stages {
        stage('Prepare build') { 
            steps {
                script {
                if (env.BRANCH_NAME.startsWith('sprint-') || env.BRANCH_NAME.startsWith('release-') || env.BRANCH_NAME.startsWith('autotest-')) {
                    sh 'cp ./prebid-server/src/main/resources/application.yml.k8s ./prebid-server/src/main/resources/application.yml'
                    }
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    sh "echo ${BRANCH_NAME} ${GIT_BRANCH} ${GIT_COMMIT} ${MY_VERSION} ${MY_ENV}"

                    if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('release-')) {
                        sh "mvn clean package -Drevision=${MY_VERSION}"
                    } else {
                        sh "mvn clean package -Dmaven.test.skip=true -Drevision=${MY_VERSION}"
                    }
                }
            }
        }
        stage('Deploy to dev') {
            when { branch 'develop' }
            steps{   
                dir('admaru-infrastructure') {
                    git branch: 'main', credentialsId: 'admaru-infrastructure', url: 'git@github.com:Admaru-dev/admaru-infrastructure.git'
                }
                    script {
                        ansiblePlaybook(
                        playbook: "admaru-infrastructure/ansible/prebid-server.yml",
                        inventory: 'admaru-infrastructure/ansible/admaru.aws_ec2.yml',
                        credentialsId: 'admaru-ansible',
                        disableHostKeyChecking: true,
                        extraVars: [
                            hosts: "applications_${env.MY_ENV}",
                            envName: "${env.MY_ENV}",
                            artifactPath: "${env.WORKSPACE}/target/prebid-server.jar",
                            configPath: "${env.WORKSPACE}/config/prebid-config.yaml"
                        ])
                    }
            }
        }
        stage('Build and push docker images') {
            when { anyOf { tag "sprint-*"; tag "release-*" } }
            steps {
                script {
                     docker.withRegistry('https://780577742507.dkr.ecr.ap-northeast-2.amazonaws.com', 'ecr:ap-northeast-2:jenkins_ecr') {
                         def dockerImage = docker.build("admaru/prebid-server:${MY_VERSION}", "--build-arg BUILD_ID=${MY_VERSION} --build-arg APP_NAME=prebid-server -f docker/Dockerfile-config-server ${WORKSPACE}")
                         dockerImage.push()
                         dockerImage.push('latest')
                     }
                }
            }
        }
    }
    post {
        always {
            script{
                if (env.BRANCH_NAME == 'develop') {
                    archiveArtifacts artifacts: '**/target/prebid-server*.jar',  onlyIfSuccessful: false
                }
            }
            script {
                if (env.BRANCH_NAME == 'develop' || env.BRANCH_NAME =~ 'sprint-') {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml'
                    recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
                    recordIssues enabledForFailure: true, tool: spotBugs()
                }
            }
        }
    }
}
