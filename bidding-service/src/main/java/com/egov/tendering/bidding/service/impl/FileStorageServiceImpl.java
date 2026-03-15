package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.exception.FileStorageException;
import com.egov.tendering.bidding.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${app.file-storage.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage location initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String prefix) {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Failed to store empty or null file");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");

        // Check for invalid characters in filename
        if (originalFileName.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + originalFileName);
        }

        try {
            // Extract file extension
            String fileExtension = "";
            int lastDotIndex = originalFileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                fileExtension = originalFileName.substring(lastDotIndex);
            }

            // Generate unique filename
            String sanitizedPrefix = prefix.replaceAll("[^a-zA-Z0-9-_]", "_");
            String fileName = sanitizedPrefix + "-" + UUID.randomUUID() + fileExtension;

            // Copy file to the target location
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Successfully stored file: {} as {}", originalFileName, fileName);
            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file " + originalFileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Invalid file path: " + fileName);
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                log.error("File not found: {}", fileName);
                throw new FileStorageException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + fileName, ex);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            Path file = this.fileStorageLocation.resolve(fileName).normalize();
            if (!file.startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Invalid file path: " + fileName);
            }
            boolean result = Files.deleteIfExists(file);
            if (result) {
                log.info("Successfully deleted file: {}", fileName);
            } else {
                log.warn("File does not exist and could not be deleted: {}", fileName);
            }
            return result;
        } catch (IOException ex) {
            log.error("Error deleting file: {}", fileName, ex);
            return false;
        }
    }
}
