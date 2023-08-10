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
                    sh 'cp ./src/main/resources/bidder-config/admaru.yaml.${MY_ENV} ./src/main/resources/bidder-config/admaru.yaml'
                    sh 'cp ./config/prebid-server-config.yaml.${MY_ENV} ./config/prebid-server-config.yaml'
                }
            }
        }
        stage('Build') {
            when { anyOf { branch 'develop'; tag "sprint-*"; tag "release-*" } }
            steps {
                script {
                    sh "echo ${BRANCH_NAME} ${GIT_BRANCH} ${GIT_COMMIT} ${MY_VERSION} ${MY_ENV}"
                    sh "mvn clean package -Dmaven.test.skip=true -Drevision=${MY_VERSION}"
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
                            configPath: "${env.WORKSPACE}/config"
                        ])
                    }
            }
        }
        stage('Build and push docker images') {
            when { anyOf { tag "sprint-*"; tag "release-*" } }
            steps {
                script {
                     docker.withRegistry('https://780577742507.dkr.ecr.ap-northeast-2.amazonaws.com', 'ecr:ap-northeast-2:jenkins_ecr') {
                         def dockerImage = docker.build("admaru/prebid-server:${MY_VERSION}", "--build-arg APP_NAME=prebid-server -f docker/Dockerfile-prebid-server ${WORKSPACE}")
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
        }
    }
}
