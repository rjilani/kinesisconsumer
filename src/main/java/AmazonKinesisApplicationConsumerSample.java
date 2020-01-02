
/*
 * Copyright 2012-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.net.InetAddress;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;

/**
 * Sample Amazon Kinesis Application.
 */
public final class AmazonKinesisApplicationConsumerSample {

    /*
     * Before running the code:
     *      Fill in your AWS access credentials in the provided credentials
     *      file template, and be sure to move the file to the default location
     *      (~/.aws/credentials) where the sample code will load the
     *      credentials from.
     *      https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING:
     *      To avoid accidental leakage of your credentials, DO NOT keep
     *      the credentials file in your source directory.
     */

    public static final String SAMPLE_APPLICATION_STREAM_NAME = "ExampleInputStream";

    private static String SAMPLE_APPLICATION_NAME = "SampleKinesisApplicationRashidJilani01012019111";

    // Initial position in the stream when the application starts up for the first time.
    // Position can be one of LATEST (most recent data) or TRIM_HORIZON (oldest available data)
    private static final InitialPositionInStream SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM =
            InitialPositionInStream.TRIM_HORIZON;

    private static ProfileCredentialsProvider credentialsProvider;

    private static void init() {
        // Ensure the JVM will refresh the cached IP values of AWS resources (e.g. service endpoints).
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
            System.out.println("rashid " + credentialsProvider.getCredentials().getAWSAccessKeyId());
        } catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
        }
    }

    public static void main(String[] args) throws Exception {
        init();

        if (args.length == 1 && "delete-resources".equals(args[0])) {
            deleteResources();
            return;
        }

        if (args.length == 1) {
            SAMPLE_APPLICATION_NAME = args[0];
            System.out.println("Application name is provided: " + args[0]);
        }

        String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        KinesisClientLibConfiguration kinesisClientLibConfiguration =
                new KinesisClientLibConfiguration(SAMPLE_APPLICATION_NAME,
                        SAMPLE_APPLICATION_STREAM_NAME,
                        credentialsProvider,
                        workerId);
        kinesisClientLibConfiguration.withRegionName("us-west-2");
        kinesisClientLibConfiguration.withInitialPositionInStream(SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM);

        IRecordProcessorFactory recordProcessorFactory = new AmazonKinesisApplicationRecordProcessorFactory();

        //Use builder this is deprecated
//        Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration);

        Worker worker = new Worker.Builder()
                .recordProcessorFactory(recordProcessorFactory)
                .config(kinesisClientLibConfiguration)
                .build();

        System.out.printf("Running %s to process stream %s as worker %s...\n",
                SAMPLE_APPLICATION_NAME,
                SAMPLE_APPLICATION_STREAM_NAME,
                workerId);

        int exitCode = 0;
        try {
            worker.run();
        } catch (Throwable t) {
            System.err.println("Caught throwable while processing data.");
            t.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    public static void deleteResources() {
        // Delete the stream
        AmazonKinesis kinesis = AmazonKinesisClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("us-west-2")
                .build();

        System.out.printf("Deleting the Amazon Kinesis stream used by the sample. Stream Name = %s.\n",
                SAMPLE_APPLICATION_STREAM_NAME);
        try {
            kinesis.deleteStream(SAMPLE_APPLICATION_STREAM_NAME);
        } catch (ResourceNotFoundException ex) {
            System.out.println("The stream " + SAMPLE_APPLICATION_STREAM_NAME + " doesn't exist, there is no stream to be deleted: " );
        }

        // Delete the table
        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("us-west-2")
                .build();
        System.out.printf("Deleting the Amazon DynamoDB table used by the Amazon Kinesis Client Library. Table Name = %s.\n",
                SAMPLE_APPLICATION_NAME);
        try {
            dynamoDB.deleteTable(SAMPLE_APPLICATION_NAME);
        } catch (com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException ex) {
            System.out.println("The dynamodb table " + SAMPLE_APPLICATION_NAME + " doesn't exist, there is no table to be deleted.");
        }
    }
}

