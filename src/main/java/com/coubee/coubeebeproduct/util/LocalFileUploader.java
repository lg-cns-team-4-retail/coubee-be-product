package com.coubee.coubeebeproduct.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Profile({"local"})
@Slf4j
@Component
public class LocalFileUploader implements FileUploader {

    @Value("${file.upload.base-dir}")
    private String baseDir;

    @Value("${file.upload.resource-url}")
    private String resourceUrl;
    @Override
    public String upload(MultipartFile file, String dirName) {
        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(file.getOriginalFilename());
        String fileName = dirName + "/" + uuid + ext;
        File dest = new File(baseDir + File.separator + fileName);
        dest.getParentFile().mkdirs();

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("로컬 파일 업로드 실패", e);
        }

        // 웹 URL 경로로 반환
        return resourceUrl.replace("**", "") + fileName.replace(File.separator, "/");
    }
    @Override
    public void delete(String fileUrl) {
        File file = new File(fileUrl);
        if (file.exists()) {
            file.delete();
        }
    }

    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    private static String getDesktopPath() {
        String home = System.getProperty("user.home");
        return home + File.separator + "Desktop"; // Mac/Linux/Windows 호환 바탕화면 경로
    }
}