FROM wowzamedia/wowza-streaming-engine-linux

ENV MAVEN_VERSION 3.6.2

RUN set -ex; \
    apt-get update; \
    apt-get install -y openjdk-8-jre-headless openjdk-8-jdk-headless wget tar gzip git

RUN set -ex; \
    wget -O /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz http://ftp.halifax.rwth-aachen.de/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz; \
    tar -xf /opt/apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt; \
    rm -f /opt/apache-maven-$MAVEN_VERSION-bin.tar.gz; \
    echo 'export PATH="${PATH}:/opt/apache-maven-${MAVEN_VERSION}/bin"' >> /etc/bash.bashrc
ENV PATH "${PATH}:/opt/apache-maven-${MAVEN_VERSION}/bin"
