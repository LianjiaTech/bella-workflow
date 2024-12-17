package com.ke.bella.workflow.service;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

@Component
public class AmazonS3Service {
    private final AmazonS3 s3;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.root}")
    private String root;

    public AmazonS3Service(@Value("${s3.ak}") String ak,
            @Value("${s3.sk}") String sk,
            @Value("${s3.endpoint}") String endpoint) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(ak, sk);
        AWSStaticCredentialsProvider provider = new AWSStaticCredentialsProvider(credentials);
        this.s3 = AmazonS3ClientBuilder.standard().withCredentials(provider)
                .withEndpointConfiguration(new EndpointConfiguration(endpoint, Regions.CN_NORTH_1.getName()))
                .withPathStyleAccessEnabled(true).build();
    }

    public void putObject(String key, String content) {
        String path = String.format("%s/%s", root, key);
        s3.putObject(bucket, path, content);
    }

    public void putObject(String key, InputStream is) {
        String path = String.format("%s/%s", root, key);
        s3.putObject(bucket, path, is, new ObjectMetadata());
    }

    public S3Object getObject(String key) {
        try {
            String path = String.format("%s/%s", root, key);
            return s3.getObject(bucket, path);
        } catch (AmazonServiceException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
