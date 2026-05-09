package com.babymenu.controller;

import cn.hutool.core.util.IdUtil;
import com.babymenu.common.BizException;
import com.babymenu.common.Result;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 上传图片到 MinIO
     *
     * @param file 图片文件
     * @return 访问 URL
     * @author wb
     * @date 2026-05-05
     */
    @PostMapping("/image")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String orig = file.getOriginalFilename() == null ? "img.jpg" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf(".")) : ".jpg";
        String filename = IdUtil.fastSimpleUUID() + ext;

        try (InputStream in = file.getInputStream()) {
            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                // 如果不存在，则创建一个新的存储桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                
                // 设置存储桶策略为公共读
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            }

            // 使用 putObject 上传一个文件到存储桶中
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(in, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 构建访问 URL
            String url = endpoint + "/" + bucketName + "/" + filename;
            return Result.success(Map.of("url", url));
        } catch (Exception e) {
            log.error("上传文件到MinIO错误", e);
            throw new BizException("上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传语音到 MinIO
     */
    @PostMapping("/voice")
    public Result<Map<String, String>> uploadVoice(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String orig = file.getOriginalFilename() == null ? "voice.aac" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf(".")) : ".aac";
        String filename = IdUtil.fastSimpleUUID() + ext;

        try (InputStream in = file.getInputStream()) {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(in, file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "audio/aac")
                            .build()
            );

            String url = endpoint + "/" + bucketName + "/" + filename;
            return Result.success(Map.of("url", url));
        } catch (Exception e) {
            log.error("上传语音到MinIO错误", e);
            throw new BizException("上传失败: " + e.getMessage());
        }
    }
}
