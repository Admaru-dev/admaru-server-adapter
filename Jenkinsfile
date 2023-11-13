#!groovy

pipeline {
    environment {
        JAVA_HOME = "/usr/lib/jvm/java-17-openjdk-amd64/"
        CI = "false"
        MY_ENV = sh(
            returnStdout: true,
            script: 'if [[ $BRANCH_NAME =~ "admaru-" ]]; then echo "admaru"; else echo "dev"; fi'
        ).trim()
        MY_VERSION = sh(
            returnStdout: true, 
            script: 'if [[ $BRANCH_NAME =~ "admaru-" ]]; then echo "${BRANCH_NAME}"; else echo "${BRANCH_NAME}.${BUILD_ID}-SNAPSHOT"; fi'
        ).trim()
        MY_TAG = "admaru"
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
                    // sh 'cp ./src/main/resources/bidder-config/admaru.yaml.${MY_ENV} ./src/main/resources/bidder-config/admaru.yaml'
                    sh 'cp ./config/prebid-server-config.yaml.${MY_ENV} ./config/prebid-server-config.yaml'
                }
            }
        }
        stage('Build') {
            when { anyOf { branch 'develop'; tag "admaru-*"} }
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
                            hosts: "apps_${env.MY_ENV}",
                            envName: "${env.MY_ENV}",
                            artifactPath: "${env.WORKSPACE}/target/prebid-server.jar",
                            configPath: "${env.WORKSPACE}/config"
                        ])
                    }
            }
        }
        stage('Build and push docker images') {
            when { anyOf { tag "admaru-*"} }
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
        stage('Generate tag') {
            when { anyOf { branch "main"} }
            steps {
                script {
                    TAG = VersionNumber(versionNumberString: '${MY_TAG}-${BUILD_DATE_FORMATTED, "yyyy.MM.dd"}_${BUILDS_TODAY}')
                        withEnv(["MY_VERSION=${TAG}"]) {
                            sshagent(credentials: ['admaru-server-adapter']) {
                                sh 'echo "Tagging with ${MY_VERSION}"'
                                sh "git tag ${MY_VERSION}"
                                sh 'git push origin --tags'
                            }
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
                    if (env.BRANCH_NAME != 'develop') {
                        cleanWs(cleanWhenAborted: true, cleanWhenFailure: true, cleanWhenNotBuilt: true, cleanWhenSuccess: true, cleanWhenUnstable: true, deleteDirs: true)
                    }
                }
        }
    }
}
