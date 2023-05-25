FROM amazoncorretto:17

WORKDIR $HOME/app

COPY build/libs/self-serve-login-rback-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

CMD ["java", "-jar", "self-serve-login-rback-service-0.0.1-SNAPSHOT.jar" ]