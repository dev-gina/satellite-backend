package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;
import com.app.satellite.model.SatelliteImage;
import com.app.satellite.repository.SatelliteImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
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
                    boolean isTiffFile = key.toLowerCase().endsWith(".tif") || key.toLowerCase().endsWith(".tiff");
                    if (endsWithExt == null && isTiffFile || (endsWithExt != null && key.toLowerCase().endsWith(endsWithExt))) {
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
    public void saveMetadata(SatelliteImageDTO dto) {
        SatelliteImage entity = new SatelliteImage();
        entity.setName(dto.getName());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setBandCount(dto.getBandCount());
        entity.setUserName(dto.getUserName());
        entity.setCogPath(dto.getCogPath());
    
        // 시퀀스 +1 증가
        int sequence = satelliteImageRepository.countByName(dto.getName()) + 1;
        entity.setSequence(sequence);
    
        satelliteImageRepository.save(entity);
        System.out.println("DB 저장 완료: " + dto.getName() + " / 시퀀스: " + sequence);
    }

    @Override
    public void uploadConvertedImageToS3(SatelliteImageDTO satelliteImageDTO, boolean isBatch) {
        long startTime = System.currentTimeMillis();
        String fullKey = satelliteImageDTO.getName();
        if (fullKey == null || fullKey.isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
    
        String fileNameOnly = new File(fullKey).getName();
        String baseName = fileNameOnly.replace(".tiff", "").replace(".tif", "");
        AmazonS3 s3Client = createS3Client();
        String userFolder = targetFolderPath + satelliteImageDTO.getUserName() + "/";
        String cogFileName = baseName + "-to-cog-" + satelliteImageDTO.getSequence() + ".tiff";
        String tempDir = System.getProperty("java.io.tmpdir");
        String cogFilePath = tempDir + cogFileName;
        String targetKey = userFolder + cogFileName;
    
        // 다건일 떄 버킷 고정시키기
        String bucketName = isBatch ? "dev1-apne2-pre-test-tester-bucket" : targetBucketName;
    
        try {
            File inputFile = downloadFileFromS3(s3Client, sourceBucketName, fullKey);
            convertToCogFormat(inputFile, cogFilePath);
    
            File cogFile = new File(cogFilePath);
            if (!cogFile.exists()) {
                throw new RuntimeException("변환된 COG 파일이 존재하지 않습니다: " + cogFilePath);
            }
    
            uploadFileToS3(s3Client, bucketName, targetKey, cogFilePath);
            System.out.println("총 처리 시간: " + (System.currentTimeMillis() - startTime) / 1000 + "초");
            System.out.println("업로드 성공: " + targetKey);
    
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("COG 변환 및 업로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public void uploadConvertedImageToS3(SatelliteImageDTO dto) {
        uploadConvertedImageToS3(dto, false);
    }
    

    @Override
    public void convertBatchImagesToCog(List<SatelliteImageDTO> list) {
        long startTime = System.currentTimeMillis();
        long timeLimit = 15 * 60 * 1000;

        for (SatelliteImageDTO dto : list) {
            if (System.currentTimeMillis() - startTime > timeLimit) {
                throw new RuntimeException("전체 변환 시간이 15분을 초과했습니다.");
            }
            uploadConvertedImageToS3(dto, true);
        }
    }

    private void convertToCogFormat(File inputFile, String outputPath) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "gdal_translate", "-of", "COG", inputFile.getAbsolutePath(), outputPath
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

    private void uploadFileToS3(AmazonS3 s3Client, String bucketName, String fileKey, String filePath) {
        File file = new File(filePath);
        s3Client.putObject(bucketName, fileKey, file);
        System.out.println("S3 업로드 완료");
    }

    private AmazonS3 createS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    @Override
    public SatelliteImageDTO getMetadata(Long id) {
        SatelliteImage entity = satelliteImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 ID의 메타데이터가 존재하지 않습니다: " + id));
    
        SatelliteImageDTO dto = new SatelliteImageDTO();
        dto.setName(entity.getName());
        dto.setWidth(entity.getWidth());
        dto.setHeight(entity.getHeight());
        dto.setBandCount(entity.getBandCount());
        dto.setUserName(entity.getUserName());
        dto.setSequence(entity.getSequence());
        dto.setCogPath(entity.getCogPath());
        dto.setRemoteUrl(null);
    
        return dto;
    }

    @Override
    public List<SatelliteImageDTO> searchMetadata(String name, Integer width, Integer bandCount) {
        List<SatelliteImage> entities = satelliteImageRepository.findByNameContainingIgnoreCase(name);

        return entities.stream()
            .map(entity -> {
                SatelliteImageDTO dto = new SatelliteImageDTO();
                dto.setName(entity.getName());
                dto.setWidth(entity.getWidth());
                dto.setHeight(entity.getHeight());
                dto.setBandCount(entity.getBandCount());
                dto.setUserName(entity.getUserName());
                dto.setSequence(entity.getSequence());
                dto.setCogPath(entity.getCogPath());
                dto.setRemoteUrl(null);
                return dto;
            })
            .toList();
    }

}
