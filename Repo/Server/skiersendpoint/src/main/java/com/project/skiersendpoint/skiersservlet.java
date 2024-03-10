package com.project.skiersendpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class skiersservlet {

    private final SkierRepository skierRepository;

    public skiersservlet(SkierRepository skierRepository) {
        this.skierRepository = skierRepository;
    }

    @PostMapping("/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skiersID}")
    public ResponseEntity<String> doPOST(
            @PathVariable String resortID,
            @PathVariable String seasonID,
            @PathVariable String dayID,
            @PathVariable String skiersID,
            @RequestBody SkierRequest skierRequest // Modified line
    ) {
        // Basic parameter validation
        if (!isValid(resortID, seasonID, dayID, skiersID)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid parameters supplied");
        }

        // Save skier data to MongoDB with additional fields
        Skier skier = new Skier();
        skier.setDayID(dayID);
        skier.setResortID(resortID);
        skier.setSeasonID(seasonID);
        skier.setSkiersID(skiersID);
        skier.setTime(skierRequest.getTime()); // Setting time
        skier.setLiftID(skierRequest.getLiftID()); // Setting liftID
        skierRepository.save(skier);

        return ResponseEntity.status(HttpStatus.CREATED).body("Skier data saved to MongoDB.");
    }

    // Basic parameter validation method
    private boolean isValid(String resortID, String seasonID, String dayID, String skiersID) {
        // Check if IDs are positive integers
        try {
            int resortIdInt = Integer.parseInt(resortID);
            int seasonIdInt = Integer.parseInt(seasonID);
            int dayIdInt = Integer.parseInt(dayID);
            int skiersIdInt = Integer.parseInt(skiersID);

            if (resortIdInt <= 0 || seasonIdInt <= 0 || dayIdInt <= 0 || skiersIdInt <= 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false; // If any ID is not a valid integer
        }

        return true;
    }
}
