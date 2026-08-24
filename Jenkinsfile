pipeline {
    agent any

    // 전제: Jenkins 에이전트에 node/npm + mvn + sudo(systemctl) 권한이 있어야 함
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Frontend Build') {
            steps {
                dir('apps/web') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Copy to Backend static') {
            steps {
                sh '''
                    rm -rf services/api/src/main/resources/static
                    mkdir -p services/api/src/main/resources/static
                    cp -r apps/web/dist/. services/api/src/main/resources/static/
                '''
            }
        }

        stage('Backend Package') {
            steps {
                sh 'mvn -o -q -DskipTests -pl services/api -am package'
            }
        }

        stage('Deploy') {
            steps {
                sh 'sudo systemctl restart domainon'
            }
        }
    }

    post {
        failure {
            echo '배포 실패 — Jenkins 콘솔 로그를 확인하세요.'
        }
    }
}
