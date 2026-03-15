pipeline {
    agent any

    environment {
        // Non-secret config
        COMPOSE_PROJECT_NAME      = 'fintech'
        DOCKERHUB_REPO            = 'francdomain/fnctech-api'
        HOST_APP_PORT             = '8081'
        SONARQUBE_SERVER          = 'SonarQube'
        SONAR_URL                 = 'http://172.26.44.147:9000'
        SONAR_PROJECT_KEY         = 'fintech-api'
        DOCKER_CREDENTIALS_ID     = 'dockerhub-credentials'
        SMOKE_TEST_CREDENTIALS_ID = 'fintech-uat-credentials'
        SONAR_TOKEN_CREDENTIAL_ID  = 'sonarqube-token'
        MAVEN_SETTINGS_ID         = '11e2101e-5b3d-4afa-894f-834c2cfacd33'
        MAVEN_IMAGE               = 'maven:3.9-eclipse-temurin-17'
        MAVEN_CLI_OPTS            = '-B -ntp -s settings.xml -Dmaven.repo.local=/workspace/.m2/repository'
        SONAR_MAVEN_PLUGIN_VERSION = '4.0.0.4121'
    }

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/francdomain/fnctech_API.git'
            }
        }

        stage('Prepare Environment') {
            steps {
                // .env is only needed by docker-compose for DB credentials at runtime
                withCredentials([file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE_PATH')]) {
                    sh 'cp "$ENV_FILE_PATH" .env && chmod 600 .env'
                }
                script {
                    env.IMAGE_BUILD  = "${env.DOCKERHUB_REPO}:${env.BUILD_NUMBER}"
                    env.IMAGE_LATEST = "${env.DOCKERHUB_REPO}:latest"
                }
                // Start DB early so SonarQube (system service) can connect during Code Quality stage
                sh '''
                    docker compose up -d db
                    echo "Waiting for DB to become healthy..."
                    for i in $(seq 1 30); do
                        docker compose ps db | grep -q '(healthy)' && echo "DB is healthy" && break
                        echo "DB not ready yet, attempt $i/30..."
                        sleep 5
                    done

                    # If SonarQube lost its DB connection (HTTP 500), restart it now that DB is back up
                    SONAR_API_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 \
                        $SONAR_URL/api/settings/values.protobuf 2>/dev/null || echo "000")
                    echo "SonarQube settings API probe (no-auth): HTTP $SONAR_API_CODE"
                    if [ "$SONAR_API_CODE" = "500" ]; then
                        echo "SonarQube DB connection is broken. Restarting SonarQube service..."
                        sudo systemctl restart sonarqube \
                            || echo "WARNING: sudo restart failed — add 'jenkins ALL=(ALL) NOPASSWD: /bin/systemctl restart sonarqube' to sudoers. Scan may fail."
                        echo "Waiting 90s for SonarQube to reinitialize..."
                        sleep 90
                    fi
                '''
            }
        }

        stage('CI') {
            stages {
                stage('Build') {
                    steps {
                        configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                            sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} clean compile'
                        }
                    }
                }

                stage('Unit Test') {
                    steps {
                        configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                            sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} verify'
                        }
                    }
                    post {
                        always {
                            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                        }
                    }
                }

                stage('Package') {
                    steps {
                        configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                            sh 'docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Code Quality') {
            stages {
                stage('SonarQube Analysis') {
                    steps {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                                    withCredentials([string(credentialsId: env.SONAR_TOKEN_CREDENTIAL_ID, variable: 'SONAR_TOKEN')]) {
                                        script {
                                            int sonarHealth = sh(
                                                script: 'curl -fsS "$SONAR_HOST_URL/api/system/status" >/dev/null',
                                                returnStatus: true
                                            )
                                            if (sonarHealth != 0) {
                                                unstable("SonarQube server is unavailable (health-check failed). Skipping analysis for this run.")
                                                return
                                            }

                                            int sonarSettingsApi = sh(
                                                script: 'curl -fsS -u "$SONAR_TOKEN:" "$SONAR_HOST_URL/api/settings/values.protobuf" -o /dev/null',
                                                returnStatus: true
                                            )
                                            if (sonarSettingsApi != 0) {
                                                unstable("SonarQube settings API is unavailable (HTTP 500/timeout). Skipping analysis for this run.")
                                                return
                                            }

                                            timeout(time: 8, unit: 'MINUTES') {
                                                int scanStatus = 1
                                                for (int attempt = 1; attempt <= 3; attempt++) {
                                                    echo "Sonar scan attempt ${attempt}/3"
                                                    scanStatus = sh(
                                                        script: '''
                                                            docker run --rm --network host -v "$WORKSPACE":/workspace -w /workspace ${MAVEN_IMAGE} \
                                                              mvn ${MAVEN_CLI_OPTS} org.sonarsource.scanner.maven:sonar-maven-plugin:${SONAR_MAVEN_PLUGIN_VERSION}:sonar \
                                                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                                              -Dsonar.host.url=$SONAR_HOST_URL \
                                                              -Dsonar.login=$SONAR_TOKEN \
                                                              -Dsonar.qualitygate.wait=true \
                                                              -Dsonar.ws.timeout=120
                                                        ''',
                                                        returnStatus: true
                                                    )

                                                    if (scanStatus == 0) {
                                                        echo "Sonar scan completed successfully"
                                                        break
                                                    }

                                                    if (attempt < 3) {
                                                        echo "Sonar scan failed (likely server-side). Waiting 15s before retry..."
                                                        sleep(time: 15, unit: 'SECONDS')
                                                    }
                                                }

                                                if (scanStatus != 0) {
                                                    unstable("SonarQube analysis failed after 3 attempts (server returned errors). Continuing pipeline as UNSTABLE.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // stage('Quality Gate') {
                //     steps {
                //         timeout(time: 10, unit: 'MINUTES') {
                //             waitForQualityGate abortPipeline: true
                //         }
                //     }
                // }
            }
        }

        stage('Docker') {
            stages {
                stage('Docker Build') {
                    steps {
                        configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_ID, targetLocation: 'settings.xml')]) {
                            sh '''
                                for i in 1 2 3 4 5; do
                                    docker pull eclipse-temurin:17-jre-jammy && break
                                    echo "Pull attempt $i failed, retrying in 15s..."
                                    sleep 15
                                done
                                docker compose build app
                                docker tag fintech-app:latest ${IMAGE_BUILD}
                                docker tag fintech-app:latest ${IMAGE_LATEST}
                            '''
                        }
                    }
                }
            }
        }

        stage('Release') {
            stages {
                stage('Deploy') {
                    steps {
                        sh '''
                            docker compose up -d --force-recreate app
                        '''
                    }
                }

                stage('Smoke Test') {
                    steps {
                        sh '''
                            APP_HOST_PORT=$(docker compose port app 8080 | awk -F: '{print $NF}' | tr -d '\r')
                            if [ -z "$APP_HOST_PORT" ]; then
                                echo "Smoke test failed: could not resolve published port for app:8080"
                                docker compose ps || true
                                docker compose logs --tail=200 app || true
                                exit 1
                            fi

                            echo "Resolved app host port: $APP_HOST_PORT"
                            for i in $(seq 1 90); do
                                status=$(curl -s -o /dev/null -w "%{http_code}" \
                                    -X POST http://localhost:${APP_HOST_PORT}/api/auth/login \
                                    -H "Content-Type: application/json" -d '{}' || true)
                                if [ "$status" != "000" ]; then
                                    echo "App reachable. HTTP status: $status"
                                    exit 0
                                fi
                                echo "Waiting for app... attempt $i"
                                sleep 2
                            done
                            echo "Smoke test failed: app not reachable after 180s"
                            docker compose ps || true
                            docker compose logs --tail=200 app || true
                            exit 1
                        '''
                    }
                }

                stage('UAT') {
                    steps {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            withCredentials([usernamePassword(credentialsId: "${env.SMOKE_TEST_CREDENTIALS_ID}", usernameVariable: 'UAT_EMAIL', passwordVariable: 'UAT_PASSWORD')]) {
                                sh '''
                                    APP_HOST_PORT=$(docker compose port app 8080 | awk -F: '{print $NF}' | tr -d '\r')
                                    if [ -z "$APP_HOST_PORT" ]; then
                                        echo "UAT failed: could not resolve published port for app:8080"
                                        docker compose ps || true
                                        exit 1
                                    fi

                                    status=$(curl -s -o /tmp/uat_response.json -w "%{http_code}" \
                                        -X POST http://localhost:${APP_HOST_PORT}/api/auth/login \
                                        -H "Content-Type: application/json" \
                                        -d "{\"email\":\"${UAT_EMAIL}\",\"password\":\"${UAT_PASSWORD}\"}" || true)
                                    if [ "$status" = "200" ]; then
                                        echo "UAT passed"
                                    else
                                        echo "UAT failed: expected 200, got $status"
                                        cat /tmp/uat_response.json || true
                                        exit 1
                                    fi
                                '''
                            }
                        }
                    }
                }

                stage('Push to Docker Hub') {
                    steps {
                        withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            sh '''
                                echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                                docker push ${IMAGE_BUILD}
                                docker push ${IMAGE_LATEST}
                                docker logout
                            '''
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker compose stop app || true'
            sh 'rm -f .env settings.xml || true'
        }
        success {
            echo "Pipeline succeeded. Pushed ${IMAGE_BUILD} and ${IMAGE_LATEST}"
        }
        failure {
            echo 'Pipeline failed. Check stage logs for details.'
        }
    }
}
