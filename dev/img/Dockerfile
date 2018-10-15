FROM java:8-jre

MAINTAINER Grigory Rozhkov

RUN apt-get update
RUN apt-get install -y vim openssh-server lsof telnet

ENV SPARK_VERSION 1.6.3
ENV HADOOP_VERSION 2.6.0
ENV SPARK_HADOOP_VERSION 2.6
ENV MAHOUT_VERSION 0.13.0
ENV SPARK_HOME /usr/local/spark
ENV HADOOP_HOME /usr/local/hadoop
ENV MAHOUT_HOME /usr/local/mahout
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
WORKDIR /tmp

# Download service distributions
RUN wget --quiet http://www-us.apache.org/dist/mahout/${MAHOUT_VERSION}/apache-mahout-distribution-${MAHOUT_VERSION}.tar.gz && \
    tar -xzf apache-mahout-distribution-${MAHOUT_VERSION}.tar.gz
RUN wget --quiet https://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz && \
    tar -xzf hadoop-${HADOOP_VERSION}.tar.gz
RUN wget --quiet https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION}.tgz && \
    tar -xzf spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION}.tgz

RUN mv spark-${SPARK_VERSION}-bin-hadoop${SPARK_HADOOP_VERSION} /usr/local/spark
RUN mv hadoop-${HADOOP_VERSION}                                 /usr/local/hadoop
RUN mv apache-mahout-distribution-${MAHOUT_VERSION}             /usr/local/mahout

# RUN ln -sf ${MAHOUT_HOME}/bin/mahout /usr/local/bin/mahout

# set JAVA_HOME for hadoop
RUN printf '%s\n%s\n' "export JAVA_HOME=$JAVA_HOME" "$(cat $HADOOP_HOME/etc/hadoop/hadoop-env.sh)" > $HADOOP_HOME/etc/hadoop/hadoop-env.sh

ADD ./hdfs/* $HADOOP_HOME/etc/hadoop/

RUN mkdir -p /usr/local/hadoop/hdfs/namenode \
	&& mkdir -p /usr/local/hadoop/hdfs/datanode


ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$SPARK_HOME/bin:$SPARK_HOME/sbin:$MAHOUT_HOME/bin:$MAHOUT_HOME/sbin


#COPY config/ /tmp/
#RUN mv /tmp/ssh_config $HOME/.ssh/config \
#    && mv /tmp/hadoop-env.sh $HADOOP_HOME/etc/hadoop/hadoop-env.sh \
#    && mv /tmp/core-site.xml $HADOOP_HOME/etc/hadoop/core-site.xml \
#    && mv /tmp/hdfs-site.xml $HADOOP_HOME/etc/hadoop/hdfs-site.xml \
#    && mv /tmp/mapred-site.xml $HADOOP_HOME/etc/hadoop/mapred-site.xml.template \
#    && cp $HADOOP_HOME/etc/hadoop/mapred-site.xml.template $HADOOP_HOME/etc/hadoop/mapred-site.xml \
#    && mv /tmp/yarn-site.xml $HADOOP_HOME/etc/hadoop/yarn-site.xml \
#    && cp /tmp/slaves $HADOOP_HOME/etc/hadoop/slaves \
#    && mv /tmp/slaves $SPARK_HOME/conf/slaves \
#    && mv /tmp/spark/spark-env.sh $SPARK_HOME/conf/spark-env.sh \
#    && mv /tmp/spark/log4j.properties $SPARK_HOME/conf/log4j.properties

RUN rm -rf /tmp/*

# setup ssh with no passphrase
RUN ssh-keygen -t rsa -f $HOME/.ssh/id_rsa -P "" \
    && cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys
ADD ssh/config /root/.ssh/config

WORKDIR /usr/local

ADD spark/ $SPARK_HOME/

EXPOSE 22
EXPOSE 9021
# Spark
EXPOSE 7077
EXPOSE 8080

# ENTRYPOINT service ssh start; /opt/entrypoint.sh; /usr/local/spark-2.3.2-bin-hadoop2.7/sbin/start-master.sh
#CMD ["/usr/local/spark-${SPARK_VERSION}-bin-hadoop2.7/sbin/start-dfs.sh"]
