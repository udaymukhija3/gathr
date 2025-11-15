package com.gathr.config;

import com.gathr.entity.Activity;
import com.gathr.entity.Hub;
import com.gathr.entity.User;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.HubRepository;
import com.gathr.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {
    
    private final HubRepository hubRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    
    public DataSeeder(HubRepository hubRepository, 
                     ActivityRepository activityRepository,
                     UserRepository userRepository) {
        this.hubRepository = hubRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    public void run(String... args) {
        // Check if data already exists
        if (hubRepository.count() > 0) {
            return;
        }
        
        // Create Hubs
        Hub cyberhub = new Hub();
        cyberhub.setName("Cyberhub");
        cyberhub.setArea("Cyber City");
        cyberhub.setDescription("A bustling hub with restaurants, cafes, and entertainment venues");
        cyberhub = hubRepository.save(cyberhub);
        
        Hub galleria = new Hub();
        galleria.setName("Galleria");
        galleria.setArea("DLF Galleria");
        galleria.setDescription("Shopping and dining destination in the heart of Gurgaon");
        galleria = hubRepository.save(galleria);
        
        Hub avenue32 = new Hub();
        avenue32.setName("32nd Avenue");
        avenue32.setArea("Sector 32");
        avenue32.setDescription("Popular food and nightlife street");
        avenue32 = hubRepository.save(avenue32);
        
        // Create a test user for activities
        User testUser = new User();
        testUser.setName("Test User");
        testUser.setPhone("1234567890");
        testUser.setVerified(true);
        testUser = userRepository.save(testUser);
        
        // Create Activities
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        
        // Activity 1: Cyberhub - Sports
        Activity activity1 = new Activity();
        activity1.setTitle("Badminton at Cyberhub Sports Complex");
        activity1.setHub(cyberhub);
        activity1.setCategory(Activity.ActivityCategory.SPORTS);
        activity1.setStartTime(today.plusHours(18));
        activity1.setEndTime(today.plusHours(20));
        activity1.setCreatedBy(testUser);
        activityRepository.save(activity1);
        
        // Activity 2: Cyberhub - Food
        Activity activity2 = new Activity();
        activity2.setTitle("Food Crawl - Cyberhub Restaurants");
        activity2.setHub(cyberhub);
        activity2.setCategory(Activity.ActivityCategory.FOOD);
        activity2.setStartTime(today.plusHours(19));
        activity2.setEndTime(today.plusHours(22));
        activity2.setCreatedBy(testUser);
        activityRepository.save(activity2);
        
        // Activity 3: Galleria - Art
        Activity activity3 = new Activity();
        activity3.setTitle("Art Exhibition Walk - Galleria");
        activity3.setHub(galleria);
        activity3.setCategory(Activity.ActivityCategory.ART);
        activity3.setStartTime(today.plusHours(16));
        activity3.setEndTime(today.plusHours(18));
        activity3.setCreatedBy(testUser);
        activityRepository.save(activity3);
        
        // Activity 4: 32nd Avenue - Music
        Activity activity4 = new Activity();
        activity4.setTitle("Live Music Night - 32nd Avenue");
        activity4.setHub(avenue32);
        activity4.setCategory(Activity.ActivityCategory.MUSIC);
        activity4.setStartTime(today.plusHours(20));
        activity4.setEndTime(today.plusHours(23));
        activity4.setCreatedBy(testUser);
        activityRepository.save(activity4);
        
        // Activity 5: Galleria - Food
        Activity activity5 = new Activity();
        activity5.setTitle("Coffee & Conversations - Galleria Cafe");
        activity5.setHub(galleria);
        activity5.setCategory(Activity.ActivityCategory.FOOD);
        activity5.setStartTime(today.plusHours(17));
        activity5.setEndTime(today.plusHours(19));
        activity5.setCreatedBy(testUser);
        activityRepository.save(activity5);
        
        System.out.println("Seed data created successfully!");
    }
}

