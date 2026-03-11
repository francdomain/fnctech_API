pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/francdomain/fnctech_API.git'
            }
        }

        stage('Load Environment Config') {
            steps {
                script {
                    if (!fileExists('.env')) {
                        error('.env file not found in workspace. Create it before running the pipeline.')
                    }

                    def envFile = readProperties file: '.env'
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

                    env.IMAGE_TAG = env.BUILD_NUMBER
                    env.IMAGE_LATEST = "${env.DOCKERHUB_REPO}:latest"
                    env.IMAGE_BUILD = "${env.DOCKERHUB_REPO}:${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    sh "mvn -B sonar:sonar -Dsonar.projectKey=${SONAR_PROJECT_KEY}"
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
        }
        success {
            echo "Pipeline completed successfully. Pushed ${IMAGE_BUILD} and ${IMAGE_LATEST}"
        }
        failure {
            echo 'Pipeline failed. Check stage logs for details.'
        }
    }
}
