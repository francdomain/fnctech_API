pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JAVA'
    }

    environment {
        COMPOSE_PROJECT_NAME      = 'fintech'
        HOST_APP_PORT             = '8081'
        SONAR_PROJECT_KEY         = 'fintech-api'
        SMOKE_TEST_CREDENTIALS_ID = 'fintech-uat-credentials'
        MAVEN_SETTINGS_ID         = '1cf7f93c-2f77-4b22-8fbc-6422ea025ca5'
        MAVEN_IMAGE               = 'maven:3.9-eclipse-temurin-17'
        MAVEN_CLI_OPTS            = '-B -ntp -s settings.xml -Dmaven.repo.local=/workspace/.m2/repository'
        SONAR_MAVEN_PLUGIN_VERSION = '4.0.0.4121'
    }

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Initial Cleanup') {
            steps {
                dir("${WORKSPACE}") {
                    deleteDir()
                }
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/francdomain/fnctech_API.git'
            }
        }

        stage("build jar") {
            steps {
                configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                    sh 'mvn clean package -B -ntp -s settings.xml -Dmaven.repo.local=${WORKSPACE}/.m2/repository'
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv(credentialsId: 'sonarqube-token', installationName: 'SonarQube') {
                        timeout(time: 12, unit: 'MINUTES') { 
                            sh '''
                                mvn sonar:sonar -B -ntp -s settings.xml -Dmaven.repo.local=${WORKSPACE}/.m2/repository -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.qualitygate.wait=true \
                                -Dsonar.ws.timeout=120
                            '''
                        }
                    }
                }
            }
        }

        stage('Build & Push Image') {
            steps {
                withCredentials([file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE')]) {
                    sh 'cp $ENV_FILE .env'
                    sh 'docker compose up -d db app'
                }

                script {
                    echo "Pushing image to Docker Hub..."
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            docker tag fnctech-api:latest ${DOCKER_USER}/fnctech-api:${BUILD_NUMBER}
                            docker push ${DOCKER_USER}/fnctech-api:${BUILD_NUMBER}
                            docker logout
                        '''
                    }
                }
            }
        }

        // stage('Deploy') {
        //     steps {
        //         script {
        //             echo "Deploying the application..."
        //             // withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        //             //     sh '''
        //             //         echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
        //             //         docker pull ${DOCKER_USER}/fnctech-api:${BUILD_NUMBER}
        //             //         docker compose down
        //             //         IMAGE_TAG=${BUILD_NUMBER} docker compose up -d
        //             //         docker logout
        //             //     '''
        //             // }
        //         }
        //     }
        // }
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

