FROM openjdk:8
COPY target/huemoe-0.1.0-SNAPSHOT-standalone.jar /usr/lib/huemoe.jar
CMD HUEMOE_ENV=env java -jar /usr/lib/huemoe.jar
