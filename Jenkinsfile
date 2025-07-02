pipeline {
    agent any

    environment {
        IMAGE_NAME = "marammanai/storage-service:latest"
        K8S_MASTER = "ceph1@192.168.13.11"
        DEPLOY_YAML = "k8s-storage-deployment.yaml"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 's3-done', url: 'https://github.com/Maram-web/storage.git'
            }
        }

        stage('Analyse des changements') {
            steps {
                script {
                    def diffResult = sh(script: "git rev-parse HEAD~1 || echo 'first-build'", returnStdout: true).trim()

                    if (diffResult == 'first-build') {
                        echo "🟡 Premier build : on force la construction de l'image"
                        env.NEED_BUILD_DOCKER = "true"
                    } else {
                        def changes = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                        echo "📂 Fichiers modifiés:\n${changes}"
                        env.NEED_BUILD_DOCKER = (changes.contains("Dockerfile") || changes.contains("src/")) ? "true" : "false"
                    }
                }
            }
        }

        stage('Docker Build') {
            when {
                expression { env.NEED_BUILD_DOCKER == "true" }
            }
            steps {
                sh 'docker build -t $IMAGE_NAME .'
            }
        }

        stage('Docker Push') {
            when {
                expression { env.NEED_BUILD_DOCKER == "true" }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME
                    '''
                }
            }
        }

        stage('Copy YAML') {
            steps {
                sh '''
                    ssh-keyscan -H 192.168.13.11 >> ~/.ssh/known_hosts
                    scp $DEPLOY_YAML $K8S_MASTER:/home/ceph1/$DEPLOY_YAML
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh 'ssh $K8S_MASTER kubectl apply -f /home/ceph1/$DEPLOY_YAML'
            }
        }
    }

    post {
        success {
            echo "✅ storage-service deployed!"
        }
        failure {
            echo "❌ storage-service failed!"
        }
    }
}
