FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

ARG JAVA_VERSION=25
ARG NODE_VERSION=20
ARG PYTHON_VERSION=3

# System-Pakete: Graphviz (fuer PlantUML), build-essentials
RUN apt-get update && apt-get install -y --no-install-recommends \
    graphviz \
    curl \
    unzip \
    python${PYTHON_VERSION} \
    python${PYTHON_VERSION}-venv \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# SDKMAN + JDK
RUN curl -s "https://get.sdkman.io" | bash
ENV SDKMAN_DIR="/root/.sdkman"
RUN bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java ${JAVA_VERSION}-tem"
# Symlink fuer vorhersagbaren Pfad
RUN ln -s /root/.sdkman/candidates/java/current /usr/lib/jvm/java-25-openjdk-amd64

# SDKMAN fuer vscode-User
USER vscode
RUN curl -s "https://get.sdkman.io" | bash
ENV SDKMAN_DIR="/home/vscode/.sdkman"
RUN bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java ${JAVA_VERSION}-tem"
ENV JAVA_HOME="/home/vscode/.sdkman/candidates/java/current"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# nvm + Node.js
ENV NVM_DIR="/home/vscode/.nvm"
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash \
    && bash -c "source $NVM_DIR/nvm.sh && nvm install ${NODE_VERSION} && nvm alias default ${NODE_VERSION}"
ENV PATH="${NVM_DIR}/versions/node/v${NODE_VERSION}.20.2/bin:${PATH}"

USER root

# PlantUML Server als Sidecar wird via docker-compose gestartet
