// Runs shell commands on Linux/macOS agents and batch commands on Windows agents
def run(String unixCmd, String windowsCmd = null) {
    if (isUnix()) {
        sh unixCmd
    } else {
        bat(windowsCmd ?: unixCmd)
    }
}

pipeline {
    agent any

    environment {
        // Change to <your-dockerhub-username>/<repo>
        IMAGE_NAME  = 'adityareddy106/calculator-devops'
        IMAGE_TAG   = "${env.BUILD_NUMBER}"
        PROXY_PORT  = '8081'
        // Jenkins credential of type "Username with password" holding a Docker Hub access token
        DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
        // NOTIFY_EMAIL is set in Manage Jenkins > System > Global properties, so no address is committed here
    }

    triggers {
        // Fires on GitHub webhook pushes (GitHub plugin)
        githubPush()
        // Fallback when Jenkins is not reachable from GitHub (e.g. running on localhost)
        pollSCM('H/2 * * * *')
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                run 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                run 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                run "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
                run 'docker compose build proxy'
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS,
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    run('echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin',
                        'echo %DOCKER_PASS%| docker login -u %DOCKER_USER% --password-stdin')
                    run "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                    run "docker push ${IMAGE_NAME}:latest"
                }
            }
            post {
                always {
                    run 'docker logout'
                }
            }
        }

        stage('Deploy') {
            steps {
                // IMAGE_NAME/IMAGE_TAG/PROXY_PORT are read by docker-compose.yml. Only containers whose
                // config changed are recreated, so re-running against a live stack is safe.
                // --wait blocks until every service's health check reports healthy.
                run 'docker compose up -d --remove-orphans --wait --wait-timeout 180'
                // Smoke tests go through the proxy from inside its container, so they work no matter
                // whether Jenkins runs on the Docker host or in a container of its own
                run 'docker compose exec -T proxy wget -qO- "http://127.0.0.1/actuator/health"'
                run 'docker compose exec -T proxy wget -qO- "http://127.0.0.1/api/v1/add?a=2&b=3"'
                run 'docker compose ps'
            }
        }
    }

    post {
        success {
            notify('SUCCESS')
        }
        failure {
            notify('FAILURE')
        }
        always {
            // Drop dangling layers left behind by rebuilt images
            run 'docker image prune -f'
        }
    }
}

def notify(String status) {
    if (!env.NOTIFY_EMAIL) {
        echo 'NOTIFY_EMAIL is not set; skipping email notification'
        return
    }
    emailext(
        to: env.NOTIFY_EMAIL,
        subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER}: ${status}",
        body: """<p>Build <b>${env.JOB_NAME} #${env.BUILD_NUMBER}</b> finished with status <b>${status}</b>.</p>
                 <p>Image: ${env.IMAGE_NAME}:${env.IMAGE_TAG}</p>
                 <p>Console: <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>""",
        mimeType: 'text/html'
    )
}
