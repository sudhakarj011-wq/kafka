pipeline {
    // Defines that this pipeline can run on any available Jenkins agent/worker node
    agent any

    // Sets global environment variables used across all stages
    environment {
        DOCKER_REGISTRY = "sudhakar-registry" 
        HELM_RELEASE_NAME = "banking-app-release"
        HELM_CHART_PATH = "./helm-charts/banking-app"
    }

    stages {
        stage('Checkout Code') {
            steps {
                echo 'Checking out source code from Git repository...'
                // Automatically pulls the latest code from your awesome new GitHub repo!
                git url: 'https://github.com/sudhakarj011-wq/kafka.git', branch: 'master'
            }
        }

        stage('Build Backend (Maven Fast)') {
            steps {
                echo 'Building Spring Boot Microservices...'
                // Using parallel execution to build multiple services at the same time to save time
                parallel {
                    stage('Account Service') {
                        steps {
                            dir('account-service') {
                                sh 'mvn clean package -DskipTests'
                            }
                        }
                    }
                    stage('Payment Service') {
                        steps {
                            dir('payment-service') {
                                sh 'mvn clean package -DskipTests'
                            }
                        }
                    }
                    stage('API Gateway') {
                        steps {
                            dir('api-gateway') {
                                sh 'mvn clean package -DskipTests'
                            }
                        }
                    }
                }
            }
        }

        stage('Build Frontend (Angular)') {
            steps {
                echo 'Compiling Angular Application...'
                dir('banking-frontend') {
                    sh 'npm install'
                    sh 'npm run build --prod'
                }
            }
        }

        stage('Dockerize (Build & Push)') {
            steps {
                echo 'Building Docker Images and pushing to Registry...'
                parallel {
                    stage('Account Image') {
                        steps {
                            sh 'docker build -t ${DOCKER_REGISTRY}/account-service:latest ./account-service'
                        }
                    }
                    stage('Payment Image') {
                        steps {
                            sh 'docker build -t ${DOCKER_REGISTRY}/payment-service:latest ./payment-service'
                        }
                    }
                }
            }
        }

        stage('Deploy to Kubernetes (Helm)') {
            steps {
                echo 'Deploying Multi-Microservice Mesh via Helm Umbrella Chart...'
                // One single command deploys all services thanks to Helm Umbrella pattern
                sh 'helm upgrade --install ${HELM_RELEASE_NAME} ${HELM_CHART_PATH} --wait'
            }
        }
    }

    // Post-execution actions
    post {
        success {
            echo '✅ Pipeline Execution SUCCESS: The Banking System is successfully deployed to Kubernetes!'
        }
        failure {
            echo '❌ Pipeline Execution FAILED: Please check logs and investigate code errors.'
        }
    }
}
