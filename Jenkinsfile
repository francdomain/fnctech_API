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
//         timestamps()
//     }

//     stages {
//         stage('Checkout') {
//             steps {
//                 git branch: 'main', url: 'https://github.com/francdomain/fnctech_API.git'
//             }
//         }

//         stage('Prepare') {
//             steps {
//                 withCredentials([file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE_PATH')]) {
//                     sh 'cp "$ENV_FILE_PATH" .env && chmod 600 .env'
//                 }
//                 script {
//                     def latestTag = sh(returnStdout: true, script: """
//                         git fetch --tags --force >/dev/null 2>&1 || true
//                         git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n 1
//                     """).trim()

//                     if (!latestTag) {
//                         env.IMAGE_VERSION = 'v1.0.0'
//                     } else {
//                         def matcher = latestTag =~ /^v(\d+)\.(\d+)\.(\d+)$/
//                         if (!matcher.matches()) {
//                             error("Latest version tag has invalid format: ${latestTag}. Expected format: vMAJOR.MINOR.PATCH")
//                         }
//                         env.IMAGE_VERSION = "v${matcher[0][1]}.${matcher[0][2]}.${matcher[0][3].toInteger() + 1}"
//                     }
//                     env.IMAGE_VERSIONED = "${env.DOCKERHUB_REPO}:${env.IMAGE_VERSION}"
//                 }
//             }
//         }

//         stage('Build and Test') {
//             steps {
//                 configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                     sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} clean verify'
//                 }
//             }
//             post {
//                 always {
//                     junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
//                 }
//             }
//         }

//         stage('Quality and Image Build') {
//             parallel {
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

//                 stage('Docker Build') {
//                     steps {
//                         configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
//                             sh '''
//                                 docker compose build --pull app
//                                 docker tag fintech-app:latest ${IMAGE_VERSIONED}
//                             '''
//                         }
//                     }
//                 }
//             }
//         }

//         stage('Deploy') {
//             steps {
//                 sh 'docker compose up -d db app'
//             }
//         }

//         stage('Validate') {
//             steps {
//                 script {
//                     env.APP_HOST_PORT = sh(returnStdout: true, script: "docker compose port app 8080 | awk -F: '{print \\$NF}' | tr -d '\\r'").trim()
//                     if (!env.APP_HOST_PORT) {
//                         sh 'docker compose ps || true'
//                         sh 'docker compose logs --tail=200 app || true'
//                         error('Validation failed: could not resolve published port for app:8080')
//                     }
//                 }

//                 sh '''
//                     echo "Resolved app host port: ${APP_HOST_PORT}"
//                     for i in $(seq 1 30); do
//                         status=$(curl -s -o /dev/null -w "%{http_code}" \
//                             -X POST http://localhost:${APP_HOST_PORT}/api/auth/login \
//                             -H "Content-Type: application/json" -d '{}' || true)
//                         if [ "$status" != "000" ]; then
//                             echo "App reachable. HTTP status: $status"
//                             exit 0
//                         fi
//                         echo "Waiting for app... attempt $i"
//                         sleep 2
//                     done
//                     echo "Smoke test failed: app not reachable after 180s"
//                     docker compose ps || true
//                     docker compose logs --tail=200 app || true
//                     exit 1
//                 '''

//                 catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
//                     withCredentials([usernamePassword(credentialsId: "${env.SMOKE_TEST_CREDENTIALS_ID}", usernameVariable: 'UAT_EMAIL', passwordVariable: 'UAT_PASSWORD')]) {
//                         sh '''
//                             status=$(curl -s -o /tmp/uat_response.json -w "%{http_code}" \
//                                 -X POST http://localhost:${APP_HOST_PORT}/api/auth/login \
//                                 -H "Content-Type: application/json" \
//                                 -d "{\"email\":\"${UAT_EMAIL}\",\"password\":\"${UAT_PASSWORD}\"}" || true)
//                             if [ "$status" = "200" ]; then
//                                 echo "UAT passed"
//                             else
//                                 echo "UAT failed: expected 200, got $status"
//                                 cat /tmp/uat_response.json || true
//                                 exit 1
//                             fi
//                         '''
//                     }
//                 }
//             }
//         }

//         stage('Push to Docker Hub') {
//             steps {
//                 withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
//                     sh '''
//                         echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
//                         docker push ${IMAGE_VERSIONED}
//                         docker logout
//                     '''
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
//             echo "Pipeline succeeded. Pushed ${IMAGE_VERSIONED}"
//         }
//         failure {
//             echo 'Pipeline failed. Check stage logs for details.'
//         }
//     }
// }


pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME      = 'fintech'
        DOCKERHUB_REPO            = 'francdocmain/fnctech-api'
        SONARQUBE_SERVER          = 'SonarQube'
        SONAR_PROJECT_KEY         = 'fintech-api'
        DOCKER_CREDENTIALS_ID     = 'dockerhub-credentials'
        SMOKE_TEST_CREDENTIALS_ID = 'fintech-uat-credentials'
        SONAR_TOKEN_CREDENTIAL_ID = 'sonarqube-token'
        MAVEN_SETTINGS_ID         = '11e2101e-5b3d-4afa-894f-834c2cfacd33'
        MAVEN_IMAGE               = 'maven:3.9-eclipse-temurin-17'
        MAVEN_CLI_OPTS            = '-B -ntp -s settings.xml -Dmaven.repo.local=/workspace/.m2/repository'
        SONAR_PLUGIN_VERSION      = '4.0.0.4121'
        MAVEN_REPO                = '/var/jenkins_home/.m2/repository'
        DOCKER_BUILDKIT           = '1'
    }

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/francdomain/fnctech_API.git'
            }
        }

        stage('Prepare') {
            steps {
                withCredentials([file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE')]) {
                    sh 'cp "$ENV_FILE" .env && chmod 600 .env'
                }
                configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) { sh 'true' }
                script {
                    def tag = sh(returnStdout: true, script: """
                        git fetch --tags --force >/dev/null 2>&1 || true
                        git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n 1
                    """).trim()
                    def ver = tag ? (tag =~ /^v(\d+)\.(\d+)\.(\d+)$/)[0].with { "v${it[1]}.${it[2]}.${it[3].toInteger()+1}" } : 'v1.0.0'
                    env.IMAGE_VERSIONED = "${env.DOCKERHUB_REPO}:${ver}"
                }
            }
        }

        stage('Build and Test') {
            steps {
                sh """
                    docker run --rm --network host \
                        -v "\$WORKSPACE":/workspace \
                        -v "${MAVEN_REPO}":/workspace/.m2/repository \
                        -w /workspace ${MAVEN_IMAGE} \
                        mvn ${MAVEN_CLI_OPTS} clean verify
                """
            }
            post {
                always { junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true }
            }
        }

        stage('Quality and Image Build') {
            parallel {
                stage('SonarQube Analysis') {
                    steps {
                        withSonarQubeEnv("${SONARQUBE_SERVER}") {
                            withCredentials([string(credentialsId: env.SONAR_TOKEN_CREDENTIAL_ID, variable: 'SONAR_TOKEN')]) {
                                timeout(time: 12, unit: 'MINUTES') {
                                    sh """
                                        docker run --rm --network host \
                                            -v "\$WORKSPACE":/workspace \
                                            -v "${MAVEN_REPO}":/workspace/.m2/repository \
                                            -w /workspace ${MAVEN_IMAGE} \
                                            mvn ${MAVEN_CLI_OPTS} \
                                            org.sonarsource.scanner.maven:sonar-maven-plugin:${SONAR_PLUGIN_VERSION}:sonar \
                                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                            -Dsonar.host.url=\$SONAR_HOST_URL \
                                            -Dsonar.login=\$SONAR_TOKEN \
                                            -Dsonar.qualitygate.wait=true \
                                            -Dsonar.ws.timeout=120
                                    """
                                }
                            }
                        }
                    }
                }
                stage('Docker Build') {
                    steps {
                        sh """
                            docker pull ${DOCKERHUB_REPO}:latest || true
                            docker compose build --pull --build-arg BUILDKIT_INLINE_CACHE=1 app
                            docker tag fintech-app:latest ${IMAGE_VERSIONED}
                            docker tag fintech-app:latest ${DOCKERHUB_REPO}:latest
                        """
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose up -d db app'
            }
        }

        stage('Validate') {
            steps {
                script {
                    env.APP_HOST_PORT = sh(returnStdout: true,
                        script: "docker compose port app 8080 | awk -F: '{print \$NF}' | tr -d '\\r'").trim()
                    if (!env.APP_HOST_PORT) error('Could not resolve published port for app:8080')
                }
                sh """
                    for i in \$(seq 1 30); do
                        curl -sf -o /dev/null -X POST http://localhost:${env.APP_HOST_PORT}/api/auth/login \
                            -H "Content-Type: application/json" -d '{}' && echo "App reachable" && exit 0
                        echo "Waiting... \$i/30"; sleep 2
                    done
                    docker compose logs --tail=100 app || true; exit 1
                """
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    withCredentials([usernamePassword(credentialsId: env.SMOKE_TEST_CREDENTIALS_ID, usernameVariable: 'U', passwordVariable: 'P')]) {
                        sh """
                            curl -sf -X POST http://localhost:${env.APP_HOST_PORT}/api/auth/login \
                                -H "Content-Type: application/json" \
                                -d '{"email":"${env.U}","password":"${env.P}"}' \
                                && echo "UAT passed" || { echo "UAT failed"; exit 1; }
                        """
                    }
                }
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'U', passwordVariable: 'P')]) {
                    sh """
                        echo "\$P" | docker login -u "\$U" --password-stdin
                        docker push ${IMAGE_VERSIONED}
                        docker push ${DOCKERHUB_REPO}:latest
                        docker logout
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'docker compose stop app db || true'
            sh 'docker image prune -f && rm -f .env settings.xml || true'
        }
        success { echo "✅ Pushed ${env.IMAGE_VERSIONED}" }
        failure { echo '❌ Pipeline failed — check logs.' }
    }
}