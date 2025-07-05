pipeline {
    agent any

    environment {
        TIMESTAMP = "${new Date().format('yyyyMMdd-HHmmss')}"
        IMAGE_TAG = "v${TIMESTAMP}"
        IMAGE_NAME = "marammanai/storage-service:${IMAGE_TAG}"
        K8S_MASTER = "ceph1@192.168.13.11"
        DEPLOY_YAML = "k8s-storage-deployment.yaml"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Maram-web/storage-service.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t $IMAGE_NAME ."
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME
                    '''
                }
            }
        }

        stage('Inject Tag into YAML') {
            steps {
                sh """
                    sed 's|__IMAGE_TAG__|$IMAGE_TAG|g' k8s-storage-template.yaml > $DEPLOY_YAML
                """
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    ssh-keyscan -H 192.168.13.11 >> ~/.ssh/known_hosts
                    scp $DEPLOY_YAML $K8S_MASTER:/home/ceph1/
                    ssh $K8S_MASTER kubectl apply -f /home/ceph1/$DEPLOY_YAML
                '''
            }
        }
    }

    post {
        success {
            echo "✅ storage-service deployed with tag: ${IMAGE_TAG}"
        }
        failure {
            echo "❌ storage-service deployment failed"
        }
    }
}
