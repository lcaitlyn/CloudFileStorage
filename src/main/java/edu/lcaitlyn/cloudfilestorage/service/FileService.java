package edu.lcaitlyn.cloudfilestorage.service;

import edu.lcaitlyn.cloudfilestorage.DTO.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.ResourceRequestDTO;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@Service
public interface FileService {

    List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request) throws IOException;

    ResourceResponseDTO getFile(ResourceRequestDTO request) throws FileNotFoundException;

    ResourceResponseDTO download(ResourceRequestDTO request);

    List<ResourceResponseDTO> getDirectory(ResourceRequestDTO request);

    ResourceResponseDTO createDirectory(ResourceRequestDTO request);

    void deleteResourceOrDirectory(ResourceRequestDTO request) throws NoSuchKeyException;
}
