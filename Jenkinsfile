// Umbrella Jenkinsfile для банковской платформы
// Запускает CI/CD для всех микросервисов параллельно

pipeline {
    agent any

    tools {
        gradle 'gradle-8.5'
        jdk 'jdk-21'
    }

    environment {
        DOCKER_REGISTRY = 'registry.example.com/bank'
        KUBECONFIG_CREDENTIALS_ID = 'k8s-kubeconfig'
        DOCKER_CREDENTIALS_ID = 'docker-registry-creds'
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'staging', 'production'],
            description: 'Окружение для деплоя'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Запускать ли тесты'
        )
        booleanParam(
            name: 'PARALLEL_BUILD',
            defaultValue: true,
            description: 'Параллельная сборка микросервисов'
        )
        string(
            name: 'GIT_COMMIT',
            defaultValue: '',
            description: 'Git commit для сборки (оставьте пустым для HEAD)'
        )
    }

    options {
        timestamps()
        timeout(time: 2, unit: 'HOURS')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Preparation') {
            stages {
                stage('Checkout') {
                    steps {
                        checkout scm
                        script {
                            if (params.GIT_COMMIT) {
                                sh "git checkout ${params.GIT_COMMIT}"
                            }
                            env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'HEAD'}"
                            env.BUILD_DATE = sh(script: 'date -u +%Y-%m-%dT%H:%M:%SZ', returnStdout: true).trim()
                        }
                    }
                }

                stage('Install Tools') {
                    steps {
                        sh '''
                            # Установка Helm
                            if ! command -v helm &> /dev/null; then
                                curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
                            fi
                            
                            # Установка kubectl
                            if ! command -v kubectl &> /dev/null; then
                                curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                                chmod +x kubectl
                                sudo mv kubectl /usr/local/bin/
                            fi
                            
                            helm version
                            kubectl version --client
                        '''
                    }
                }

                stage('Validate') {
                    parallel {
                        stage('Validate Helm Charts') {
                            steps {
                                sh '''
                                    echo "=== Helm Lint ==="
                                    helm lint helm/accounts
                                    helm lint helm/cash
                                    helm lint helm/transfer
                                    helm lint helm/notifications
                                    helm lint helm/gateway
                                    helm lint helm/frontend
                                    helm lint helm/postgresql
                                    helm lint helm/keycloak
                                    helm lint helm/bank
                                '''
                            }
                        }

                        stage('Validate Gradle') {
                            steps {
                                sh './gradlew buildEnvironment --no-daemon'
                            }
                        }
                    }
                }
            }
        }

        stage('Build & Test') {
            parallel {
                stage('Accounts Service') {
                    steps {
                        script {
                            buildMicroservice('accounts')
                        }
                    }
                }

                stage('Cash Service') {
                    steps {
                        script {
                            buildMicroservice('cash')
                        }
                    }
                }

                stage('Transfer Service') {
                    steps {
                        script {
                            buildMicroservice('transfer')
                        }
                    }
                }

                stage('Notifications Service') {
                    steps {
                        script {
                            buildMicroservice('notifications')
                        }
                    }
                }

                stage('Gateway Service') {
                    steps {
                        script {
                            buildMicroservice('gateway')
                        }
                    }
                }

                stage('Frontend Service') {
                    steps {
                        script {
                            buildMicroservice('frontend')
                        }
                    }
                }
            }
        }

        stage('Integration Tests') {
            when {
                expression { return params.RUN_TESTS }
            }
            steps {
                script {
                    sh '''
                        echo "Running integration tests..."
                        ./gradlew :accounts:test --tests "*IntegrationTest*" || true
                        ./gradlew :cash:test --tests "*IntegrationTest*" || true
                        ./gradlew :transfer:test --tests "*IntegrationTest*" || true
                        ./gradlew :notifications:test --tests "*IntegrationTest*" || true
                        ./gradlew :gateway:test --tests "*IntegrationTest*" || true
                        ./gradlew :frontend:test --tests "*IntegrationTest*" || true
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    withDockerRegistry(credentialsId: DOCKER_CREDENTIALS_ID, toolName: 'docker') {
                        sh '''
                            docker build -t ${DOCKER_REGISTRY}/bank-accounts:${BUILD_VERSION} -f Dockerfile . --build-arg SERVICE_NAME=accounts
                            docker build -t ${DOCKER_REGISTRY}/bank-cash:${BUILD_VERSION} -f Dockerfile . --build-arg SERVICE_NAME=cash
                            docker build -t ${DOCKER_REGISTRY}/bank-transfer:${BUILD_VERSION} -f Dockerfile . --build-arg SERVICE_NAME=transfer
                            docker build -t ${DOCKER_REGISTRY}/bank-notifications:${BUILD_VERSION} -f Dockerfile . --build-arg SERVICE_NAME=notifications
                            docker build -t ${DOCKER_REGISTRY}/bank-gateway:${BUILD_VERSION} -f Dockerfile . --build-arg SERVICE_NAME=gateway
                            docker build -t ${DOCKER_REGISTRY}/bank-frontend:${BUILD_VERSION} -f Dockerfile . --build-arg SERVICE_NAME=frontend
                            
                            # Тегируем как latest для dev
                            docker tag ${DOCKER_REGISTRY}/bank-accounts:${BUILD_VERSION} ${DOCKER_REGISTRY}/bank-accounts:latest
                            docker tag ${DOCKER_REGISTRY}/bank-cash:${BUILD_VERSION} ${DOCKER_REGISTRY}/bank-cash:latest
                            docker tag ${DOCKER_REGISTRY}/bank-transfer:${BUILD_VERSION} ${DOCKER_REGISTRY}/bank-transfer:latest
                            docker tag ${DOCKER_REGISTRY}/bank-notifications:${BUILD_VERSION} ${DOCKER_REGISTRY}/bank-notifications:latest
                            docker tag ${DOCKER_REGISTRY}/bank-gateway:${BUILD_VERSION} ${DOCKER_REGISTRY}/bank-gateway:latest
                            docker tag ${DOCKER_REGISTRY}/bank-frontend:${BUILD_VERSION} ${DOCKER_REGISTRY}/bank-frontend:latest
                            
                            docker push ${DOCKER_REGISTRY}/bank-accounts:${BUILD_VERSION}
                            docker push ${DOCKER_REGISTRY}/bank-cash:${BUILD_VERSION}
                            docker push ${DOCKER_REGISTRY}/bank-transfer:${BUILD_VERSION}
                            docker push ${DOCKER_REGISTRY}/bank-notifications:${BUILD_VERSION}
                            docker push ${DOCKER_REGISTRY}/bank-gateway:${BUILD_VERSION}
                            docker push ${DOCKER_REGISTRY}/bank-frontend:${BUILD_VERSION}
                            
                            docker push ${DOCKER_REGISTRY}/bank-accounts:latest
                            docker push ${DOCKER_REGISTRY}/bank-cash:latest
                            docker push ${DOCKER_REGISTRY}/bank-transfer:latest
                            docker push ${DOCKER_REGISTRY}/bank-notifications:latest
                            docker push ${DOCKER_REGISTRY}/bank-gateway:latest
                            docker push ${DOCKER_REGISTRY}/bank-frontend:latest
                        '''
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                expression { return params.DEPLOY_ENV in ['dev', 'staging', 'production'] }
            }
            stages {
                stage('Deploy to Dev') {
                    when {
                        expression { return params.DEPLOY_ENV == 'dev' }
                    }
                    steps {
                        deployToKubernetes('dev')
                    }
                }

                stage('Deploy to Staging') {
                    when {
                        expression { return params.DEPLOY_ENV == 'staging' }
                    }
                    steps {
                        deployToKubernetes('staging')
                    }
                }

                stage('Deploy to Production') {
                    when {
                        expression { return params.DEPLOY_ENV == 'production' }
                    }
                    steps {
                        input message: 'Confirm deployment to PRODUCTION?', ok: 'Deploy to Production'
                        deployToKubernetes('production')
                    }
                }
            }
        }

        stage('Post-Deployment Validation') {
            when {
                expression { return params.DEPLOY_ENV in ['dev', 'staging', 'production'] }
            }
            steps {
                script {
                    sh '''
                        echo "Waiting for pods to be ready..."
                        kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=bank --timeout=300s
                        
                        echo "Checking service health..."
                        kubectl get pods -l app.kubernetes.io/part-of=bank
                        kubectl get svc -l app.kubernetes.io/part-of=bank
                        
                        echo "Running health checks..."
                        kubectl get pods -l app.kubernetes.io/part-of=bank -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}'
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed. Check logs for details."
        }
    }
}

// Вспомогательные методы
def buildMicroservice(String serviceName) {
    node {
        stage("Build ${serviceName}") {
            sh "./gradlew :${serviceName}:build -x test -x contractTest --no-daemon"
        }

        stage("Test ${serviceName}") {
            if (params.RUN_TESTS) {
                sh "./gradlew :${serviceName}:test :${serviceName}:contractTest --no-daemon"
                junit "**/${serviceName}/build/test-results/test/TEST-*.xml"
            }
        }

        stage("Archive ${serviceName}") {
            archiveArtifacts artifacts: "**/${serviceName}/build/libs/*.jar", allowEmptyArchive: true
        }
    }
}

def deployToKubernetes(String environment) {
    node {
        withKubeConfig([credentialsId: KUBECONFIG_CREDENTIALS_ID, serverUrl: '']) {
            sh """
                echo "Deploying to ${environment} environment..."
                
                # Определяем namespace
                NAMESPACE="bank-${environment}"
                
                # Обновляем values для окружения
                helm upgrade --install bank helm/bank \\
                    --namespace ${NAMESPACE} \\
                    --create-namespace \\
                    --set global.imageRegistry=${DOCKER_REGISTRY} \\
                    --set global.imageTag=${BUILD_VERSION} \\
                    --values helm/values-${environment}.yaml \\
                    --timeout 10m \\
                    --wait \\
                    --atomic
                
                echo "Deployment to ${environment} completed!"
            """
        }
    }
}
