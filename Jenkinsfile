pipeline {
    // Defines that this pipeline can run on any available Jenkins agent/worker node
    agent any

    // Link the Maven installation from Jenkins UI (Matches the name 'Maven3')
    tools {
        maven 'Maven3'
    }

    // Sets global environment variables used across all stages
    environment {
        DOCKER_REGISTRY = "sudhakar-registry" 
        HELM_RELEASE_NAME = "banking-app-release"
        HELM_CHART_PATH = "./helm-charts/banking-app"
        // Add Docker to PATH so Jenkins can find it without Docker plugin
        PATH = "C:\\Users\\Sudhakar\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin;${env.PATH}"
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
            // In Declarative Pipeline, parallel stages must be direct children 
            // of the parent stage, replacing the steps block.
            parallel {
                stage('Account Service') {
                    steps {
                        dir('account-service') {
                            bat 'mvn clean package -DskipTests -Dmaven.repo.local=.m2/repository'
                        }
                    }
                }
                stage('Payment Service') {
                    steps {
                        dir('payment-service') {
                            bat 'mvn clean package -DskipTests -Dmaven.repo.local=.m2/repository'
                        }
                    }
                }
                stage('API Gateway') {
                    steps {
                        dir('api-gateway') {
                            bat 'mvn clean package -DskipTests -Dmaven.repo.local=.m2/repository'
                        }
                    }
                }
            }
        }

        stage('Build Frontend (Angular)') {
            steps {
                echo 'Compiling Angular Application...'
                dir('banking-frontend') {
                    bat 'npm install'
                    bat 'npm run build --prod'
                }
            }
        }

        stage('Dockerize (Build & Push)') {
            parallel {
                stage('Account Image') {
                    steps {
                        bat "docker build -t ${env.DOCKER_REGISTRY}/account-service:latest ./account-service"
                    }
                }
                stage('Payment Image') {
                    steps {
                        bat "docker build -t ${env.DOCKER_REGISTRY}/payment-service:latest ./payment-service"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes (Helm)') {
            steps {
                echo 'Deploying Multi-Microservice Mesh via Helm Umbrella Chart...'
                // One single command deploys all services thanks to Helm Umbrella pattern
                bat "helm upgrade --install ${env.HELM_RELEASE_NAME} ${env.HELM_CHART_PATH} --wait"
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
