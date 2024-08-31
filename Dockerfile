FROM maven:3.9.9-eclipse-temurin-21

RUN apt-get update && \
    apt-get install -y curl zip unzip \
    && rm -rf /var/lib/apt/lists/*

# Install respective JDK via SDK man
RUN rm /bin/sh && ln -s /bin/bash /bin/sh
RUN curl -s "https://get.sdkman.io" | bash
RUN source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install java 8.0.422-tem && \
    sdk install java 11.0.24-tem && \
    sdk install java 17.0.12-tem && \
    sdk install java 21.0.4-tem

ENV VERSION=999999-SNAPSHOT

# Add jar
ADD plugin-modernizer-cli/target/jenkins-plugin-modernizer-${VERSION}.jar /jenkins-plugin-modernizer.jar
ADD plugin-modernizer-core/target/plugin-modernizer-core-${VERSION}.jar /jenkins-plugin-modernizer-core.jar

# Install core dependency
RUN mvn org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file  \
    -Dfile=/jenkins-plugin-modernizer-core.jar \
    -DgroupId=io.jenkins.plugin-modernizer \
    -DartifactId=plugin-modernizer-core \
    -Dversion=${VERSION} \
    -Dpackaging=jar

ENTRYPOINT ["java", "-jar", "/jenkins-plugin-modernizer.jar"]