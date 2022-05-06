# adapted from https://github.com/eclipse/hawkbit/blob/master/hawkbit-runtime/docker/0.3.0M7/Dockerfile
FROM adoptopenjdk/openjdk8:jre8u282-b08

ENV HAWKBIT_HOME=/opt/hawkbit

EXPOSE 8080

COPY hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-0.3.0-SNAPSHOT.jar $HAWKBIT_HOME/hawkbit-update-server.jar

VOLUME "$HAWKBIT_HOME/data"

WORKDIR $HAWKBIT_HOME
ENTRYPOINT ["java","-jar","hawkbit-update-server.jar","-Xms768m -Xmx768m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]