package tourGuide.model;

import gpsUtil.location.Attraction;

import java.util.ArrayList;
import java.util.List;

public class AttractionProximityModel {
    private UserReduce user;
    private List<AttractionRange> attractions = new ArrayList<>();

    public List<AttractionRange> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<AttractionRange> attractions) {
        this.attractions = attractions;
    }

    public UserReduce getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = new UserReduce(user);
    }

    public static class AttractionRange {
        private final Attraction attraction;
        private final double distance;

        public AttractionRange(Attraction attraction, double distance) {
            this.attraction = attraction;
            this.distance = distance;
        }

        public Attraction getAttraction() {
            return attraction;
        }

        public double getDistance() {
            return distance;
        }
    }
}
