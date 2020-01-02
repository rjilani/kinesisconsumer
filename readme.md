This is a demo application that shows how to use Kineis KCL library to read the record from Kinesis Stream

Usage: mvn clean package


How to run:
java -jar target/kinesisconsumer.jar

Note: This client creates Dynamodb table, so please make sure your role has access to create the dyanmodb in the same region where the stream exist
