FROM openjdk:11-jdk

RUN apt-get update
RUN apt-get install     apt-transport-https     ca-certificates     curl     gnupg2     software-properties-common -y
RUN  curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN  apt-key fingerprint 0EBFCD88
RUN add-apt-repository    "deb [arch=amd64] https://download.docker.com/linux/debian  $(lsb_release -cs)  stable"
RUN apt-get update
RUN apt-get install python-pip docker-ce docker-ce-cli -y
RUN pip install awscli
RUN apt-get install rsync -y
RUN mkdir -p ~/.ssh ; chmod 700 ~/.ssh; ssh-keyscan si-jenkins02.dev.setl.io >> ~/.ssh/known_hosts ; chmod 644 ~/.ssh/known_hosts
