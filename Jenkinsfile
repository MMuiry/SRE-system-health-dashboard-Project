def onServer(String command) {
    withCredentials([sshUserPrivateKey(credentialsId: 'aws-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
        sh "ssh -i \"\$SSH_KEY\" -o StrictHostKeyChecking=accept-new \"\$SSH_USER@${env.DEPLOY_HOST}\" 'cd ~/${env.REPO_DIR} && ${command}'"
    }
}

def jfrogLoginOnServer() {
    withCredentials([
        sshUserPrivateKey(credentialsId: 'aws-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
        usernamePassword(credentialsId: 'jfrog-creds', usernameVariable: 'JF_USER', passwordVariable: 'JF_TOKEN')
    ]) {
        sh 'echo "$JF_TOKEN" | ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$SSH_USER@$DEPLOY_HOST" "docker login $JFROG_REGISTRY -u $JF_USER --password-stdin"'
    }
}

def deployTo(String environmentName) {
    jfrogLoginOnServer()
    onServer("git fetch -q origin && git checkout -q --force ${env.GIT_COMMIT} && ./scripts/deploy.sh ${environmentName} ${env.IMAGE_TAG}")
    env.DEPLOYED_TO = environmentName
}

pipeline {
    agent any

    environment {
        IMAGE_NAME      = 'system-health-dashboard'
        AGENT_IMAGE     = 'system-agent'
        IMAGE_TAG       = "1.0.${BUILD_NUMBER}"
        CI_ID           = "${env.BRANCH_NAME.replaceAll('[^A-Za-z0-9]', '-').toLowerCase()}-${env.BUILD_NUMBER}"
        CONTAINER       = "shd-ci-${CI_ID}"
        AGENT_CONTAINER = "agent-ci-${CI_ID}"
        KAFKA_CONTAINER = "kafka-ci-${CI_ID}"
        CI_NETWORK      = "shd-ci-net-${CI_ID}"
        JFROG_REGISTRY  = 'muirytest.jfrog.io'
        JFROG_REPO      = 'docker-local'
        DEPLOY_HOST     = '16.170.218.243'
        REPO_DIR        = 'SRE-system-health-dashboard'
    }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Install') {
            steps {
                sh '''
                    chmod +x mvnw
                    ./mvnw -B -q dependency:go-offline
                '''
            }
        }

        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test agent') {
            steps {
                sh '''
                    docker run --rm --volumes-from jenkins -w "$WORKSPACE/agent" python:3.12-slim \
                      sh -c "pip install -q -r requirements.txt && python -m pytest -v --junitxml=pytest-report.xml"
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'agent/pytest-report.xml'
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    docker build --build-arg APP_VERSION=${IMAGE_TAG} -t ${IMAGE_NAME}:candidate .
                    docker build -t ${AGENT_IMAGE}:candidate ./agent
                '''
            }
        }

        stage('Tag') {
            steps {
                sh '''
                    docker tag ${IMAGE_NAME}:candidate ${IMAGE_NAME}:${IMAGE_TAG}
                    docker tag ${AGENT_IMAGE}:candidate ${AGENT_IMAGE}:${IMAGE_TAG}
                    docker images ${IMAGE_NAME}
                    docker images ${AGENT_IMAGE}
                '''
            }
        }

        stage('Health check') {
            steps {
                sh '''
                    docker network create ${CI_NETWORK}

                    docker run -d --name ${KAFKA_CONTAINER} --network ${CI_NETWORK} \
                      -e KAFKA_NODE_ID=1 \
                      -e KAFKA_PROCESS_ROLES=broker,controller \
                      -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
                      -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${KAFKA_CONTAINER}:9092 \
                      -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
                      -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
                      -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
                      -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                      -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
                      -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
                      apache/kafka:3.9.0

                    docker run -d --name ${CONTAINER} --network ${CI_NETWORK} \
                      -e APP_ENV=ci -e PORT=8080 \
                      -e KAFKA_ENABLED=true \
                      -e KAFKA_BOOTSTRAP_SERVERS=${KAFKA_CONTAINER}:9092 \
                      ${IMAGE_NAME}:${IMAGE_TAG}

                    docker run -d --name ${AGENT_CONTAINER} --network ${CI_NETWORK} \
                      -e MACHINE_NAME=ci-machine \
                      -e KAFKA_BOOTSTRAP_SERVERS=${KAFKA_CONTAINER}:9092 \
                      ${AGENT_IMAGE}:${IMAGE_TAG}

                    for i in $(seq 1 30); do
                        if docker run --rm --network ${CI_NETWORK} curlimages/curl -fsS http://${CONTAINER}:8080/health \
                           && docker run --rm --network ${CI_NETWORK} curlimages/curl -fsS http://${AGENT_CONTAINER}:5000/health; then
                            echo ""
                            echo "Health check passed (Java API and agent)"
                            exit 0
                        fi
                        echo "Waiting for services ($i/30)..."
                        sleep 2
                    done
                    echo "Health check FAILED"
                    docker logs ${CONTAINER} | tail -50
                    docker logs ${AGENT_CONTAINER} | tail -50
                    exit 1
                '''
            }
        }

        stage('Kafka check') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                        docker run --rm --network ${CI_NETWORK} curlimages/curl -fsS http://${CONTAINER}:8080/health > /dev/null || true
                        EVENTS=$(docker run --rm --network ${CI_NETWORK} curlimages/curl -fsS http://${CONTAINER}:8080/events || true)
                        echo "$EVENTS"
                        if echo "$EVENTS" | grep -q "health-check UP"; then
                            echo "Kafka round trip passed"
                            exit 0
                        fi
                        echo "Waiting for Kafka events ($i/30)..."
                        sleep 2
                    done
                    echo "Kafka check FAILED"
                    docker logs ${CONTAINER} | tail -50
                    exit 1
                '''
            }
        }

        stage('Machine metrics check') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                        MACHINES=$(docker run --rm --network ${CI_NETWORK} curlimages/curl -fsS http://${CONTAINER}:8080/machines || true)
                        echo "$MACHINES"
                        if echo "$MACHINES" | grep -q "ci-machine"; then
                            echo "Agent metrics reached the Java API through Kafka"
                            exit 0
                        fi
                        echo "Waiting for agent metrics ($i/30)..."
                        sleep 2
                    done
                    echo "Machine metrics check FAILED"
                    docker logs ${AGENT_CONTAINER} | tail -50
                    exit 1
                '''
            }
        }

        stage('Publish to JFrog') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'jfrog-creds',
                                                  usernameVariable: 'JF_USER',
                                                  passwordVariable: 'JF_TOKEN')]) {
                    sh '''
                        echo "$JF_TOKEN" | docker login ${JFROG_REGISTRY} -u "$JF_USER" --password-stdin
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${JFROG_REGISTRY}/${JFROG_REPO}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${JFROG_REGISTRY}/${JFROG_REPO}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker tag ${AGENT_IMAGE}:${IMAGE_TAG} ${JFROG_REGISTRY}/${JFROG_REPO}/${AGENT_IMAGE}:${IMAGE_TAG}
                        docker push ${JFROG_REGISTRY}/${JFROG_REPO}/${AGENT_IMAGE}:${IMAGE_TAG}
                        docker logout ${JFROG_REGISTRY}
                    '''
                }
            }
        }

        stage('Deploy to Staging') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                script {
                    deployTo('staging')
                }
            }
        }

        stage('Smoke test (staging)') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                script {
                    onServer("./scripts/smoke_test.sh 5001 ${env.IMAGE_TAG}")
                }
            }
        }

        stage('Verify monitoring') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                script {
                    onServer('./scripts/verify_monitoring.sh staging')
                }
            }
        }

        stage('Approval') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def approver = input(message: "Deploy ${env.IMAGE_TAG} to production?",
                                         ok: 'Deploy to production',
                                         submitterParameter: 'APPROVER')
                    echo "Production release ${env.IMAGE_TAG} approved by ${approver}"
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    deployTo('production')
                }
            }
        }

        stage('Smoke test (production)') {
            when {
                branch 'main'
            }
            steps {
                script {
                    onServer("./scripts/smoke_test.sh 5002 ${env.IMAGE_TAG}")
                }
            }
        }
    }

    post {
        failure {
            script {
                if (env.DEPLOYED_TO) {
                    echo "Deployment failed - rolling back ${env.DEPLOYED_TO} to its previous tag"
                    jfrogLoginOnServer()
                    onServer("./scripts/rollback.sh ${env.DEPLOYED_TO}")
                } else {
                    echo "Failed before any deployment - nothing to roll back"
                }
            }
        }
        always {
            sh '''
                docker rm -f ${CONTAINER} ${AGENT_CONTAINER} ${KAFKA_CONTAINER} || true
                docker network rm ${CI_NETWORK} || true
            '''
        }
    }
}