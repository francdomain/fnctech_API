pipeline {
    agent any

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

        stage('Prepare .env') {
            steps {
                withCredentials([file(credentialsId: 'fintech-env-file', variable: 'ENV_FILE_PATH')]) {
                    sh '''
                        cp "$ENV_FILE_PATH" .env
                        chmod 600 .env
                    '''
                }
            }
        }

        stage('Load Environment Config') {
            steps {
                script {
                    if (!fileExists('.env')) {
                        error('.env file not found in workspace. Create it before running the pipeline.')
                    }

                    def envFile = [:]
                    readFile('.env').split('\n').each { rawLine ->
                        def line = rawLine.trim()
                        if (!line || line.startsWith('#')) {
                            return
                        }

                        int separatorIndex = line.indexOf('=')
                        if (separatorIndex <= 0) {
                            return
                        }

                        def key = line.substring(0, separatorIndex).trim()
                        def value = line.substring(separatorIndex + 1).trim()

                        if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1)
                        }

                        envFile[key] = value
                    }
                    def requiredKeys = [
                        'APP_NAME',
                        'APP_PORT',
                        'CONTAINER_NAME',
                        'DOCKERHUB_REPO',
                        'DOCKER_CREDENTIALS_ID',
                        'SMOKE_TEST_CREDENTIALS_ID',
                        'SONARQUBE_SERVER',
                        'SONAR_PROJECT_KEY'
                    ]

                    def missingKeys = requiredKeys.findAll { key ->
                        !envFile[key] || !envFile[key].toString().trim()
                    }

                    if (!missingKeys.isEmpty()) {
                        error("Missing required .env keys: ${missingKeys.join(', ')}")
                    }

                    env.APP_NAME = envFile['APP_NAME']
                    env.APP_PORT = envFile['APP_PORT']
                    env.CONTAINER_NAME = envFile['CONTAINER_NAME']
                    env.DOCKERHUB_REPO = envFile['DOCKERHUB_REPO']
                    env.DOCKER_CREDENTIALS_ID = envFile['DOCKER_CREDENTIALS_ID']
                    env.SMOKE_TEST_CREDENTIALS_ID = envFile['SMOKE_TEST_CREDENTIALS_ID']
                    env.SONARQUBE_SERVER = envFile['SONARQUBE_SERVER']
                    env.SONAR_PROJECT_KEY = envFile['SONAR_PROJECT_KEY']

                    env.MAVEN_CLI_OPTS = '-B -ntp -Dmaven.wagon.http.retryHandler.count=3 -Dsun.net.client.defaultConnectTimeout=30000 -Dsun.net.client.defaultReadTimeout=180000'
                    if (envFile['PROXY_HOST']?.trim() && envFile['PROXY_PORT']?.trim()) {
                        env.MAVEN_CLI_OPTS = "${env.MAVEN_CLI_OPTS} -Dhttp.proxyHost=${envFile['PROXY_HOST']} -Dhttp.proxyPort=${envFile['PROXY_PORT']} -Dhttps.proxyHost=${envFile['PROXY_HOST']} -Dhttps.proxyPort=${envFile['PROXY_PORT']}"
                    }
                    env.MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-17'

                    env.IMAGE_TAG = env.BUILD_NUMBER
                    env.IMAGE_LATEST = "${env.DOCKERHUB_REPO}:latest"
                    env.IMAGE_BUILD = "${env.DOCKERHUB_REPO}:${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build') {
            steps {
                timeout(time: 20, unit: 'MINUTES') {
                    sh 'docker run --rm -v "$WORKSPACE":/workspace -w /workspace -v "$HOME/.m2":/root/.m2 ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} clean compile'
                }
            }
        }

        stage('Unit Test') {
            steps {
                sh 'docker run --rm -v "$WORKSPACE":/workspace -w /workspace -v "$HOME/.m2":/root/.m2 ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'docker run --rm -v "$WORKSPACE":/workspace -w /workspace -v "$HOME/.m2":/root/.m2 ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    sh 'docker run --rm -v "$WORKSPACE":/workspace -w /workspace -v "$HOME/.m2":/root/.m2 ${MAVEN_IMAGE} mvn ${MAVEN_CLI_OPTS} sonar:sonar -Dsonar.projectKey=${SONAR_PROJECT_KEY}'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t ${IMAGE_BUILD} -t ${IMAGE_LATEST} .'
            }
        }

        stage('Run Containers (Compose)') {
            steps {
                sh '''
                    docker compose down -v || true
                    docker compose up -d db app
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    for i in {1..30}; do
                        status=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:${APP_PORT}/api/auth/login -H "Content-Type: application/json" -d '{}' || true)
                        if [ "$status" != "000" ]; then
                            echo "Smoke test passed (application is reachable). HTTP status: $status"
                            exit 0
                        fi
                        echo "Waiting for app to be reachable... attempt $i"
                        sleep 2
                    done
                    echo "Smoke test failed: application not reachable"
                    exit 1
                '''
            }
        }

        stage('UAT') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.SMOKE_TEST_CREDENTIALS_ID}", usernameVariable: 'UAT_EMAIL', passwordVariable: 'UAT_PASSWORD')]) {
                    sh '''
                        status=$(curl -s -o /tmp/uat_login_response.json -w "%{http_code}" -X POST http://localhost:${APP_PORT}/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"${UAT_EMAIL}\",\"password\":\"${UAT_PASSWORD}\"}" || true)
                        if [ "$status" = "200" ]; then
                            echo "UAT passed: login accepted with test credentials"
                            cat /tmp/uat_login_response.json
                            exit 0
                        fi
                        echo "UAT failed: expected HTTP 200 from login, got $status"
                        cat /tmp/uat_login_response.json || true
                        exit 1
                    '''
                }
            }
        }

        stage('Push Image to Docker Hub') {
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

    post {
        always {
            sh 'docker compose down -v || true'
            sh 'rm -f .env || true'
        }
        success {
            echo "Pipeline completed successfully. Pushed ${IMAGE_BUILD} and ${IMAGE_LATEST}"
        }
        failure {
            echo 'Pipeline failed. Check stage logs for details.'
        }
    }
}
