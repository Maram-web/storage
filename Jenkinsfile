pipeline {
    agent any

    environment {
        K8S_MASTER = "ceph1@192.168.13.11"
        DEPLOY_YAML = "k8s-storage-deployment.yaml"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 's3-done', url: 'https://github.com/Maram-web/storage.git'
            }
        }

        stage('Set Dynamic Image Tag') {
            steps {
                script {
                    def tag = "v${new Date().format('yyyyMMdd-HHmmss')}"
                    env.IMAGE_NAME = "marammanai/storage-service:${tag}"
                    env.IMAGE_TAG = tag
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh "docker build -t $IMAGE_NAME ."
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

        stage('Deploy') {
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
            echo "✅ storage-service deployed with tag: ${env.IMAGE_TAG}"
        }
        failure {
            echo "❌ storage-service deployment failed"
        }
    }
}
