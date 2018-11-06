FROM java:8-jdk

MAINTAINER Grigory Rozhkov

RUN apt-get update && \
    apt-get install -y vim openssh-server lsof telnet git libviennacl-dev
ENV SPARK_VERSION 2.1.3
ENV HADOOP_VERSION 2.4.0
ENV SPARK_HADOOP_VERSION 2.4
ENV MAHOUT_VERSION 0.13.0
ENV SPARK_HOME /usr/local/spark
ENV HADOOP_HOME /usr/local/hadoop
ENV MAHOUT_HOME /usr/local/mahout

WORKDIR /tmp

# install maven
RUN wget http://www-eu.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
RUN tar -xvzf apache-maven-3.3.9-bin.tar.gz
RUN mv apache-maven-3.3.9 /opt/maven
ENV M2_HOME /opt/maven
ENV PATH $M2_HOME/bin:$PATH


# Download service distributions
#RUN wget --quiet http://www-us.apache.org/dist/mahout/${MAHOUT_VERSION}/apache-mahout-distribution-${MAHOUT_VERSION}.tar.gz && \
#    tar -xzf apache-mahout-distribution-${MAHOUT_VERSION}.tar.gz
RUN wget --quiet https://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz && \
    tar -xzf hadoop-${HADOOP_VERSION}.tar.gz
RUN wget --quiet https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION}.tgz && \
    tar -xzf spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION}.tgz

RUN mv spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION} /usr/local/spark
RUN mv hadoop-${HADOOP_VERSION}                                 /usr/local/hadoop

WORKDIR /usr/local

RUN git clone https://github.com/apache/mahout.git

WORKDIR /usr/local/mahout
RUN git checkout tags/mahout-$MAHOUT_VERSION -b $MAHOUT_VERSION

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN mvn clean install -DskipTests

# set JAVA_HOME for hadoop
RUN printf '%s\n%s\n' "export JAVA_HOME=$JAVA_HOME" "$(cat $HADOOP_HOME/etc/hadoop/hadoop-env.sh)" > $HADOOP_HOME/etc/hadoop/hadoop-env.sh

ADD ./hdfs/* $HADOOP_HOME/etc/hadoop/

RUN mkdir -p /usr/local/hadoop/hdfs/namenode \
	&& mkdir -p /usr/local/hadoop/hdfs/datanode

ADD ./mahout/mahout $MAHOUT_HOME/bin/
RUN chmod a+x $MAHOUT_HOME/bin/mahout

ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$SPARK_HOME/bin:$SPARK_HOME/sbin:$MAHOUT_HOME/bin:$MAHOUT_HOME/sbin

RUN rm -rf /tmp/*

# setup ssh with no passphrase
RUN ssh-keygen -t rsa -f $HOME/.ssh/id_rsa -P "" \
    && cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys
ADD ssh/config /root/.ssh/config

WORKDIR /usr/local

ADD spark/ $SPARK_HOME/

ADD tmp/batch_jobs-assembly-0.1.jar /usr/local/
ADD tmp/job_runner-assembly-0.1.jar /usr/local/

RUN mkdir -p /usr/local/scripts
ADD export_events.sh /usr/local/scripts/
ADD train_model.sh /usr/local/scripts/
ADD export_model.sh /usr/local/scripts/
RUN chmod a+x /usr/local/scripts/*

EXPOSE 22
