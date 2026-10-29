# calculator-devops

A Calculator REST API (Java 17, Spring Boot 3, Maven) with an automated CI/CD pipeline built from
**GitHub → Jenkins → Docker → Docker Compose**. This is the CSE 816 Software Production Engineering mini-project.

A push to `main` triggers Jenkins, which builds, tests, packages, containerizes, pushes to Docker Hub
and redeploys the stack with Docker Compose, with no manual steps.

## API

| Operation | Endpoint                          | Behaviour                                 |
|-----------|-----------------------------------|-------------------------------------------|
| Add       | `GET /api/v1/add?a=&b=`           | `a + b`                                   |
| Subtract  | `GET /api/v1/subtract?a=&b=`      | `a - b`                                   |
| Multiply  | `GET /api/v1/multiply?a=&b=`      | `a * b`                                   |
| Divide    | `GET /api/v1/divide?a=&b=`        | `a / b`; `b=0` returns HTTP 400           |
| Health    | `GET /actuator/health`            | `{"status":"UP"}`                         |

Success:

```json
{ "operation": "add", "a": 2.0, "b": 3.0, "result": 5.0 }
```

Error (HTTP 400). This is returned for division by zero, a missing parameter, a non-numeric value,
`NaN`/`Infinity` inputs, or a result that overflows:

```json
{
  "timestamp": "2026-10-29T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Division by zero is not allowed",
  "path": "/api/v1/divide"
}
```

## Project layout

```
calculator-devops/
├── src/main/java/com/spe/calculator/
│   ├── CalculatorApplication.java
│   ├── controller/CalculatorController.java    # REST endpoints + input validation
│   ├── service/CalculatorService.java          # arithmetic
│   ├── exception/                              # 400 error handling
│   └── model/                                  # JSON response records
├── src/test/java/...                           # JUnit 5 unit + MockMvc integration tests
├── pom.xml
├── Dockerfile                                  # multi-stage: Maven build -> JRE runtime
├── .dockerignore
├── docker-compose.yml                          # calculator API + Nginx reverse proxy
├── nginx/                                      # proxy image (Dockerfile + nginx.conf)
├── Jenkinsfile                                 # declarative CI/CD pipeline
├── jenkins/                                    # Jenkins itself, as code (image, plugins, JCasC, job)
└── .gitignore
```

## Build, test and run locally

Requires JDK 17 and Maven 3.9+.

```bash
mvn clean package          # build + run tests -> target/calculator.jar
mvn test                   # tests only; JUnit XML in target/surefire-reports/
java -jar target/calculator.jar
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

## Docker

```bash
docker build -t calculator .
docker run --rm -p 8080:8080 calculator
curl "http://localhost:8080/api/v1/divide?a=7&b=2"
```

The image is built in two stages. The first is a Maven/JDK stage, where dependencies sit in their own
cached layer. The second is an `eclipse-temurin:17-jre-alpine` runtime that holds only the JAR and runs
as a non-root user.

## Docker Compose

```bash
docker compose up -d --build --wait     # builds and starts the full stack, waits until healthy
docker compose ps
curl "http://localhost:8081/api/v1/multiply?a=6&b=7"
docker compose down
```

| Service      | Image                    | Purpose                                                        |
|--------------|--------------------------|----------------------------------------------------------------|
| `calculator` | `${IMAGE_NAME}:${IMAGE_TAG}` | API on internal port 8080; health check on `/actuator/health` |
| `proxy`      | `calculator-proxy` (built from `nginx/`, based on `nginx:1.27-alpine`) | Reverse proxy published on host port `8081` (`PROXY_PORT`) |

Both services share the `calc-net` bridge network and use `restart: unless-stopped`. The proxy starts
only after the API reports healthy. Nginx re-resolves the `calculator` hostname at runtime, so a
redeploy that recreates the API container does not break the proxy.

## Jenkins pipeline

| Stage        | What it does                                                                 |
|--------------|------------------------------------------------------------------------------|
| Checkout     | Pulls the commit that triggered the build                                    |
| Build        | `mvn clean package -DskipTests`                                              |
| Test         | `mvn test`; publishes JUnit results; the build fails if any test fails        |
| Docker Build | Builds `IMAGE_NAME:BUILD_NUMBER` and `IMAGE_NAME:latest`                     |
| Push         | Logs in with Jenkins-stored credentials and pushes both tags to Docker Hub   |
| Deploy       | `docker compose up -d --wait`, then smoke-tests health and `/api/v1/add` through the proxy |

On success or failure, Jenkins emails the build status.

The deploy step is idempotent. Compose recreates only containers whose configuration changed, so a
re-run against a running stack leaves it running.

### Running Jenkins (recommended: Jenkins as code)

The [`jenkins/`](jenkins/) folder defines the whole Jenkins server:

- **Image:** LTS Jenkins with JDK 21, plus Maven, the Docker CLI, Compose and Buildx.
- **Plugins:** listed in `plugins.txt`.
- **Configuration as Code (`casc.yaml`):** creates the admin user, the Docker Hub credential, Gmail
  SMTP settings for email notifications, and the `calculator-devops` pipeline job.

Pipeline steps call Docker on the host through the mounted `/var/run/docker.sock`.

```bash
cp jenkins/.env.example jenkins/.env      # fill in the secrets; .env is git-ignored
docker compose -f jenkins/docker-compose.yml up -d --build
# open http://localhost:8090 and log in with JENKINS_ADMIN_ID / JENKINS_ADMIN_PASSWORD
```

The job polls GitHub every 2 minutes. It also accepts GitHub webhooks at `/github-webhook/` whenever
Jenkins is reachable from the internet. Its first run starts automatically.

### Manual Jenkins setup (alternative)

1. **Plugins:** Pipeline, Git, GitHub, JUnit, Email Extension, Credentials Binding. All except Email
   Extension come with the suggested plugins.
2. **Agent tools:** `mvn`, `docker` (with Compose v2) and `curl` must be on the agent's `PATH`. The
   Jenkins user also needs permission to use Docker. On Linux, run `sudo usermod -aG docker jenkins`.
3. **Docker Hub credentials:** go to *Manage Jenkins → Credentials → Global → Add*. Choose *Username
   with password* with ID `dockerhub-credentials`. Use a Docker Hub **access token** as the password.
4. **Image name:** set `IMAGE_NAME` at the top of the `Jenkinsfile` to `<dockerhub-user>/calculator-devops`.
5. **Email:**
   - Under *Manage Jenkins → System → Extended E-mail Notification*, configure SMTP: for Gmail, use
     `smtp.gmail.com`, port 465, SSL, and an app password.
   - Under *Global properties → Environment variables*, add `NOTIFY_EMAIL` with your address.
6. **Job:** create a *Pipeline* job → *Pipeline script from SCM* → Git → this repo, branch `*/main`,
   script path `Jenkinsfile`.
7. **Trigger:** in the GitHub repo, go to *Settings → Webhooks* and add
   `http://<jenkins-host>/github-webhook/` with content type `application/json`. If Jenkins runs on
   localhost, expose it with a tunnel such as ngrok, or rely on the `pollSCM` fallback, which checks
   every 2 minutes.

## Docker Hub

Image: `https://hub.docker.com/r/<dockerhub-user>/calculator-devops`
