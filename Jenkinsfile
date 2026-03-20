pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JAVA'
    }

    environment {
        COMPOSE_PROJECT_NAME       = 'fintech'
        HOST_APP_PORT              = '8081'
        GIT_SHA                    = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        SONAR_PROJECT_KEY          = 'fintech-api'
        SONAR_TOKEN                = credentials('sonarqube-token')
        SMOKE_TEST_CREDENTIALS_ID  = 'fintech-uat-credentials'
        MAVEN_SETTINGS_ID          = '1cf7f93c-2f77-4b22-8fbc-6422ea025ca5'
        MAVEN_IMAGE                = 'maven:3.9-eclipse-temurin-17'
        MAVEN_CLI_OPTS             = '-B -ntp -s settings.xml -Dmaven.repo.local=/workspace/.m2/repository'
        SONAR_MAVEN_PLUGIN_VERSION = '4.0.0.4121'
    }

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Initial Cleanup') {
            steps {
                dir("${WORKSPACE}") { deleteDir() }
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/francdomain/fnctech_API.git'
            }
        }

        stage('Build Jar') {
            steps {
                configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                    sh 'mvn clean package -B -ntp -s settings.xml -Dmaven.repo.local=${WORKSPACE}/.m2/repository'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv(credentialsId: 'sonarqube-token', installationName: 'SonarQube') {
                    timeout(time: 12, unit: 'MINUTES') {
                        sh '''
                            mvn sonar:sonar -B -ntp -s settings.xml \
                                -Dmaven.repo.local=${WORKSPACE}/.m2/repository \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.login=${SONAR_TOKEN} \
                                -Dsonar.qualitygate.wait=true \
                                -Dsonar.ws.timeout=120
                        '''
                    }
                }
            }
        }

        stage('Build Image') {
            steps {
                withCredentials([
                    file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE'),
                    usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')
                ]) {
                    sh '''
                        cp $ENV_FILE .env
                        docker compose build --no-cache app
                        docker compose up -d db app
                        docker tag $(docker compose images -q app) ${DOCKER_USER}/fnctech-api:${GIT_SHA}
                    '''
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh './scripts/trivy-scan.sh ${DOCKER_USER}/fnctech-api:${GIT_SHA}'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-report.txt', allowEmptyArchive: true
                }
            }
        }

        stage('Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker push ${DOCKER_USER}/fnctech-api:${GIT_SHA}
                        docker logout
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f .env'
            sh 'docker logout || true'
        }
        success {
            echo "Pipeline completed successfully. Build #${BUILD_NUMBER} deployed."
        }
        failure {
            echo "Pipeline failed. Check logs for build #${BUILD_NUMBER}."
        }
    }
}