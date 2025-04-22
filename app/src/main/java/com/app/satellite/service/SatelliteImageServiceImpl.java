package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;
import com.app.satellite.model.SatelliteImage;
import com.app.satellite.repository.SatelliteImageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SatelliteImageServiceImpl implements SatelliteImageService {

    private final SatelliteImageRepository satelliteImageRepository;

    @Value("${aws.access-key}")
    private String accessKeyId;

    @Value("${aws.secret-key}")
    private String secretAccessKey;

    @Value("${aws.source-bucket}")
    private String sourceBucketName;

    @Value("${aws.target-bucket}")
    private String targetBucketName;

    @Value("${aws.folder-path}")
    private String targetFolderPath;

    @Value("${aws.region}")
    private String region;

    @Override
    public List<SatelliteImageDTO> getTiffSourceFilesOnly() {
        return listFromBucket(sourceBucketName, "", null);
    }
    
    private List<SatelliteImageDTO> listFromBucket(String bucketName, String prefix, String endsWithExt) {
        List<SatelliteImageDTO> fileList = new ArrayList<>();
        AmazonS3 s3Client = createS3Client();
    
        String continuationToken = null;
        do {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withContinuationToken(continuationToken);
    
            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            result.getObjectSummaries().forEach(s3Object -> {
                String key = s3Object.getKey();
                if (!key.endsWith("/")) {
    
                    //확장자 체크 조건 개선
                    boolean isTiffFile = key.toLowerCase().endsWith(".tif") || key.toLowerCase().endsWith(".tiff");
    
                    if (endsWithExt == null) {
                        if (isTiffFile) {
                            SatelliteImageDTO dto = new SatelliteImageDTO();
                            dto.setName(key);
                            dto.setRemoteUrl(s3Client.getUrl(bucketName, key).toString());
                            fileList.add(dto);
                        }
                    } else if (key.toLowerCase().endsWith(endsWithExt)) {
                        SatelliteImageDTO dto = new SatelliteImageDTO();
                        dto.setName(key);
                        dto.setRemoteUrl(s3Client.getUrl(bucketName, key).toString());
                        fileList.add(dto);
                    }
                }
            });
    
            continuationToken = result.getNextContinuationToken();
        } while (continuationToken != null);
    
        return fileList;
    }
    

    @Override
    public List<SatelliteImageDTO> getFileList() {
        List<SatelliteImageDTO> fileList = new ArrayList<>();
        try {
            AmazonS3 s3Client = createS3Client();

            String continuationToken = null;
            do {
                ListObjectsV2Request request = new ListObjectsV2Request()
                        .withBucketName(targetBucketName)
                        .withPrefix(targetFolderPath)
                        .withContinuationToken(continuationToken);

                ListObjectsV2Result result = s3Client.listObjectsV2(request);

                result.getObjectSummaries().forEach(s3Object -> {
                    String key = s3Object.getKey();
                    SatelliteImageDTO dto = new SatelliteImageDTO();
                    dto.setName(key);
                    dto.setRemoteUrl(s3Client.getUrl(targetBucketName, key).toString());
                    fileList.add(dto);
                });

                continuationToken = result.getNextContinuationToken();
            } while (continuationToken != null);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("S3 파일 목록 가져오기 실패", e);
        }

        return fileList;
    }

    @Override
    public void saveMetadata(SatelliteImageDTO dto) {
        SatelliteImage entity = new SatelliteImage();
        entity.setName(dto.getName());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setBandCount(dto.getBandCount());
        entity.setUserName(dto.getUserName());
        entity.setSequence(dto.getSequence());
        entity.setCogPath(dto.getCogPath());

        satelliteImageRepository.save(entity);
        System.out.println("DB 저장 완료: " + dto.getName());
    }

    @Override
    public void convertImageToCog(SatelliteImageDTO satelliteImageDTO) {
        try {
            AmazonS3 s3Client = createS3Client();
            String sourceFileKey = satelliteImageDTO.getName();

            File downloadedFile = downloadFileFromS3(s3Client, sourceBucketName, sourceFileKey);
            String outputFilePath = convertToCogFormat(downloadedFile);
            System.out.println("변환된 COG 파일 경로: " + outputFilePath);

            if (outputFilePath != null && new File(outputFilePath).exists()) {
            } else {
                System.err.println("변환된 파일이 존재하지 않습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void convertBatchImagesToCog(List<SatelliteImageDTO> satelliteImageDTOList) {
        throw new UnsupportedOperationException("예시 메서드");
    }

    @Override
    public void uploadConvertedImageToS3(SatelliteImageDTO satelliteImageDTO) {
        String fullKey = satelliteImageDTO.getName();
    
        if (fullKey == null || fullKey.isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
    
        String fileNameOnly = new File(fullKey).getName();
        
        String tempDir = System.getProperty("java.io.tmpdir"); 
        String cogFilePath = tempDir + fileNameOnly.replace(".tiff", "-to-cog.tiff");
    
        File cogFile = new File(cogFilePath);
        if (!cogFile.exists()) {
            throw new RuntimeException("변환된 COG 파일이 존재하지 않습니다: " + cogFilePath);
        }
    
        AmazonS3 s3Client = createS3Client();
        String targetKey = targetFolderPath + fileNameOnly.replace(".tiff", "-to-cog.tiff");
    
        uploadFileToS3(s3Client, targetBucketName, targetKey, cogFilePath);
    
        System.out.println("업로드 성공: " + targetKey);
    }
    

    @Override
    public SatelliteImageDTO getMetadata(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getMetadata'");
    }

    private AmazonS3 createS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    private File downloadFileFromS3(AmazonS3 s3Client, String bucketName, String fileKey) throws Exception {
        String baseDir = System.getProperty("java.io.tmpdir");
        File downloadedFile = new File(baseDir + File.separator + new File(fileKey).getName());
    
        S3Object s3Object = s3Client.getObject(bucketName, fileKey);
        try (InputStream inputStream = s3Object.getObjectContent();
             FileOutputStream fos = new FileOutputStream(downloadedFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        System.out.println("다운로드 완료: " + downloadedFile.getAbsolutePath());
        return downloadedFile;
    }
    

    private String convertToCogFormat(File inputFile) throws Exception {
        String inputPath = inputFile.getAbsolutePath();
        String outputPath = inputPath.replace(".tif", "-to-cog.tif").replace(".tiff", "-to-cog.tiff");
    
        ProcessBuilder processBuilder = new ProcessBuilder(
            "gdal_translate", "-of", "COG", inputPath, outputPath
        );
    
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[GDAL] " + line);
            }
        }
    
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("GDAL 변환 실패: exit code = " + exitCode);
        }
    
        System.out.println("COG 변환 완료: " + outputPath);
        return outputPath;
    }

    private void uploadFileToS3(AmazonS3 s3Client, String bucketName, String fileKey, String filePath) {
        File file = new File(filePath); 
        s3Client.putObject(bucketName, fileKey, file);     
        System.out.println("S3 업로드 완료!");
    }

    private void saveMetadataToDatabase(SatelliteImageDTO satelliteImageDTO, String outputFilePath) {
        System.out.println("메타데이터 저장: " + satelliteImageDTO.getName());
    }

    @Override
    public List<SatelliteImageDTO> getConvertedFilesOnly() {
        throw new UnsupportedOperationException("Unimplemented method 'getConvertedFilesOnly'");
    }
}
