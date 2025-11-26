package com.gathr.config;

import com.gathr.entity.Activity;
import com.gathr.entity.Hub;
import com.gathr.entity.User;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.HubRepository;
import com.gathr.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Configuration
@Profile("local")
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(HubRepository hubRepository, UserRepository userRepository,
            ActivityRepository activityRepository) {
        return args -> {
            if (hubRepository.count() > 0) {
                System.out.println("Database already seeded. Skipping...");
                return;
            }

            System.out.println("Seeding database...");

            Hub downtown = new Hub();
            downtown.setName("Downtown");
            downtown.setSlug("downtown");
            downtown.setCity("San Francisco");
            downtown.setArea("City Center");
            downtown.setLatitude(new BigDecimal("37.7749"));
            downtown.setLongitude(new BigDecimal("-122.4194"));
            hubRepository.save(downtown);

            Hub techPark = new Hub();
            techPark.setName("Tech Park");
            techPark.setSlug("tech-park");
            techPark.setCity("San Francisco");
            techPark.setArea("SOMA");
            techPark.setLatitude(new BigDecimal("37.7800"));
            techPark.setLongitude(new BigDecimal("-122.4000"));
            hubRepository.save(techPark);

            // 2. Create Users
            User alice = new User();
            alice.setName("Alice");
            alice.setPhone("1234567890");
            alice.setVerified(true);
            alice.setBio("Loves hiking and coffee.");
            alice.setInterests(new String[] { "OUTDOOR", "FOOD" });
            alice.setHomeHub(downtown);
            userRepository.save(alice);

            User bob = new User();
            bob.setName("Bob");
            bob.setPhone("0987654321");
            bob.setVerified(true);
            bob.setBio("Tech enthusiast and gamer.");
            bob.setInterests(new String[] { "GAMES", "LEARNING" });
            bob.setHomeHub(techPark);
            userRepository.save(bob);

            // 3. Create Activities
            Activity coffeeRun = new Activity();
            coffeeRun.setTitle("Morning Coffee Run");
            coffeeRun.setCategory(Activity.ActivityCategory.FOOD);
            coffeeRun.setStartTime(LocalDateTime.now().plusHours(1));
            coffeeRun.setEndTime(LocalDateTime.now().plusHours(2));
            coffeeRun.setHub(downtown);
            coffeeRun.setPlaceName("Blue Bottle Coffee");
            coffeeRun.setPlaceAddress("123 Market St");
            coffeeRun.setLatitude(37.7750);
            coffeeRun.setLongitude(-122.4195);
            coffeeRun.setCreatedBy(alice);
            coffeeRun.setMaxMembers(4);
            activityRepository.save(coffeeRun);

            Activity hiking = new Activity();
            hiking.setTitle("Weekend Hike");
            hiking.setCategory(Activity.ActivityCategory.OUTDOOR);
            hiking.setStartTime(LocalDateTime.now().plusDays(1).plusHours(10));
            hiking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(14));
            hiking.setHub(techPark); // Or null if custom location, but let's use hub for simplicity
            hiking.setPlaceName("Twin Peaks");
            hiking.setPlaceAddress("Twin Peaks Blvd");
            hiking.setLatitude(37.7544);
            hiking.setLongitude(-122.4477);
            hiking.setCreatedBy(bob);
            hiking.setMaxMembers(10);
            activityRepository.save(hiking);

            System.out.println("Database seeded successfully!");
        };
    }
}
