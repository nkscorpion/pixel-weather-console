package com.ledpixelart.console;

import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.RgbLedMatrix;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOConnectionManager.Thread;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.pc.IOIOConsoleApp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.onebeartoe.pixel.hardware.Pixel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//import com.ledpixelart.pc.PixelApp;

/******** Pseudo code for this app ***************
Get the current matrix type from preferences or the command line
Get the gif filename that the user has selected
A. Check if the GIf has already been decoded
B. If it was already decoded, does the current matrix resolution match the decoded resolution
If A or B is no, then we need to decode the GIF
Now we can animate the GIF in a loop
We will make calls to the PIXEL class
*/

public class PIXELConsole extends IOIOConsoleApp {
@SuppressWarnings("deprecation")
//public class PIXELConsole  {
	private boolean ledOn_ = false;

	private static IOIO ioiO; 
	 
	private static RgbLedMatrix.Matrix KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32;
	 
	private static int weatherCode;
	 
	private static String weatherCondition;
     
	public static final Pixel pixel = new Pixel(KIND);
	
	private static int selectedFileResolution = 2048; //hard coded this for now
	 
	public static String pixelFirmware = "Not Found";
	 
	public static String pixelHardwareID = "";
	    
	private static VersionType v;
	 
	private static int i;
	
	private static int z = 0;
	    
    private static int numFrames = 0;
    
    private static String animation_name;
    
    private volatile static Timer timer;
    
    private static ActionListener AnimateTimer;
    
    private static String selectedFileName;
    
  	private static String decodedDirPath;
  	
  	private static byte[] BitmapBytes;
  	
  	private static short[] frame_;
  	
  	private static String framestring;
  	
  	private static float fps = 0;

	private static Command command_;
	
	private static String zip_ = "";
	
	private static String gifFileName_ = "";
	
	private static String woeid_ = "";
	
	private static int zipInt_;
	
	private static int woeidInt_;
	
	private static boolean zipMode;
	
	private static boolean reportTomorrowWeather = false;
	
	private static boolean validCommandLine = false;
	
	private static boolean writeMode = false;
	
	private static boolean gifModeInternal = false;
	
	private static boolean gifModeExternal = false;
	
	private static boolean weatherMode = false;
	
	private static HttpGet getRequest;
	
	private static HttpResponse httpResponse;
	
	private static HttpEntity entity;
	
	private static NodeList nodi;
	
	private static InputStream inputXml = null;
	
    private static int selectedFileTotalFrames;
    
    private static int selectedFileDelay;
    
    private static float GIFfps;
    
    private static int GIFnumFrames;
    
    private static int GIFselectedFileDelay;
    
    private static int GIFresolution;
    
    private static String currentDir;

    private static int matrix_model;
    
    private static int frame_length;
    
    private static int currentResolution;
    
    
	private static enum Command {
		VERSIONS, FINGERPRINT, WRITE
	}
  	
  	private static void printUsage() {
		System.err.println("PIXEL: Console Version");
		System.err.println();
		System.err.println("Usage:");
		System.err.println("pixel <options>");
		System.err.println();
		System.err.println("Valid options are:");
		System.err.println("********* Weather  **********");
		System.err
				.println("--zip=your_zip_code Non-US users should use woeid");
		System.err
				.println("--woeid=your_woeid_code A numeric number that Yahoo uses to designate your location");
		System.err
				.println("--forecast Displays tomorrow's weather conditions, defaults to current weather conditions if not specified");
		System.err.println("********* OR  Pre-loaded GIFs **********");
		System.err
		.println("--list  Displays names of all the pre-loaded GIFs");
		System.err
		.println("--gifp=your_filename.gif  Send this gif to PIXEL");
		System.err
		.println("--write  Puts PIXEL into write mode, default is streaming mode");
		System.err.println("********* OR  User Supplied / External GIFs **********");
		System.err
		.println("--gif=your_filename.gif  Send this gif to PIXEL");
		System.err
		.println("--write  Puts PIXEL into write mode, default is streaming mode");
		//need an option to display all possible gif names
		
	}

	// Boilerplate main(). Copy-paste this code into any IOIOapplication.
		public static void main(String[] args) throws Exception {
			
			System.out.println("Working Directory = " +
		              System.getProperty("user.dir"));
			
			currentDir = System.getProperty("user.dir");
			
			if (args.length == 0) {
				printUsage();
				System.exit(1);
			}
			
			
			
			try {
				parseArgs(args);
			} catch (BadArgumentsException e) {
				System.err.println("Invalid command line.");
				System.err.println(e.getMessage());
				printUsage();
				System.exit(2);
			}
			
			new PIXELConsole().go(args);
		}
		
		private static void parseArgs(String[] args) throws BadArgumentsException {
			int nonOptionArgNum = 0;
			for (String arg : args) {
				if (arg.startsWith("--")) {
					parseOption(arg);
				} else {
					if (nonOptionArgNum == 0) {
						parseCommand(arg);
					}
				}
			}
		}

		private static void parseCommand(String arg) throws BadArgumentsException {
			try {
				command_ = Command.valueOf(arg.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BadArgumentsException("Unrecognized command: " + arg);
			}
		}

		private static void parseOption(String arg) throws BadArgumentsException {
			//it can only be zip or woeid
			
			if (arg.startsWith("--forecast")) {
				reportTomorrowWeather = true;
				System.out.println("Displaying the current weather conditions, use --forecast if you want tomorrow's forecast.\n");
			}
			
			if (arg.startsWith("--gifp=")) {
				gifFileName_ = arg.substring(7);
				System.out.println("gif file name: " + gifFileName_);
				gifModeInternal = true;
				validCommandLine = true;
				z++;
			}	
			
			if (arg.startsWith("--gif=")) {
				gifFileName_ = arg.substring(6);
				System.out.println("gif file name: " + gifFileName_);
				gifModeExternal = true;
				validCommandLine = true;
				z++;
			}	
			
			if (arg.startsWith("--image=")) {
			//	gifFileName_ = arg.substring(6);
				System.out.println("gif file name: " + gifFileName_);
				gifModeExternal = true;
				validCommandLine = true;
				z++;
			}	
			
			if (arg.startsWith("--write")) {
				writeMode = true;
				System.out.println("PIXEL is in write mode\n");
			}
			
			if (arg.startsWith("--zip=")) {
				zip_ = arg.substring(6);
				System.out.println("zip code: " + zip_);
				validCommandLine = true;
				zipMode = true;
				weatherMode = true;
				z++;
			} else if (arg.startsWith("--woeid")) {
				woeid_ = arg.substring(8);
				System.out.println("woeid: " + woeid_);
				validCommandLine = true;
				zipMode = false;
				weatherMode = true;
				z++;
			}
			
			if (validCommandLine == false) {
				throw new BadArgumentsException("Unexpected option: " + arg);
			}
		}
		
		private static class BadArgumentsException extends Exception {
			private static final long serialVersionUID = -5730905669013719779L;

			public BadArgumentsException(String message) {
				super(message);
			}
		}
	

	protected void run(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		boolean abort = false;
		String line;
		while (!abort && (line = reader.readLine()) != null) {
			if (line.equals("t")) {
				//ledOn_ = !ledOn_;
			} else if (line.equals("n")) {
				//ledOn_ = true;
			} else if (line.equals("f")) {
				//ledOn_ = false;
			} else if (line.equals("q")) {
				abort = true;
			} else {
				System.out
						.println("Unknown input. t=toggle, n=on, f=off, q=quit.");
			}
		}
	
	}
	
	private static void getWeather() {
	
		    inputXml = null;
		    try
		    {

	           if (zipMode == true) {
	 	    	//	 zipInt_ = Integer.parseInt(zip_);
	 	    		 //System.out.println("zip code mode is: " + zipInt_); 
	 	    		 inputXml  = new URL("http://weather.yahooapis.com/forecastrss?p=" + zip_ + "&u=f").openConnection().getInputStream();
	 	    		// System.out.println("http://weather.yahooapis.com/forecastrss?p=" + zip_ + "&u=f"); 
	 		     }
	 		      else {
	 		    	  woeidInt_ = Integer.parseInt(woeid_);
	 		    	  inputXml  = new URL("http://weather.yahooapis.com/forecastrss?p=" + woeid_ + "&u=f").openConnection().getInputStream();
	 		      }

		       DocumentBuilderFactory factory = DocumentBuilderFactory.
		                                        newInstance();
		       DocumentBuilder builder = factory.newDocumentBuilder();
		       Document doc = builder.parse(inputXml);
		       
		       if (reportTomorrowWeather == true) {
		    	   nodi = doc.getElementsByTagName("yweather:forecast");
		       }
		       else {
		    	  // System.out.println("report now weather"); 
		    	   nodi = doc.getElementsByTagName("yweather:condition");    //yweather:condition is now
		       }

		       if (nodi.getLength() > 0)
		       {
		          Element nodo = (Element)nodi.item(0);

		          String parsedWeatherString = nodo.getAttribute("code");
		        //  System.out.println("code is: " + parsedWeatherString);
		        
		          weatherCode = Integer.parseInt(parsedWeatherString);
		          System.out.println("Yahoo Weather Code: " + weatherCode + "");

		        }
		    }
		    catch (Exception ex)
		    {
		       System.out.println(ex.getMessage());
		    }
		    finally
		    {
		       try
		       {
		          if (inputXml != null)
		          inputXml.close();
		       }
		       catch (IOException ex)
		       {
		          System.out.println(ex.getMessage());
		       }
		    }
		    
		    switch (weatherCode)  //gets the weather condition from yahoo
			{
				case 0: //tornado							
					weatherCondition = "rain";				
					break;
				case 1: //tropical storm					
					weatherCondition = "rain";	
					break;
				case 2: //hurricane					
					weatherCondition = "rain";	
					break;
				case 3: //severe thunderstorms					
					weatherCondition = "rain";	
					break;
				case 4: //thunderstorms					
					weatherCondition = "rain";	
					break;
				case 5: //mixed rain and snow
					weatherCondition = "rain";	
					break;
				case 6: //mised rain and sleet
					weatherCondition = "rain";	
					break;
				case 7: //mised snow and sleet
					weatherCondition = "snow";	
					break;
				case 8: //freezing drizzle
					weatherCondition = "rain";	
					break;
				case 9: //drizzle
					weatherCondition = "rain";	
					break;
				case 10: //freezing rain
					weatherCondition = "rain";	
					break;
				case 11: //showers
					weatherCondition = "rain";	
					break;	
				case 12: //showers
					weatherCondition = "rain";	
					break;	
				case 13: //snow flurries
					weatherCondition = "snow";	
					break;	
				case 14: //light snow showers
					weatherCondition = "snow";	
					break;	
				case 15: //blowing snow
					weatherCondition = "snow";	
					break;
				case 16: //snow
					weatherCondition = "snow";	
					break;
				case 17: //hail
					weatherCondition = "snow";	
					break;
				case 18: //sleet
					weatherCondition = "snow";	
					break;
				case 19: //dust
					weatherCondition = "rain";	
					break;
				case 20: //foggy
					weatherCondition = "rain";	
					break;
				case 21: //haze
					weatherCondition = "rain";	
					break;
				case 22: //smoky
					weatherCondition = "rain";	
					break;
				case 23: //blustery
					weatherCondition = "rain";	
					break;
				case 24: //windy
					weatherCondition = "cloudy";	
					break;
				case 25: //cold
					weatherCondition = "cloudy";	
					break;
				case 26: //cloudy
					weatherCondition = "cloudy";	
					break;
				case 27: //mostly cloudy (day)
					weatherCondition = "cloudy";	
					break;
				case 28: //mostly cloudy (night)
					weatherCondition = "cloudy";	
					break;
				case 29: //partly cloudy (night)
					weatherCondition = "sunny";	
					break;
				case 30: //partly cloudy (day)
					weatherCondition = "sunny";	
					break;
				case 31: //clear (night)
					weatherCondition = "sunny";	
					break;
				case 32: //sunny
					weatherCondition = "sunny";	
					break;
				case 33: //fair (night)
					weatherCondition = "sunny";	
					break;
				case 34: //fair (day)
					weatherCondition = "sunny";	
					break;
				case 35: //mixed rain and hail
					weatherCondition = "rain";	
					break;
				case 36: //hot
					weatherCondition = "sunny";	
					break;
				case 37: //isoldated thunderstorms
					weatherCondition = "rain";	
					break;
				case 38: //scattered thunderstorms
					weatherCondition = "rain";	
					break;
				case 39: //scattered thunderstorms
					weatherCondition = "rain";	
					break;
				case 40: //scattered showers
					weatherCondition = "rain";	
					break;
				case 41: //heavy snow
					weatherCondition = "snow";	
					break;	
				case 42: //scattered snow showers
					weatherCondition = "snow";	
					break;	
				case 43: //heavy snow
					weatherCondition = "snow";	
					break;	
				case 44: //partly cloudy
					weatherCondition = "cloudy";	
					break;
				case 45: //thundershowers
					weatherCondition = "rain";	
					break;
				case 46: //snow showers
					weatherCondition = "rain";	
					break;
				case 47: //isoldated thundershowers
					weatherCondition = "rain";	
					break;	
				case 3200: //not available
					weatherCondition = "cloudy";	
					break;						
				default:
					//showToast("Could not obtain weather, please check Internet connection");
			}
		    System.out.println("Weather Condition = " + weatherCondition);
		}
	
	 private static void weatherGIF() //not used
	    {
		 
		selectedFileName = weatherCondition;
		//selectedFileName = gifFileName_;
		/*
		String decodedDirPath = "animations/decoded";
		String imagePath = decodedanDirPath; //animations/decoded/rainx.rgb565
		String path = decodedDirPath + "/" + selectedFileName + ".txt"; //animations/decoded/rain.txt , this file tells us fps
		InputStream decodedFile = PIXELConsole.class.getClassLoader().getResourceAsStream(path); //how to access this file from the jar file
		//note can't use file operator here as you can't reference files from a jar file
*/		
		 //here we will send the selectedfilename to the pixel class, the pixel class will then look for the corresponding filename.txt meta-data file and return back the meta data
		
		if (pixel.GIFNeedsDecoding(currentDir, selectedFileName, currentResolution) == true) {    //resolution can be 16, 32, 64, 128 (String CurrentDir, String GIFName, int currentResolution)
			
			//decodeGIF
		}
		
		GIFfps = pixel.getDecodedfps(currentDir, selectedFileName); //get the fps //to do fix this later becaause we are getting from internal path
	    GIFnumFrames = pixel.getDecodednumFrames(currentDir, selectedFileName);
	    GIFselectedFileDelay = pixel.getDecodedframeDelay(currentDir, selectedFileName);
	    GIFresolution = pixel.getDecodedresolution(currentDir, selectedFileName);
		
		//****** Now let's setup the animation ******
		    
		   // = ;
		    i = 0;
		   // numFrames = selectedFileTotalFrames;
	            
	            stopExistingTimer(); //is this needed, probably not
	    				   
	    				   ActionListener AnimateTimer = new ActionListener() {

	    	                    public void actionPerformed(ActionEvent actionEvent) {
	    	                    
	    	               		 	i++;
	    	               			
	    	               			if (i >= numFrames - 1) 
	    	               			{
	    	               			    i = 0;
	    	               			}
	    	               		 
	    	               		String framestring = "animations/decoded/" + selectedFileName;
	    	               		
	    	               		System.out.println("framestring: " + framestring);

	    	               		//pixel.loadRGB565(framestring);
	    	               		// pixel.SendPixelDecodedFrame(framestring, i, GIFnumFrames, GIFresolution);
	    	               		 pixel.SendPixelDecodedFrame(currentDir, selectedFileName, i, GIFnumFrames, GIFresolution);
	    	               	
	    	      
	                    }
	                };
	    				   
	    				   
	    				   timer = new Timer(GIFselectedFileDelay, AnimateTimer); //the timer calls this function per the interval of fps
	    				   timer.start();
	    				   System.out.println("file delay: " + selectedFileDelay);
	    }    
	
	
	 private static void streamGIF(boolean writeMode) 
	    {
		
		//selectedFileName = gifFileName_;
		//gifName = FilenameUtils.removeExtension(gifName); //with no extension, ex. tree instead of tree.gif
		/*
		String decodedDirPath = "animations/decoded";
		String imagePath = decodedanDirPath; //animations/decoded/rainx.rgb565
		String path = decodedDirPath + "/" + selectedFileName + ".txt"; //animations/decoded/rain.txt , this file tells us fps
		InputStream decodedFile = PIXELConsole.class.getClassLoader().getResourceAsStream(path); //how to access this file from the jar file
		//note can't use file operator here as you can't reference files from a jar file
*/		
		 //here we will send the selectedfilename to the pixel class, the pixel class will then look for the corresponding filename.txt meta-data file and return back the meta data
		
		if (pixel.GIFNeedsDecoding(currentDir, gifFileName_, currentResolution) == true) {    //resolution can be 16, 32, 64, 128 (String CurrentDir, String GIFName, int currentResolution)
			System.out.println("Decoding " + gifFileName_);
			pixel.decodeGIF(currentDir, gifFileName_, currentResolution,KIND.width,KIND.height);
			
		}
		else {
			System.out.println(gifFileName_ + " is already decoded, skipping decoding step");
		}
		
		GIFfps = pixel.getDecodedfps(currentDir, gifFileName_); //get the fps
	    GIFnumFrames = pixel.getDecodednumFrames(currentDir, gifFileName_);
	    GIFselectedFileDelay = pixel.getDecodedframeDelay(currentDir, gifFileName_);  // to do fix
	    GIFresolution = pixel.getDecodedresolution(currentDir, gifFileName_);
	    
	    System.out.println(gifFileName_ + " contains " + GIFnumFrames + " total frames, a " + GIFselectedFileDelay + "ms frame delay or " + GIFfps + " frames per second and a resolution of " + GIFresolution);
		
		//****** Now let's setup the animation ******
		    
		   // animation_name = selectedFileName;
		    i = 0;
		   // numFrames = selectedFileTotalFrames;
		    
		           
	            stopExistingTimer(); //is this needed, probably not
	    			
	    			if (pixelHardwareID.substring(0,4).equals("PIXL") && writeMode == true) {  //in write mode, we don't need a timer because we don't need a delay in between frames, we will first put PIXEL in write mode and then send all frames at once
	    					pixel.interactiveMode();
	    					//send loading image
	    					pixel.writeMode(fps); //need to tell PIXEL the frames per second to use, how fast to play the animations
	    					
	    					  int y;
	    				    	 
	    				   	  //for (y=0;y<numFrames-1;y++) { //let's loop through and send frame to PIXEL with no delay
	    				      for (y=0;y<numFrames;y++) { //Al removed the -1, make sure to test that!!!!!
	    				 		
	    				 			//framestring = "animations/decoded/" + animation_name + ".rgb565";
	    				 			//System.out.println("Writing to PIXEL: Frame " + y + "of " + GIFnumFrames + " Total Frames");
	
	    				 		    pixel.SendPixelDecodedFrame(currentDir, gifFileName_, i, GIFnumFrames, GIFresolution);
	    				   	  } //end for loop
	    					
	    					
	    					pixel.playLocalMode(); //now tell PIXEL to play locally
	    			}
	    			else {   //we're not writing so let's just stream
	            
	            stopExistingTimer(); //is this needed, probably no
	    				   
	    				   ActionListener AnimateTimer = new ActionListener() {

	    	                    public void actionPerformed(ActionEvent actionEvent) {
	    	                    
	    	               		 	i++;
	    	               			
	    	               			if (i >= GIFnumFrames - 1) 
	    	               			{
	    	               			    i = 0;
	    	               			}
	    	               		 
	    	               		//String framestring = "animations/decoded/" + gifFileName_;
	    	               		
	    	               		//System.out.println("framestring: " + framestring);

	    	               		//pixel.loadRGB565(framestring);
	    	               		// pixel.SendPixelDecodedFrame(framestring, i, GIFnumFrames, GIFresolution);
	    	               		 pixel.SendPixelDecodedFrame(currentDir, gifFileName_, i, GIFnumFrames, GIFresolution);
 	                    }
 	                };
	    				   
	    				   
	    				   timer = new Timer(GIFselectedFileDelay, AnimateTimer); //the timer calls this function per the interval of fps
	    				   timer.start();
	    			}    
		
	      }
	
	
	 private static void runWeatherAnimations() //old one, no longer we need this one
	    {
		 
	    selectedFileName = weatherCondition;
		 
	    System.err.println("file name: " + selectedFileName);
	    
		String decodedDirPath = "animations/decoded";
		String imagePath = decodedDirPath; //animations/decoded/rain.rgb565
		String path = decodedDirPath + "/" + selectedFileName + ".txt"; //animations/decoded/rain.txt , this file tells us fps
		
		System.err.println("file name path: " + path);
		
		
		InputStream decodedFile = PIXELConsole.class.getClassLoader().getResourceAsStream(path); //how to access this file from the jar file
		//note can't use file operator here as you can't reference files from a jar file

		if (decodedFile != null) 
		{
		    // ok good, now let's read it, we need to get the total numbers of frames and the frame speed

		    String line = "";

		    try 
		    {
				InputStreamReader streamReader = new InputStreamReader(decodedFile);
				BufferedReader br = new BufferedReader(streamReader);
				line = br.readLine();
		    } 
		    catch (IOException e) 
		    {
			    //You'll need to add proper error handling here
		    }

		    String fileAttribs = line.toString();  //now convert to a string	 
		    String fdelim = "[,]"; //now parse this string considering the comma split  ie, 32,60
		    String[] fileAttribs2 = fileAttribs.split(fdelim);
		    int selectedFileTotalFrames = Integer.parseInt(fileAttribs2[0].trim());
		    int selectedFileDelay = Integer.parseInt(fileAttribs2[1].trim());	
		    
		    if (selectedFileDelay != 0) {  //then we're doing the FPS override which the user selected from settings
	    		fps = 1000.f / selectedFileDelay;
			} else { 
	    		fps = 0;
	    	}

		    //****** Now let's setup the animation ******
		    
		    final String weatherAnimationPath = selectedFileName + ".rgb565"; //rain.rgb565
		    i = 0;
		    numFrames = selectedFileTotalFrames;
		    System.err.println("Num Frames: " + numFrames);
	            
	            stopExistingTimer(); //is this needed, probably not
	            
	    			if (pixelHardwareID.substring(0,4).equals("TIXL")) {
	    					pixel.interactiveMode();
	    					//send loading image
	    					pixel.writeMode(fps); //need to tell PIXEL the frames per second to use, how fast to play the animations
	    					sendFramesToPIXEL(); //send all the frame to PIXEL
	    					pixel.playLocalMode(); //now tell PIXEL to play locally
	    			}
	    			else {
	    				   stopExistingTimer();
	    				   
	    				   ActionListener AnimateTimer = new ActionListener() {

	    	                    public void actionPerformed(ActionEvent actionEvent) {
	    	                    
	    	               		 	i++;
	    	               			
	    	               			if (i >= numFrames - 1) 
	    	               			{
	    	               			    i = 0;
	    	               			}
	    	               		 
	    	               		//String framestring = "animations/decoded/" + animation_name + "/" + animation_name + i + ".rgb565";
	    	               		String framestring = "animations/decoded/" + weatherAnimationPath + ".rgb565";
	    	               		//String framestring = "animations/decoded/" + animation_name;
	    	               		
	    	               		System.out.println("framestring: " + framestring);

									//pixel.loadRGB565(framestring);
									pixel.SendPixelDecodedFrame(currentDir, gifFileName_, i, numFrames, selectedFileResolution);
							
	    	               		// pixel.SendPixelDecodedFrame(currentDir, gifFileName_, i, numFrames, selectedFileResolution);
	    	      
    	                    }
    	                };
	    				   
	    				   
	    				   timer = new Timer(selectedFileDelay, AnimateTimer); //the timer calls this function per the interval of fps
	    				   timer.start();
	    				   System.out.println("file delay: " + selectedFileDelay);
	    			}    
			}
	    }
	 
	/* private static void decodeGIF() {
		 GifDecoder d = new GifDecoder();
		      //d.read(&quot;sample.gif&quot;);
		      d.read(gifFileName_);
		      int n = d.getFrameCount();
		      for (int i = 0; i < n; i++) {
		         BufferedImage frame = d.getFrame(i);  // frame i
		         int t = d.getDelay(i);  // display duration of frame in milliseconds
		         // do something with frame
		      }
	 }*/
	 
	 private static void sendFramesToPIXEL() { 
   	  int y;
    	 
   	  for (y=0;y<numFrames-1;y++) { //let's loop through and send frame to PIXEL with no delay
 		
 		//framestring = "animations/decoded/" + animation_name + "/" + animation_name + y + ".rgb565";
 		framestring = "animations/decoded/" + animation_name + ".rgb565";
 		
 			System.out.println("writing to PIXEL frame: " + framestring);

 		//  pixel.loadRGB565(framestring);
 		   pixel.SendPixelDecodedFrame(currentDir, gifFileName_, i, numFrames, selectedFileResolution);
   	  } //end for loop
     	 
     }
	    
	    private static void stopExistingTimer()
	    {
	        if(timer != null && timer.isRunning() )
	        {
	            //System.out.println("Stoping PIXEL activity in " + getClass().getSimpleName() + ".");
	            timer.stop();
	        }        
	    }
	 
	 private void setupEnvironment() {
		 
		// currentDir = "c:\\deleteme";
		 
		 int matrix_model = 3;
		 
		 
		 switch (matrix_model) {  //get this from the preferences
	     case 0:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x16;
	    	 frame_length = 1048;
	    	 currentResolution = 16;
	    	 break;
	     case 1:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
	    	 frame_length = 1048;
	    	 currentResolution = 16;
	    	 break;
	     case 2:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32_NEW; //v1
	    	 frame_length = 2048;
	    	 currentResolution = 32;
	    	 break;
	     case 3:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2
	    	 frame_length = 2048;
	    	 currentResolution = 32;
	    	 break;
	     case 4:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_64x32;
	    	 frame_length = 8192;
	    	 currentResolution = 64; 
	    	 break;
	     case 5:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x64; 
	    	 frame_length = 8192;
	    	 currentResolution = 64; 
	    	 break;	 
	     case 6:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_2_MIRRORED; 
	    	 frame_length = 8192;
	    	 currentResolution = 64; 
	    	 break;	 	 
	     case 7:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_4_MIRRORED;
	    	 frame_length = 8192;
	    	 currentResolution = 128; 
	     case 8:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_128x32; //horizontal
	    	 frame_length = 8192;
	    	 currentResolution = 128;  
	    	 break;	 
	     case 9:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x128; //vertical mount
	    	 frame_length = 8192;
	    	 currentResolution = 128; 
	    	 break;	 
	     case 10:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_64x64;
	    	 frame_length = 8192;
	    	 currentResolution = 128; 
	    	 break;	 	 		 
	     default:	    		 
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2 as the default
	    	 frame_length = 2048;
	    	 currentResolution = 32;
	     }
	 }

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return new BaseIOIOLooper() {
			//private DigitalOutput led_;

			@Override
			protected void setup() throws ConnectionLostException,
					InterruptedException {
				//led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
		    	
		    	//**** let's get IOIO version info for the About Screen ****
	  			pixelFirmware = ioio_.getImplVersion(v.APP_FIRMWARE_VER);
	  			//pixelBootloader = ioio_.getImplVersion(v.BOOTLOADER_VER);
	  			pixelHardwareID = ioio_.getImplVersion(v.HARDWARE_VER);
	  			//IOIOLibVersion = ioio_.getImplVersion(v.IOIOLIB_VER);
	  			//**********************************************************
		    	
	  			//	led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
	  				PIXELConsole.this.ioiO = ioio_;
	                pixel.matrix = ioio_.openRgbLedMatrix(pixel.KIND);
	                pixel.ioiO = ioio_;
	                
		
			System.out.println("Found PIXEL: " + pixel.matrix + "\n");
			
			//need to add if statements here, what happens if they choose weather and gif
			
			setupEnvironment();
			
			if (gifModeExternal == true) {
				
				if (writeMode == true) {
					streamGIF(true); //write to PIXEL's SD card
				}
				else {
					streamGIF(false);  //steam the GIF but don't write
				}
			}
			else {
				getWeather();
				//writeImage(); //change this to animate
				runWeatherAnimations();
			}
			
			}

			@Override
			public void loop() throws ConnectionLostException,
					InterruptedException {
				//led_.write(!ledOn_);
				Thread.sleep(10);
			}
		};
	}
}

