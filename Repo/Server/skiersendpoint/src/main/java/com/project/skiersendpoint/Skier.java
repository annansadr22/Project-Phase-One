package com.project.skiersendpoint;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "DSS")
public class Skier {

    private String resortID;
    private String seasonID;
    private String dayID;
    private String skiersID;
    private String time;
    private String liftID;
    public String getResortID() {
		return resortID;
	}
	public void setResortID(String resortID) {
		this.resortID = resortID;
	}
	public String getSeasonID() {
		return seasonID;
	}
	public void setSeasonID(String seasonID) {
		this.seasonID = seasonID;
	}
	public String getDayID() {
		return dayID;
	}
	public void setDayID(String dayID) {
		this.dayID = dayID;
	}
	public String getSkiersID() {
		return skiersID;
	}
	public void setSkiersID(String skiersID) {
		this.skiersID = skiersID;
	}
	public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLiftID() {
        return liftID;
    }

    public void setLiftID(String liftID) {
        this.liftID = liftID;
    }
	
}