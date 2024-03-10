package com.project.skiersendpoint;




import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SkierRepository extends MongoRepository<Skier, String> {
        @Query(value = "{ 'resortID': ?0 }")
        Skier findByMappingCode(String resortID);
        

}
