run server
java -jar server/target/server-1.0-SNAPSHOT.jar --port 8080 --directory src/main/resources/www

run client
get
java -jar client/target/client-1.0-SNAPSHOT.jar --url http://localhost:8080/index.html --method GET
post
java -jar client/target/client-1.0-SNAPSHOT.jar -u localhost:8080/test -m POST -f C:/Users/Broken/Pictures/g81.jpg

post
curl -X POST -F "file=@C:/Users/Broken/Pictures/test.rar" http://localhost:8080/upload