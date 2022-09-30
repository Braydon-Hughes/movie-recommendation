import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
/**
 * @author Braydon Hughes
 * @version 1.0
 * @since 2021-12-5
 */
public class Recommender {

	public static void main(String[] args) throws FileNotFoundException {
		Scanner scan = new Scanner(new File("ratings.dat"));		
		Map<Integer, HashMap<Integer, Integer>> userMovie = new HashMap<Integer, HashMap<Integer, Integer>>();
		Map<Integer, HashMap<Integer, Integer>> movieUser = new HashMap<Integer, HashMap<Integer, Integer>>();
		
		while(scan.hasNextLine()) //reads through ratings.dat and stores userID, movieID, and rating as two nested hashmaps. 
		{
			int userID = scan.nextInt();
			int movieID = scan.nextInt();
			int rating = scan.nextInt();
			if(scan.hasNextLine()) {
				scan.nextLine();
			}
			if(userMovie.containsKey(userID)) {  //adds data to userMovie, with userID as the key, and each key is paired to a hashmap of movieID/rating
				userMovie.get(userID).put(movieID, rating);
			}
			else {
				userMovie.put(userID, new HashMap<Integer, Integer>());
				userMovie.get(userID).put(movieID, rating);
			}
			if(movieUser.containsKey(movieID)) //adds data to movieUser, with movieID as key and each hashmap containing userID/rating
			{
				movieUser.get(movieID).put(userID, rating);
			}
			else {
				movieUser.put(movieID, new HashMap<Integer, Integer>());
				movieUser.get(movieID).put(userID, rating);
			}
		} 
		
		Scanner scan2 = new Scanner(new File("movies.dat"));
		HashMap<Integer, String> movies = new HashMap<Integer,String>();
		while(scan2.hasNextLine()) //reads movies.dat file and stores a hashmap with movieID as the key and movie name as the value
		{
			String str = scan2.nextLine();
			String[] splitStr = str.split("\\|");
			int id = Integer.parseInt(splitStr[0]);
			String name = splitStr[1];
			movies.put(id, name);
		}
		ArrayList<Double>[] similarities = computeSimilarity(movieUser); //calls helper method for similarity
		predictRating(similarities, userMovie, movies); //calls helper method that predicts user ratings
	}
	
	public static ArrayList<Double>[] computeSimilarity(Map<Integer, HashMap<Integer, Integer>> movieUser) { //computes similarity table (an array of arraylists) which compares all movies to each other
		
		HashMap<Integer, Integer> moviei;
		HashMap<Integer, Integer> moviej;
		@SuppressWarnings("unchecked")
		ArrayList<Double>[] similarityTable = new ArrayList[movieUser.size()];
		
		for(int i = 0; i < similarityTable.length; i++) //iterates through all movies
		{
			ArrayList<Double> simList = new ArrayList<Double>();
			for(int j = 1; j <= i; j++) //iterates i times before i increments. This way, only half of the (symmetric) table is computed.
			{
				int product = 0;
				double squareL,	squareR;
				double sumL = 0;
				double sumR = 0;
				double denominator, similarity;
				double numerator = 0;
				moviei = movieUser.get(i+1);
				moviej = movieUser.get(j);
				for(int k : moviei.keySet()) //iterates through all users that have rated movie i, computes numerator of similarity formula
				{
					if(moviej.containsKey(k)) 
					{
						product = moviei.get(k) * moviej.get(k);
						numerator += product;
					}
					squareL = Math.pow(moviei.get(k), 2); //computes left side of formula's denominator
					sumL += squareL;
				}
				for(int p : moviej.values()) //computes right side of formula's denominator, requires separate loop for other user's ratings set
				{
					squareR = Math.pow(p, 2);
					sumR += squareR;
				}
				if(numerator > 0) //prevents possible errors from negative numbers and unnecessary computations, also finishes computing the similarity between two movies
				{
					sumL = Math.sqrt(sumL);
					sumR = Math.sqrt(sumR);
			
					denominator = sumL * sumR;
			
					similarity = numerator / denominator;
					simList.add(similarity);
				}
				else {
					simList.add(0.0);
				}
			}
			similarityTable[i] = simList; //adds all similarities computed for movies 1 through i to movie i+1 (adds nothing when i=0);
		}
		return similarityTable;
	}
	
	
	public static void predictRating(ArrayList<Double>[] similarity, Map<Integer, HashMap<Integer, Integer>> userMovie, HashMap<Integer, String> movies) //predicts ratings for movies that are not rated for each user, finds top 5 predictions and prints them out
	{
		double product = 0;
		double prediction = 0;
		HashMap<Integer, Integer> mapi;
		for(int i = 0; i < userMovie.size(); i++) //iterates through each user
		{
			int sum = 0;
			mapi = userMovie.get(i+1);
			ArrayList<Rating> topFive = new ArrayList<Rating>(); //makes an arraylist of Ratings objects
			for(int j = 0; j < similarity.length; j++) //iterates through all movies
			{
				double numerator = 0;
				double denominator = 0;
				if(!mapi.containsKey(j+1)) //runs if user i has not rated the movie
				{	
					for(Integer k : mapi.keySet()) //iterates through all movies user i has seen
					{
						if(j >= k) { //ensures correct indexes for similarity table are accessed, calculates numerator and denominator simultaneously
							ArrayList<Double> sim1 = similarity[j];
							double sim2 = sim1.get(k-1);
							product = mapi.get(k) * sim2;
							denominator += similarity[j].get(k-1);
						}
						else {
							ArrayList<Double> sim1 = similarity[k-1];
							double sim2 = sim1.get(j);
							product = mapi.get(k) * sim2;
							denominator += similarity[k-1].get(j);
						}
						numerator += product;
					}
					prediction = numerator / denominator;
					sum++;
					if(sum <= 5) //adds predicted rating to top 5 if the list is not "full"
					{
						topFive.add(new Rating(j+1, prediction));				
					}
					else //if the list already has 5 recommendations
					{
						double currentMin = topFive.get(0).getRating();
						int minIndex = 0;
						for(int a = 1; a < 5; a++) //finds smallest predicted rating in current top 5 
						{
							if(currentMin >= topFive.get(a).getRating())
							{
								currentMin = topFive.get(a).getRating();
								minIndex = a;
							}
						}
						if(prediction > currentMin) //replaces smallest predicted rating with newly computed rating if new rating is larger
						{
							topFive.remove(minIndex);
							topFive.add(new Rating(j+1, prediction));
						}
					}
				}
			}
			System.out.println();
			System.out.println("User ID: " + (i+1) + ", Top 5 Recommendations: ");
			int length = topFive.size();
			for(int a = 0; a < length; a++) { //iterates through all recommendations
				double currentMax = topFive.get(0).getRating();
				int maxIndex = 0;
				for(int b = 1; b < topFive.size(); b++) //finds the largest rating currently in the arraylist
				{
					if(currentMax < topFive.get(b).getRating())
					{
						currentMax = topFive.get(b).getRating();
						maxIndex = b;
					}
				}
				String movieName = movies.get(topFive.get(maxIndex).getID()); //prints out the rating and movie name
				System.out.println(movieName + ": " + topFive.get(maxIndex).getRating() + " ");
				topFive.remove(maxIndex); //removes from list so next largest rating can be found
			}
		}	
	}
	
	private static class Rating { //class stores the movieID and rating together as an object so that ID can still be associated with rating when added to top 5 list
		int movieID;
		double predictedRating;
		
		private Rating(int movieID, double predictedRating)
		{
			this.movieID = movieID;
			this.predictedRating = predictedRating;
		}
		
		private int getID()
		{
			return movieID;
		}
		
		private double getRating()
		{
			return predictedRating;
		}
		
	}
	
	
	
}
