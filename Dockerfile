FROM openjdk:8
COPY target/huemoe.jar /usr/lib/huemoe.jar
CMD HUEMOE_ENV=env java -jar /usr/lib/huemoe.jar
