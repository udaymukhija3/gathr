package com.gathr.controller;

import com.gathr.dto.HubDto;
import com.gathr.service.HubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hubs")
public class HubController {
    
    private final HubService hubService;
    
    public HubController(HubService hubService) {
        this.hubService = hubService;
    }
    
    @GetMapping
    public ResponseEntity<List<HubDto>> getAllHubs() {
        List<HubDto> hubs = hubService.getAllHubs();
        return ResponseEntity.ok(hubs);
    }
}

