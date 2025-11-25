package com.gathr.service;

import com.gathr.dto.HubDto;
import com.gathr.entity.Hub;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.HubRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HubService {

    private final HubRepository hubRepository;

    public HubService(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public List<HubDto> getAllHubs() {
        return hubRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public HubDto getHubById(Long hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub", hubId));
        return convertToDto(hub);
    }

    private HubDto convertToDto(Hub hub) {
        HubDto dto = new HubDto();
        dto.setId(hub.getId());
        dto.setName(hub.getName());
        dto.setArea(hub.getArea());
        dto.setDescription(hub.getDescription());
        dto.setLatitude(hub.getLatitude());
        dto.setLongitude(hub.getLongitude());
        dto.setIsPartner(hub.getIsPartner());
        dto.setPartnerTier(hub.getPartnerTier() != null ? hub.getPartnerTier().name() : null);
        return dto;
    }
}

