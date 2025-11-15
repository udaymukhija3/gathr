package com.gathr.service;

import com.gathr.dto.HubDto;
import com.gathr.entity.Hub;
import com.gathr.repository.HubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubServiceTest {

    @Mock
    private HubRepository hubRepository;

    @InjectMocks
    private HubService hubService;

    private Hub hub1;
    private Hub hub2;

    @BeforeEach
    void setUp() {
        hub1 = new Hub();
        hub1.setId(1L);
        hub1.setName("Cyberhub");
        hub1.setArea("Cyber City");
        hub1.setDescription("Popular hangout spot with many restaurants");

        hub2 = new Hub();
        hub2.setId(2L);
        hub2.setName("Galleria");
        hub2.setArea("DLF Phase 4");
        hub2.setDescription("Shopping mall with food court");
    }

    @Test
    void getAllHubs_ShouldReturnListOfHubDtos() {
        // Given
        List<Hub> hubs = Arrays.asList(hub1, hub2);
        when(hubRepository.findAll()).thenReturn(hubs);

        // When
        List<HubDto> result = hubService.getAllHubs();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Cyberhub");
        assertThat(result.get(0).getArea()).isEqualTo("Cyber City");
        assertThat(result.get(0).getDescription()).isEqualTo("Popular hangout spot with many restaurants");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Galleria");
        assertThat(result.get(1).getArea()).isEqualTo("DLF Phase 4");

        verify(hubRepository).findAll();
    }

    @Test
    void getAllHubs_WithNoHubs_ShouldReturnEmptyList() {
        // Given
        when(hubRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<HubDto> result = hubService.getAllHubs();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(hubRepository).findAll();
    }
}
