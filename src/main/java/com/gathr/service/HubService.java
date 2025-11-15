package com.gathr.service;

import com.gathr.dto.HubDto;
import com.gathr.entity.Hub;
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
    
    private HubDto convertToDto(Hub hub) {
        return new HubDto(
            hub.getId(),
            hub.getName(),
            hub.getArea(),
            hub.getDescription()
        );
    }
}

