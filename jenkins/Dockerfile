# Jenkins controller with the tools the pipeline needs: Maven, the Docker CLI and the Compose plugin.
# Docker commands run against the host's daemon through the mounted /var/run/docker.sock.
FROM jenkins/jenkins:lts-jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# Static Docker CLI, Buildx and Compose binaries from the official image
COPY --from=docker:28-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:28-cli /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

USER jenkins

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Configuration as Code replaces the interactive setup wizard
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    CASC_JENKINS_CONFIG=/var/jenkins_config/casc.yaml
