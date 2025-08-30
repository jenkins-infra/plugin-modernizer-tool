# Define the VERSION argument with a default value
ARG VERSION=999999-SNAPSHOT

# Create the image using Maven and Eclipse Temurin JDK 21
FROM maven:3.9.11-eclipse-temurin-21-noble AS result-image

LABEL org.opencontainers.image.description="Using OpenRewrite Recipes for Plugin Modernization or Automation Plugin Build Metadata Updates"

# Update package lists and install necessary packages
RUN apt-get update && \
    apt-get install -y curl zip unzip && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables for JDK versions managed by SDKMAN
ENV JDK8_PACKAGE=8.0.462-tem
ENV JDK11_PACKAGE=11.0.28-tem
ENV JDK17_PACKAGE=17.0.16-tem
ENV JDK21_PACKAGE=21.0.8-tem
ENV MVN_INSTALL_PLUGIN_VERSION=3.1.4

# Replace the default shell with bash
RUN rm /bin/sh && ln -s /bin/bash /bin/sh

# Install SDKMAN and respective JDKs
RUN curl -s "https://get.sdkman.io" | bash
RUN source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install java $JDK8_PACKAGE && \
    sdk install java $JDK11_PACKAGE && \
    sdk install java $JDK17_PACKAGE && \
    sdk install java $JDK21_PACKAGE

# Re-define the VERSION argument for the result-image stage
ARG VERSION

# Set the VERSION environment variable
ENV VERSION=${VERSION}

# Copy the built JAR files from the downloaded artifacts to the final image
# Fail fast if jars are missing
COPY --chmod=755 plugin-modernizer-cli/target/jenkins-plugin-modernizer-${VERSION}.jar /jenkins-plugin-modernizer.jar
COPY --chmod=755 plugin-modernizer-core/target/plugin-modernizer-core-${VERSION}.jar /jenkins-plugin-modernizer-core.jar

# Install the core dependency using the Maven install plugin
RUN mvn org.apache.maven.plugins:maven-install-plugin:${MVN_INSTALL_PLUGIN_VERSION}:install-file \
    -Dfile=/jenkins-plugin-modernizer-core.jar \
    -DgroupId=io.jenkins.plugin-modernizer \
    -DartifactId=plugin-modernizer-core \
    -Dversion=${VERSION} \
    -Dpackaging=jar

# Set the entry point for the Docker container to run the main JAR file
ENTRYPOINT ["java", "-jar", "/jenkins-plugin-modernizer.jar"]
