FROM ubuntu:16.04

MAINTAINER Peizhao Hu, http://cs.rit.edu/~ph
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get install -y openssh-server net-tools iputils-ping ant nmap wget git vim build-essential && \
    apt-get clean
WORKDIR PubSubAgent
EXPOSE 8888
COPY PubSubAgent.jar PubSubAgent.jar
#CMD java -jar PubSubAgent.jar eventmanager

