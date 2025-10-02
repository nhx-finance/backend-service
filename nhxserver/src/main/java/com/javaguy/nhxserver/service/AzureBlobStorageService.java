package com.javaguy.nhxserver.service;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AzureBlobStorageService {

    @Value("${azure.storage.account-name}")
    private String accountName;

    @Value("${azure.storage.account-key}")
    private String accountKey;

    @Value("${azure.storage.container-name}")
    private String containerName;

    private BlobContainerClient blobContainerClient;


    public void init() {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", accountName))
                .credential(credential)
                .buildClient();
        this.blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
            log.info("Created Azure Blob Storage container: {}", containerName);
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String blobFileName = UUID.randomUUID() + fileExtension;
        BlobClient blobClient = blobContainerClient.getBlobClient(blobFileName);

        try (InputStream inputStream = file.getInputStream()) {
            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType(file.getContentType());
            headers.setContentDisposition("inline");
            BlobParallelUploadOptions uploadOptions = new BlobParallelUploadOptions(inputStream);
            uploadOptions.setHeaders(headers);
            blobClient.uploadWithResponse(uploadOptions,null, Context.NONE);
            log.info("Uploaded blob: {} to container: {}", blobFileName, containerName);
            return getBlobSasUrl(blobFileName);
        } catch (IOException e) {
            log.error("Failed to upload image to Azure Blob Storage: {}", e.getMessage());
            throw new IOException("Failed to upload image to Azure Blob Storage", e);
        }
    }

    public void deleteImage(String imageUrl) {
        String blobName = extractBlobNameFromUrl(imageUrl);
        if (blobName == null) {
            log.warn("Could not extract blob name from URL: {}", imageUrl);
            return;
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.delete();
            log.info("Deleted blob: {} from container: {}", blobName, containerName);
        } else {
            log.warn("Blob not found for deletion: {}", blobName);
        }
    }

    public String getBlobSasUrl(String blobName) {
        if (blobName == null || blobName.isEmpty()) {
            return null;
        }
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        if (!blobClient.exists()) {
            return null;
        }
        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);

        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permission);
        String sasToken = blobClient.generateSas(sasSignatureValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
        
    }

    private String extractBlobNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        int lastSlashIndex = imageUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < imageUrl.length() - 1) {
            int queryParamIndex = imageUrl.indexOf('?', lastSlashIndex);
            if (queryParamIndex != -1) {
                return imageUrl.substring(lastSlashIndex + 1, queryParamIndex);
            } else {
                return imageUrl.substring(lastSlashIndex + 1);
            }
        }
        return null;
    }
}
