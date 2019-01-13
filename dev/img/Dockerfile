FROM java:8-jdk

MAINTAINER Grigory Rozhkov

RUN apt-get update && \
    apt-get install -y vim openssh-server lsof telnet git libviennacl-dev
ENV SPARK_VERSION 2.1.3
ENV HADOOP_VERSION 2.4.0
ENV SPARK_HADOOP_VERSION 2.4
ENV SPARK_HOME /usr/local/spark
ENV HADOOP_HOME /usr/local/hadoop

WORKDIR /tmp

# Download service distributions
RUN wget --quiet https://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz && \
    tar -xzf hadoop-${HADOOP_VERSION}.tar.gz
RUN wget --quiet https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION}.tgz && \
    tar -xzf spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION}.tgz

RUN mv spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION} /usr/local/spark
RUN mv hadoop-${HADOOP_VERSION}                                 /usr/local/hadoop

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/

# set JAVA_HOME for hadoop
RUN printf '%s\n%s\n' "export JAVA_HOME=$JAVA_HOME" "$(cat $HADOOP_HOME/etc/hadoop/hadoop-env.sh)" > $HADOOP_HOME/etc/hadoop/hadoop-env.sh

ADD ./hdfs/* $HADOOP_HOME/etc/hadoop/

RUN mkdir -p /usr/local/hadoop/hdfs/namenode \
	&& mkdir -p /usr/local/hadoop/hdfs/datanode

ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$SPARK_HOME/bin:$SPARK_HOME/sbin

# setup ssh with no passphrase
RUN ssh-keygen -t rsa -f $HOME/.ssh/id_rsa -P "" \
    && cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys
ADD ssh/config /root/.ssh/config

WORKDIR /usr/local

ADD spark/ $SPARK_HOME/

ADD tmp/export_job-assembly-0.1.jar     /usr/local/
ADD tmp/import_job-assembly-0.1.jar     /usr/local/
ADD tmp/similarity_job-assembly-0.1.jar /usr/local/
ADD tmp/job_runner-assembly-0.1.jar     /usr/local/

RUN mkdir -p /usr/local/scripts
ADD export_events.sh /usr/local/scripts/
ADD train_model.sh /usr/local/scripts/
ADD export_model.sh /usr/local/scripts/
RUN chmod a+x /usr/local/scripts/*

EXPOSE 22
