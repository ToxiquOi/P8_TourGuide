package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.AttractionProximityModel;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService implements DisposableBean {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);

	private final TaskExecutorService executorService;
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	@Autowired
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.executorService = new TaskExecutorService(1200, 3);
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	public VisitedLocation getUserLocation(User user) {
		try {
			return (user.getVisitedLocations().size() > 0) ?
				user.getLastVisitedLocation() :
				trackUserLocation(user).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return new ArrayList<>(internalUserMap.values());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards()
				.stream()
				.mapToInt(UserReward::getRewardPoints)
				.sum();

		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}
	
	public Future<VisitedLocation> trackUserLocation(User user) {
		 return executorService.submit(() -> {
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(visitedLocation);
			rewardsService.calculateRewards(user).get();
			return visitedLocation;
		});
	}

	public AttractionProximityModel getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> attractions = gpsUtil.getAttractions();
		attractions.sort((a, b) -> {
			double distA = rewardsService.getDistance(visitedLocation.location, a);
			double distB = rewardsService.getDistance(visitedLocation.location, b);
			logger.debug(String.format("getNearByAttractions distA:%s, distB:%s", distA, distB));
			return Double.compare(distA, distB);
		});
		AttractionProximityModel attractionProximityModel = new AttractionProximityModel();
		attractionProximityModel.setAttractions(attractions.stream()
				.limit(5)
				.map(att -> new AttractionProximityModel.AttractionRange(att, rewardsService.getDistance(att, visitedLocation.location)))
				.collect(Collectors.toList())
		);

		return attractionProximityModel;
	}
	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(tracker::stopTracking));
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

	@Override
	public void destroy() throws Exception {
		executorService.destroy();
	}
}
