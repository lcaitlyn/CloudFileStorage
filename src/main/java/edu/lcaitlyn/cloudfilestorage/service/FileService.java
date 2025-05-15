package edu.lcaitlyn.cloudfilestorage.service;

import edu.lcaitlyn.cloudfilestorage.DTO.response.DownloadResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.MoveResourceRequestDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.response.ResourceResponseDTO;
import edu.lcaitlyn.cloudfilestorage.DTO.request.ResourceRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FileService {

    List<ResourceResponseDTO> uploadFile(ResourceRequestDTO request);

    ResourceResponseDTO getFile(ResourceRequestDTO request);

    List<ResourceResponseDTO> getDirectory(ResourceRequestDTO request);

    ResourceResponseDTO createDirectory(ResourceRequestDTO request);

    void deleteResourceOrDirectory(ResourceRequestDTO request);

    List<ResourceResponseDTO> findResource(ResourceRequestDTO request);

    ResourceResponseDTO moveResource(MoveResourceRequestDTO request);

    DownloadResourceResponseDTO download(ResourceRequestDTO request);
}
