package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService
{

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Override
    public String upload(MultipartFile file)
    {
        String url = "";
        try
        {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioConstantProperties.getEndpointUrl())
                    .credentials(minioConstantProperties.getAccessKey(), minioConstantProperties.getSecreKey())
                    .build();
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            if (!found)
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            String originalFileName = "images/" + UUID.randomUUID().toString().replace("-", "") + "." + FilenameUtils.getExtension(file.getOriginalFilename());
            minioClient.putObject(PutObjectArgs.builder().bucket(minioConstantProperties.getBucketName()).object(originalFileName).stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build());
            url = minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + originalFileName;
            System.out.println("url:" +  url);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return url;
    }
}
