// pipeline {
//     agent any

//     environment {
//         // Non-secret config
//         COMPOSE_PROJECT_NAME      = 'fintech'
//         DOCKERHUB_REPO            = 'francdocmain/fnctech-api'
//         HOST_APP_PORT             = '8081'
//         SONARQUBE_SERVER          = 'SonarQube'
//         SONAR_PROJECT_KEY         = 'fintech-api'
//         DOCKER_CREDENTIALS_ID     = 'dockerhub-credentials'
//         SMOKE_TEST_CREDENTIALS_ID = 'fintech-uat-credentials'
//         SONAR_TOKEN_CREDENTIAL_ID  = 'sonarqube-token'
//         MAVEN_SETTINGS_ID         = '11e2101e-5b3d-4afa-894f-834c2cfacd33'
//         MAVEN_IMAGE               = 'maven:3.9-eclipse-temurin-17'
//         MAVEN_CLI_OPTS            = '-B -ntp -s settings.xml -Dmaven.repo.local=/workspace/.m2/repository'
//         SONAR_MAVEN_PLUGIN_VERSION = '4.0.0.4121'
//     }

//     options {
//         disableConcurrentBuilds()
//         skipDefaultCheckout(true)
//     }

//     stages {
//         stage('Checkout') {
//             steps {
//                 git branch: 'main', url: 'https://github.com/francdomain/fnctech_API.git'
//             }
//         }

//         stage('Prepare Environment') {
//             steps {
//                 // .env is only needed by docker-compose for DB credentials at runtime
//                 withCredentials([file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE_PATH')]) {
//                     sh 'cp "$ENV_FILE_PATH" .env && chmod 600 .env'
//                 }
//                 script {
//                     env.IMAGE_BUILD  = "${env.DOCKERHUB_REPO}:${env.BUILD_NUMBER}"
//                     env.IMAGE_LATEST = "${env.DOCKERHUB_REPO}:latest"
//                 }
//             }
//         }

//         stage('CI') {
//             stages {
//                 stage('Build') {
//                     steps {
//                         configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                             sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} clean compile'
//                         }
//                     }
//                 }

//                 stage('Unit Test') {
//                     steps {
//                         configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                             sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} verify'
//                         }
//                     }
//                     post {
//                         always {
//                             junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
//                         }
//                     }
//                 }

//                 stage('Package') {
//                     steps {
//                         configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                             sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} package -DskipTests'
//                         }
//                     }
//                 }
//             }
//         }

//         stage('Code Quality') {
//             stages {
//                 stage('SonarQube Analysis') {
//                     steps {
//                         configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                             withSonarQubeEnv("${SONARQUBE_SERVER}") {
//                                 withCredentials([string(credentialsId: env.SONAR_TOKEN_CREDENTIAL_ID, variable: 'SONAR_TOKEN')]) {
//                                     timeout(time: 12, unit: 'MINUTES') {
//                                         sh '''
//                                             docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} \
//                                               mvn ${MAVEN_CLI_OPTS} org.sonarsource.scanner.maven:sonar-maven-plugin:${SONAR_MAVEN_PLUGIN_VERSION}:sonar \
//                                               -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
//                                               -Dsonar.host.url=$SONAR_HOST_URL \
//                                               -Dsonar.login=$SONAR_TOKEN \
//                                               -Dsonar.qualitygate.wait=true \
//                                               -Dsonar.ws.timeout=120
//                                         '''
//                                     }
//                                 }
//                             }
//                         }
//                     }
//                 }

//                 // Keep quality gate enforcement in scanner with -Dsonar.qualitygate.wait=true.
//                 // We intentionally avoid waitForQualityGate because webhook is not configured.
//             }
//         }

//         stage('Docker') {
//             stages {
//                 stage('Docker Build') {
//                     steps {
//                         configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                             sh '''
//                                 for i in 1 2 3 4 5; do
//                                     docker pull eclipse-temurin:17-jre-jammy && break
//                                     echo "Pull attempt $i failed, retrying in 15s..."
//                                     sleep 15
//                                 done
//                                 docker compose build app
//                                 docker tag fintech-app:latest ${IMAGE_BUILD}
//                                 docker tag fintech-app:latest ${IMAGE_LATEST}
//                             '''
//                         }
//                     }
//                 }
//             }
//         }

//         stage('Release') {
//             stages {
//                 stage('Deploy') {
//                     steps {
//                         sh '''
//                             docker compose up -d db app
//                         '''
//                     }
//                 }

//                 stage('Smoke Test') {
//                     steps {
//                         sh '''
//                             APP_HOST_PORT=$(docker compose port app 8080 | awk -F: '{print $NF}' | tr -d '\r')
//                             if [ -z "$APP_HOST_PORT" ]; then
//                                 echo "Smoke test failed: could not resolve published port for app:8080"
//                                 docker compose ps || true
//                                 docker compose logs --tail=200 app || true
//                                 exit 1
//                             fi

//                             echo "Resolved app host port: $APP_HOST_PORT"
//                             for i in $(seq 1 30); do
//                                 status=$(curl -s -o /dev/null -w "%{http_code}" \
//                                     -X POST http://localhost:${APP_HOST_PORT}/api/auth/login \
//                                     -H "Content-Type: application/json" -d '{}' || true)
//                                 if [ "$status" != "000" ]; then
//                                     echo "App reachable. HTTP status: $status"
//                                     exit 0
//                                 fi
//                                 echo "Waiting for app... attempt $i"
//                                 sleep 2
//                             done
//                             echo "Smoke test failed: app not reachable after 180s"
//                             docker compose ps || true
//                             docker compose logs --tail=200 app || true
//                             exit 1
//                         '''
//                     }
//                 }

//                 stage('UAT') {
//                     steps {
//                         catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
//                             withCredentials([usernamePassword(credentialsId: "${env.SMOKE_TEST_CREDENTIALS_ID}", usernameVariable: 'UAT_EMAIL', passwordVariable: 'UAT_PASSWORD')]) {
//                                 sh '''
//                                     APP_HOST_PORT=$(docker compose port app 8080 | awk -F: '{print $NF}' | tr -d '\r')
//                                     if [ -z "$APP_HOST_PORT" ]; then
//                                         echo "UAT failed: could not resolve published port for app:8080"
//                                         docker compose ps || true
//                                         exit 1
//                                     fi

//                                     status=$(curl -s -o /tmp/uat_response.json -w "%{http_code}" \
//                                         -X POST http://localhost:${APP_HOST_PORT}/api/auth/login \
//                                         -H "Content-Type: application/json" \
//                                         -d "{\"email\":\"${UAT_EMAIL}\",\"password\":\"${UAT_PASSWORD}\"}" || true)
//                                     if [ "$status" = "200" ]; then
//                                         echo "UAT passed"
//                                     else
//                                         echo "UAT failed: expected 200, got $status"
//                                         cat /tmp/uat_response.json || true
//                                         exit 1
//                                     fi
//                                 '''
//                             }
//                         }
//                     }
//                 }

//                 stage('Push to Docker Hub') {
//                     steps {
//                         withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
//                             sh '''
//                                 echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
//                                 docker push ${IMAGE_BUILD}
//                                 docker push ${IMAGE_LATEST}
//                                 docker logout
//                             '''
//                         }
//                     }
//                 }
//             }
//         }
//     }

//     post {
//         always {
//             sh 'docker compose stop app || true'
//             sh 'rm -f .env settings.xml || true'
//         }
//         success {
//             echo "Pipeline succeeded. Pushed ${IMAGE_BUILD} and ${IMAGE_LATEST}"
//         }
//         failure {
//             echo 'Pipeline failed. Check stage logs for details.'
//         }
//     }
// }


pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    environment {
        COMPOSE_PROJECT_NAME = 'fintech'
    }
    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Initial cleanup') {
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
                echo "building the application..."
                sh 'mvn package'
            }
        }
        stage('SonarQube analysis') {
            steps {
                script {
                    withSonarQubeEnv(credentialsId: 'sonarqube-token', installationName: 'SonarQube') {
                        sh 'mvn clean package sonar:sonar -Dsonar.qualitygate.wait=true -Dsonar.ws.timeout=120'
                    }
                }
            }
        }
        // stage("Quality Gate"){
        //     timeout(time: 12, unit: 'MINUTES') {
        //         def qg = waitForQualityGate()
        //         if (qg.status != 'OK') {
        //             error "Pipeline aborted due to quality gate failure: ${qg.status}"
        //         }
        //     }
        // }
        stage("build image") {
            steps {
                steps {
                        sh '''
                            docker compose up -d db app
                        '''
                    }
                script {
                    echo "pushing image to docker hub..."
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            docker tag ${DOCKER_USER}/fnctech-api:${BUILD_NUMBER}
                            docker push ${DOCKER_USER}/fnctech-api:${BUILD_NUMBER}
                            docker logout
                        '''
                    }
                }
            }
        }
        stage("deploy") {
            steps {
                script {
                    echo "deploying the application..."
                }
            }
        }
    }
}

