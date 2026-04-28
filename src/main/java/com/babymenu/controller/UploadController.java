package com.babymenu.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.babymenu.common.BizException;
import com.babymenu.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.url-prefix}")
    private String urlPrefix;

    @PostMapping("/image")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("文件不能为空");
        String orig = file.getOriginalFilename() == null ? "img.jpg" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf(".")) : ".jpg";
        String filename = IdUtil.fastSimpleUUID() + ext;
        File dest = new File(uploadPath, filename);
        FileUtil.mkParentDirs(dest);
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new BizException("上传失败: " + e.getMessage());
        }
        return Result.success(Map.of("url", urlPrefix + filename));
    }
}
