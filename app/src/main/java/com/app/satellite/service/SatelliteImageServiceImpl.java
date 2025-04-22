package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;
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

@Service
public class SatelliteImageServiceImpl implements SatelliteImageService {

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
    public void convertImageToCog(SatelliteImageDTO satelliteImageDTO) {
        try {
            AmazonS3 s3Client = createS3Client();
            String sourceFileKey = satelliteImageDTO.getName();

            // 1. 원본 버킷에서 파일 다운로드
            File downloadedFile = downloadFileFromS3(s3Client, sourceBucketName, sourceFileKey);

            // 2. COG 포맷으로 변환
            String outputFilePath = convertToCogFormat(downloadedFile);

            // 3. 변환된 파일을 결과 버킷에 업로드
            String outputFileName = satelliteImageDTO.getName().replace(".tiff", "-to-cog.tiff");
            uploadFileToS3(s3Client, targetBucketName, targetFolderPath + outputFileName, outputFilePath);

            // 4. 메타데이터 저장 (추후 DB 연결 예정)
            saveMetadataToDatabase(satelliteImageDTO, outputFilePath);

            // 변환된 파일 경로 DTO에 저장
            satelliteImageDTO.setCogPath(outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    System.out.println("▶ 전체 key: " + key);  
    
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


    private AmazonS3 createS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    private File downloadFileFromS3(AmazonS3 s3Client, String bucketName, String fileKey) throws Exception {
        S3Object s3Object = s3Client.getObject(bucketName, fileKey);
        File downloadedFile = new File("/tmp/" + new File(fileKey).getName());
        try (InputStream inputStream = s3Object.getObjectContent();
             FileOutputStream fos = new FileOutputStream(downloadedFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        return downloadedFile;
    }

    private void uploadFileToS3(AmazonS3 s3Client, String bucketName, String fileKey, String filePath) {
        File file = new File(filePath);
        s3Client.putObject(bucketName, fileKey, file);
    }

    private String convertToCogFormat(File inputFile) throws Exception {
        String outputFilePath = inputFile.getAbsolutePath().replace(".tiff", "-to-cog.tiff");

        ProcessBuilder processBuilder = new ProcessBuilder(
                "gdal_translate",
                "-of", "COG",
                inputFile.getAbsolutePath(),
                outputFilePath
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

        return outputFilePath;
    }

    private void saveMetadataToDatabase(SatelliteImageDTO satelliteImageDTO, String outputFilePath) {
        System.out.println("메타데이터 저장: " + satelliteImageDTO.getName());
    }

    @Override
    public void saveMetadata(SatelliteImageDTO satelliteImageDTO) {
        System.out.println("파일명: " + satelliteImageDTO.getName());
        System.out.println("COG 경로: " + satelliteImageDTO.getCogPath());
    }

    @Override
    public void convertBatchImagesToCog(List<SatelliteImageDTO> satelliteImageDTOList) {
        throw new UnsupportedOperationException("Unimplemented method 'convertBatchImagesToCog'");
    }

    @Override
    public SatelliteImageDTO getMetadata(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getMetadata'");
    }
    
}
