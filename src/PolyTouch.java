/** POLYTOUCH - motion tracking of single and multiple contact points with close-loop feedback.
*
* --- DISCLAIMER 04-2019 ---------------------------------------------------------------------------------------------------------------
* - PolyTouch retrieves simultaneous contact points with TouchInfoArray{}, TouchStateItem{}, TouchStateLog{}, PointerGUI ('PolyTouchGUI')
*   and external library JWinPointer.jar written and made available by Michael McGuffin - see http://www.michaelmcguffin.com/code/JWinPointer/)
* - PolyTouch uses the audio out jack of the computer as a communication port to deliver control signals to external devices and generates tones with
*   playTone() and createSineWave() obtained and edited from StdAudio.java available at https://introcs.cs.princeton.edu/java/stdlib/StdAudio.java.html 
* ---------------------------------------------------------------------------------------------------------------------------------------
*
* */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Font;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.BoxLayout;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import jwinpointer.JWinPointerReader;
import jwinpointer.JWinPointerReader.PointerEventListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

class TouchInfo {
	public int deviceType, pointerID;
	public boolean inverted;
	public int x, y, pressure;
}

// READ USER FILE 
// This method is created to read user-specified session parameters from an external ASCII file ('sesFileStart.txt')
//and returns file content as array if readSesParams is called.
class ReadSesFile {

	static String[] readSesParams() {	
		// read sesFileStart to read session parameters		
		File startfile = new File("C:\\Users\\Public/sesFileStart.txt");
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(startfile));
		} catch (FileNotFoundException e2) {
			// Auto-generated catch block
			e2.printStackTrace();
		}
		String str;
		ArrayList<String> list = new ArrayList<String>();
		try {
			while((str = in.readLine()) != null) {
				list.add(str);
			}
		} catch (IOException e1) {
			//  Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e1) {
			// Auto-generated catch block
			e1.printStackTrace();
		}
		String[] readParams = list.toArray(new String[0]);
		return readParams;
	}
}


// TRACKING MODULE - performs motion tracking of single and multiple contact points retrieved using external library JWinPointer.jar
// An array is generated to store information about each contact point.
class TouchInfoArray {
	public ArrayList< TouchInfo > array = new ArrayList< TouchInfo >();

	// returns -1 if none found
	private int getIndex( int deviceType, int pointerID ) {
		for ( int i = 0; i < array.size(); ++i ) {
			if ( array.get(i).deviceType == deviceType && array.get(i).pointerID == pointerID )
				return i;
		}
		return -1;
	}

	// Finds and updates existing pointer info, or creates a pointer info instance if no existing one is found.
	public void updatePointer( int deviceType, int pointerID, boolean inverted, int x, int y, int pressure ) {
		int i = getIndex( deviceType, pointerID );
		TouchInfo TOUCH;

		// remove computer mouse events
		if ( i == -1 ) {
			TOUCH = new TouchInfo();
			TOUCH.deviceType = deviceType;
			TOUCH.pointerID = pointerID;
			array.add(TOUCH);
		} else TOUCH = array.get(i);

		TOUCH.inverted = inverted;
		TOUCH.x = x;
		TOUCH.y = y;
		TOUCH.pressure = pressure;
	}

	// remove pointer if it is not new 
	public void removePointer( int deviceType, int pointerID ) {
		int i = getIndex( deviceType, pointerID );
		if ( i != -1) {
			array.remove(i);
		}
	}
}

class TouchStateItem {
	public String description;
	public int count;

	public TouchStateItem( String d ) {
		description = d;
		count = 1;
	}
}

// Display locomotion parameters of animal
class TouchStateLog {
	private static final int MAX_NUM_ITEMS = 10; //25;
	ArrayList< TouchStateItem > ITEMS = new ArrayList< TouchStateItem >();

	public void log( String message ) {
		int N = ITEMS.size();
		if ( N>0 && message.equals( ITEMS.get(N-1).description ) ) {
			ITEMS.get(N-1).count ++;
		}
		else {
			ITEMS.add( new TouchStateItem( message ) );
			while ( ITEMS.size() > MAX_NUM_ITEMS )
				ITEMS.remove(0);
		}
	}

	public void draw( Graphics2D DRAW, int FONTHEIGHT, int viewportHeight ) {
		for ( int row = 0; row < ITEMS.size(); ++row ) {
			String d = ITEMS.get(row).description;
			int c = ITEMS.get(row).count;
			if ( c > 1 )
				d = d + "(x"+c+")";
			DRAW.drawString( d, 10, viewportHeight-10-(ITEMS.size()-1-row)*FONTHEIGHT /*for debugging*/ - FONTHEIGHT);
		}
	}
}

@SuppressWarnings("serial")
class PolyTouchGUI extends JPanel implements PointerEventListener {
	private Line2D LINE = new Line2D.Float();
	private Ellipse2D.Float TOUCHPOINT = new Ellipse2D.Float();
	private Ellipse2D.Float TARGETZONE = new Ellipse2D.Float();
	private int FONTHEIGHT = 15;
	private Font FONT = new Font( "Sans-serif", Font.BOLD, FONTHEIGHT );

	private Component ROOTCOMPONENT = null; // used to convert between coordinate systems
	private TouchInfoArray TOUCHARRAY = new TouchInfoArray();
	private TouchStateLog LOGGER = new TouchStateLog();

	String[] readParams = ReadSesFile.readSesParams();
	int animalID = Integer.parseInt(readParams[0]);	
	int protocolID = Integer.parseInt(readParams[1]);
	int sessionID = Integer.parseInt(readParams[2]);
	int sessionDur = Integer.parseInt(readParams[3]);
	double targetZoneX = Double.parseDouble(readParams[4]);
	double targetZoneY = Double.parseDouble(readParams[5]);
	double targetZoneRad = Double.parseDouble(readParams[6]);
	double pixelconv = Double.parseDouble(readParams[7]);	

	public PolyTouchGUI(
			// The coordinates we receive for pointer events are with respect to this component.
			// We use this component to convert to the coordinate system of the GUI window.
			Component ROOT
			) {
		ROOTCOMPONENT = ROOT;

		// set background color PolyTouchGUI
		setBorder( BorderFactory.createLineBorder( Color.black ) );
		setBackground( Color.black );

	}
	public Dimension getPreferredSize() {
		return new Dimension( 512, 512 );
	}

	// Initialise time tracking variables and make them global, so that they are accessible from other threads
	public static long startTime = System.nanoTime(); long dStartTime = 0;
	public static double endTime = 0;
	static int staticCount = 0; 
	public static int eventType; 

	// Initialise tracking variables 
	double tsOld; double tsNew;
	double xSUM = 0; double ySUM = 0;
	double xCOM; double xCOMOld; double xCOMNew; double xCOMTempOld = 0; double xCOMTempNew = 0;
	double xCOM1; double xCOM2; double xCOM3; double xCOM4;
	double yCOM; double yCOMOld; double yCOMNew; double yCOMTempOld = 0; double yCOMTempNew = 0;
	double yCOM1; double yCOM2; double yCOM3; double yCOM4;
	double relDist = 0;
	double elapDist = 0; double elapDistOld = 0; double elapDistNew = 0; double elapDistTot = 0;
	double tempSpeed; double tempSpeed1; double tempSpeed2; double tempSpeed3; double tempSpeed4;
	double bodySpeed = 0; double bodySpeedOld = 0;
	double relHead = 0; double relHeadTemp = 0; double relHeadTempNew = 0; double relHeadTempOld = 0;
	double relHead8 = 0; double relHead7 = 0; double relHead6 = 0; double relHead5 = 0;
	double relHead4 = 0; double relHead3 = 0; double relHead2 = 0; double relHead1 = 0;

	// Display contacts with contact id, x position, y position
	public void paintComponent( Graphics g) {

		super.paintComponent( g );
		Graphics2D DRAW = (Graphics2D)g;
		DRAW.setFont(FONT);
		DRAW.setColor( Color.GRAY );
		LOGGER.draw(DRAW, FONTHEIGHT, getHeight());
		int TOUCHRAD = 20;
		int TOUCHCOM = 10;

		xSUM = 0; ySUM = 0;
		double nTouches = 0;

		// Loop through each multi-touch event
		if (TOUCHARRAY.array.size() != 0) {
			//startTime_BC = System.nanoTime(); // elapsed time to compute XY->COM,bodyspeed,etc. variables

			for ( int i = 0; i < TOUCHARRAY.array.size(); ++i ) {
				TouchInfo TOUCH = TOUCHARRAY.array.get(i);

				// Ignore computer mouse events
				if (TOUCH.pointerID != 1) { 
					staticCount ++; // keep track of touches detected simultaneously

					// Save touch variables in object array (x,y position, pressure, id touch) - overwrite file or create new if file does not exist
					if (staticCount == 1) { // get elapsed time for first loop, so that elapsed time can be computed relative from this time point
						dStartTime = System.nanoTime() - startTime;
					} endTime = (System.nanoTime() - startTime - dStartTime); // compute elapsed time in nanoseconds (relative from start time first loop)

					// update time stamp
					tsOld = tsNew;
					tsNew = endTime;

					// store multi-touch events detected at the same time
					xSUM = xSUM+TOUCH.x; 
					ySUM = ySUM+TOUCH.y;
					nTouches = nTouches+1;
					DRAW.setColor( Color.WHITE );

					// draw multi-touches
					TOUCHPOINT.setFrame( TOUCH.x-TOUCHRAD, TOUCH.y-TOUCHRAD, 2*TOUCHRAD, 2*TOUCHRAD );
					DRAW.draw( TOUCHPOINT );

					LINE.setLine( TOUCH.x, TOUCH.y, TOUCH.x+TOUCHRAD, TOUCH.y+2*TOUCHRAD );
					DRAW.draw( LINE );
					String STRING = "x,y,id = " + TOUCH.x + "," + TOUCH.y + "," + TOUCH.pointerID;
					DRAW.drawString(STRING,TOUCH.x+TOUCHRAD,TOUCH.y+3*TOUCHRAD);
				}

				// compute centre-of-mass (COM) - only if touches are detected (nTouches != 0), otherwise read previous COM
				double xCOMTemp = xSUM/nTouches;
				double yCOMTemp = ySUM/nTouches;
				if (Double.isNaN(xCOMTemp)) {
					xCOMTemp = xCOMTempOld;
					yCOMTemp = yCOMTempOld;
				} else {
					xCOMTempOld = xCOMTempNew;
					yCOMTempOld = yCOMTempNew;
					xCOMTempNew = xCOMTemp;
					yCOMTempNew = yCOMTemp;
				}

				// interpolate centre-of-mass (COM) with last 4 COMs
				xCOM4 = xCOM3; xCOM3 = xCOM2; xCOM2 = xCOM1;
				xCOM1 = xCOMTemp; // update xCOM
				xCOM = (xCOM1+xCOM2+xCOM3+xCOM4)/4;
				xCOMOld = xCOMNew; xCOMNew = xCOM;
				yCOM4 = yCOM3; yCOM3 = yCOM2; yCOM2 = yCOM1;
				yCOM1 = yCOMTemp; // update yCOM
				yCOM = (yCOM1+yCOM2+yCOM3+yCOM4)/4;
				yCOMOld = yCOMNew; yCOMNew = yCOM;

				// compute walked distance (cm)
				elapDist = (Math.sqrt(Math.pow(xCOMOld-xCOMNew,2) + Math.pow(yCOMOld-yCOMNew,2)))*pixelconv;
				elapDistOld = elapDistNew; elapDistNew = elapDist; // update variables
				elapDistTot = elapDistTot+elapDist;

				// compute distance to virtual target (in cm)
				double dx = xCOM - targetZoneX; // relative distance of point x from centre target zone (in pixels)
				double dy = yCOM - targetZoneY; // relative distance of point y from centre  target zone (in pixels)
				relDist = (Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2)))*pixelconv; // compute relative distance of point x,y from target centerMagnet (in pixels)

				// compute walked speed (cm/s)
				tempSpeed = Math.abs(elapDistOld-elapDist)/((tsNew-tsOld)/1000000000);
				if (tempSpeed > 100) {
					bodySpeed = 0;
				} else {
					bodySpeed = tempSpeed;
				}

				// compute heading direction (deg) between previous and current COM point
				relHeadTemp = Math.atan2((yCOM-yCOMOld),(xCOM-xCOMOld))*180/Math.PI; // relative angle robot to center of target area
				if (relHeadTemp < 0) {
					relHeadTemp = relHeadTemp+360; // positive angles only
				} 
				relHead8 = relHead7; relHead7 = relHead6; relHead6 = relHead5;
				relHead5 = relHead4; 
				relHead4 = relHead3; relHead3 = relHead2; relHead2 = relHead1;
				relHead1 = relHeadTemp;
				relHead = (relHead8+relHead7+relHead6+relHead5+relHead4+relHead3+relHead2+relHead1)/8;			

				TARGETZONE.setFrame( targetZoneX-(targetZoneRad/pixelconv),targetZoneY-(targetZoneRad/pixelconv),targetZoneRad/pixelconv*2,targetZoneRad/pixelconv*2);			
				DRAW.draw( TARGETZONE );

				// draw centre point as a cross
				LINE.setLine( xCOM,yCOM-TOUCHCOM, xCOM, yCOM+TOUCHCOM );
				DRAW.setColor( Color.RED );
				DRAW.draw( LINE );
				LINE.setLine( xCOM-TOUCHCOM,yCOM, xCOM+TOUCHCOM, yCOM );
				DRAW.setColor( Color.RED );
				DRAW.draw( LINE );

			}
			// Evaluate if session duration has been reached
			if (endTime/1000000000 > sessionDur) {
				System.out.println("Session duration is reached. Motion tracking is terminated...");
				System.exit(0);
			}
		}
	} 

	public void pointerXYEvent(int deviceType, int pointerID, int eventType, boolean inverted, int x, int y, int pressure) {
		Point TOUCH = SwingUtilities.convertPoint(ROOTCOMPONENT, x, y, this);
		x = TOUCH.x;
		y = TOUCH.y;
		TOUCHARRAY.updatePointer( deviceType, pointerID, inverted, x, y, pressure );

		LOGGER.log(genereateStateLog(pointerID,eventType));
		repaint();

		if (bodySpeed < 1) {
			eventType = 0;
		}

		// Store data in external text file
		Object content[]; 
		BufferedWriter varout = null;
		content = new Object[] {x, y, xCOM, yCOM,relHead,pressure,pointerID,eventType,elapDistTot,bodySpeed,relDist,endTime};
		String dataPre = Arrays.toString(content);
		String data = dataPre.replace("[","").replace("]","").replaceAll(",",""); // set proper format of output file, because Arrays.toString() has a standard format of adding brackets and commas
		try {
			// create new output file at specified directory
			String sesFileString = "C:\\Users\\Public/sesFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
			File sesFile = new File(sesFileString);
			varout = new BufferedWriter(new FileWriter(sesFile, true));
			varout.write(data); // save content as array
			varout.newLine(); // save content at new line
			varout.flush();
			varout.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (varout != null) { 
					varout.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void pointerButtonEvent(int deviceType, int pointerID, int eventType, boolean inverted, int buttonIndex) {
		LOGGER.log(genereateStateLog(pointerID,eventType));
		repaint();
	}

	// get pointerEvent 
	public void pointerEvent( int deviceType, int pointerID, int eventType, boolean inverted) {
		LOGGER.log(genereateStateLog(pointerID,eventType));
		repaint();
		TOUCHARRAY.removePointer( deviceType, pointerID );
	}

	private static final int EVENT_TYPE_DRAG = 1;
	private static final int EVENT_TYPE_HOVER = 2;
	private static final int EVENT_TYPE_DOWN = 3;
	private static final int EVENT_TYPE_UP = 4;
	private static final int EVENT_TYPE_BUTTON_DOWN = 5;
	private static final int EVENT_TYPE_BUTTON_UP = 6;
	private static final int EVENT_TYPE_IN_RANGE = 7;
	private static final int EVENT_TYPE_OUT_OF_RANGE = 8;
	private String genereateStateLog( int pointerID, int eventType) {	
		String STRING = "";
		switch ( eventType ) {
		case EVENT_TYPE_DRAG :
			if (bodySpeed > 1) {
				STRING =  "Moving";
			} else {
				STRING = "Immobile";
			}
			break;
		case EVENT_TYPE_HOVER :
			if (bodySpeed > 1) {
				STRING = "Moving";
			} else {
				STRING = "Immobile";
			}
			break;
		case EVENT_TYPE_DOWN :
			STRING = "On the ground";
			break;
		case EVENT_TYPE_UP :
			STRING = "Off the ground";
			break;
		case EVENT_TYPE_BUTTON_DOWN :
			STRING = "On the ground";
			break;
		case EVENT_TYPE_BUTTON_UP :
			STRING = "Off the ground";
			break;
		case EVENT_TYPE_IN_RANGE :
			STRING = "Animal detected";
			break;
		case EVENT_TYPE_OUT_OF_RANGE :
			STRING = "Animal not detected";
			break;
		default:
			STRING = "?";
			break;
		}
		STRING = "time: " + Math.round((endTime/1000000000)*100)/100 + " s - walked: " + Math.round(elapDistTot*100)/100 + " cm - speed: " + Math.round(bodySpeed*100)/100 + " cm/s - dist to T: " + Math.round((relDist-targetZoneRad)*100)/100 + "cm - "+ STRING;
		return STRING;
	}
} 

/** TRIGGER AUDIO FEEDBACK
 *  tone volume is regulated with gain control
 *  
 *  init(), start(), close(), flush(), playTone(), createSineWave() from: StdAudio.java https://introcs.cs.princeton.edu/java/stdlib/StdAudio.java.html 
 *  
 *  */
class TriggerSound {
	static String[] readParams = ReadSesFile.readSesParams();
	private static int animalID = Integer.parseInt(readParams[0]);	
	private static int protocolID = Integer.parseInt(readParams[1]);
	private static int sessionID = Integer.parseInt(readParams[2]);
	private static double sessionDur = Double.parseDouble(readParams[3]);
	private static double targetZoneRad = Double.parseDouble(readParams[6]);
	private static double toneAmp = Double.parseDouble(readParams[8]);
	private static double toneFreq = Double.parseDouble(readParams[9]);
	private static double toneDur = Double.parseDouble(readParams[10]);
	private static double toneFs = Double.parseDouble(readParams[11]);
	private static double tonePeriod = Double.parseDouble(readParams[12]);
	private static double toneFreq5 = 760; // high frequency(Hz)
	private static double toneFreq4 = 600;
	private static double toneFreq3 = 450;
	private static double toneFreq2 = 300;
	private static double toneFreq1 = 150; // low frequency (Hz)	
	double targetZoneRad1 = 5; // zone 1
	double targetZoneRad2 = 10; // zone 2
	double targetZoneRad3 = 15; // zone 3
	double targetZoneRad4 = 20; // zone 4

	private static final int BYTES_PER_SAMPLE = 2; // 2; //2;                // 16-bit audio
	private static final int BITS_PER_SAMPLE = 16; //16;                // 16-bit audio
	private static final double MAX_16_BIT = 32767 ; // Short.MAX_VALUE;     // 32,767 
	private static final int SAMPLE_BUFFER_SIZE = 4096; // 4096;

	private static SourceDataLine LINE;   // to play the sound
	private static byte[] BUFFER;         // initialise internal buffer
	private static int bufferSize = 0;    // number of samples currently in internal buffer	
	private static float GAIN = 0f; 	// set at 0f initially to ignore first beep
	
	// open up new audio stream
	private static void init() {
		try {
			// 44,100 samples per second, 16-bit audio, mono, signed PCM, little Endian
			AudioFormat format = new AudioFormat((float) toneFs, BITS_PER_SAMPLE, 1, true, false);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			LINE = (SourceDataLine) AudioSystem.getLine(info);
			LINE.open(format, SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE); // original code; acquires the system resources

			GAIN = 0;
			FloatControl VOLUME = (FloatControl) LINE.getControl(FloatControl.Type.MASTER_GAIN);
			VOLUME.setValue(VOLUME.getMinimum() * (1 - GAIN));

			// the internal buffer is a fraction of the actual buffer size, this choice is arbitrary
			// it gets divided because we can't expect the buffered data to line up exactly with when
			// the sound card decides to push out its samples.
			BUFFER = new byte[SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE/3];
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

		// no sound gets made before this call (clean sound)
		LINE.start();
	}

	// Close standard audio.
	public static void close() {
		LINE.drain();
		LINE.stop(); // (prevent audio stream leaking)
	}

	// Close standard audio.
	public static void flush() {
		LINE.flush();
	}

	// Write one sample (between -1.0 and +1.0) to standard audio. If the sample
	// is outside the range, it will be clipped.
	public static void playTone(double in) {
		// clip if outside [-1, +1]
		if (in < -1.0) in = -1.0;
		if (in > +1.0) in = +1.0;

		// convert to bytes
		short s = (short) (MAX_16_BIT * in);
		BUFFER[bufferSize++] = (byte) s;
		BUFFER[bufferSize++] = (byte) (s >> 8);   // little Endian
		// send to sound card if buffer is full        
		if (bufferSize >= BUFFER.length) {
			LINE.write(BUFFER, 0, BUFFER.length);
			bufferSize = 0;
		}
	}

	// Write an array of samples (between -1.0 and +1.0) to standard audio. If a sample
	// is outside the range, it will be clipped.
	public static void play(double[] sineWaveSample) {
		for (int i = 0; i < sineWaveSample.length; i++) {
			playTone(sineWaveSample[i]);
		}
	}

	// create sine wave sample that will be converted to byte format by StdAudio, so that it can be written to the audio device
	private double[] createSineWave( double toneFreq, double toneDur ) {
		int nSamples = (int) (toneFs* toneDur);
		double[] sineWaveSample = new double[nSamples+1];
		for (int i = 0; i <= nSamples; i++)
			sineWaveSample[i] = toneAmp * Math.sin(2 * Math.PI * i * toneFreq / toneFs);
		return sineWaveSample;
	}

	/** --- PROTOCOL 1: POSITIONAL FEEDBACK IS PROVIDED AS A DISCRETE 1 SEC 450 HZ TONE PULSE
	 *  IF THE ANIMAL IS DETECTED IN THE VIRTUAL TARGET ZONE
	 * 	- Session 1: no feedback (class TriggerSound is never called)
	 *  - Session 2: feedback with intensity 39 dB
	 *  - Session 3: feedback with intensity 49 dB
	 *  - Session 4: feedback with intensity 59 dB
	 *  - Session 5: pseudo-random presentation of 10s 39,49,59 dB (3x each)
	 */

	// Initialise time and event variables, so that feedback trigger time stamps can be monitored
	public static long startTime2 = System.nanoTime();
	long dStartTime2 = 0;
	long endTimeNs2 = 0;
	double relDistLast = 0;
	double lastTime = 0;
	double lastEvent = 0;
	
	// alternative method (to avoid screeching sound)
	void playSoundStatic() {
		init();
		play(createSineWave(toneFreq,sessionDur));
		//close();
	}

	void playSoundDiscr () throws IOException {
		String sesFileString = "C:\\Users\\Public/sesFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";	
		File sesFile = new File(sesFileString);
		if(sesFile.exists()) {	
			BufferedReader in = new BufferedReader(new FileReader(sesFile));
			String lastLine = ""; String currentLine;
			while ((currentLine = in.readLine()) != null) {
				lastLine = currentLine;
			}
			in.close();
			String[] splitLine = lastLine.split(" "); // split string after empty space
			String lastTimeString = splitLine[splitLine.length-1]; // get last time sample
			String relDistLastString = splitLine[splitLine.length-2]; // get relDist located at end-2
			String lastEventString = splitLine[splitLine.length-3];
			lastTime = Double.parseDouble(lastTimeString);
			relDistLast = Double.parseDouble(relDistLastString);
			lastEvent = Double.parseDouble(lastEventString);
			
			// Stimulus condition: only trigger feedback if PolyTouchGUI is running (tracked time > 0.0 sec)			
			// optional: create stimulus protocol where condition depends on relative distance and basic behavioural state (e.g. mobile vs immobile)
			if (relDistLast < targetZoneRad && lastTime > 0.0) {
				GAIN = 0.6f;
				FloatControl VOLUME = (FloatControl) LINE.getControl(FloatControl.Type.MASTER_GAIN);
				VOLUME.setValue(VOLUME.getMinimum() * (1 - GAIN));
			} else {
				GAIN = 0f;
				FloatControl VOLUME = (FloatControl) LINE.getControl(FloatControl.Type.MASTER_GAIN);
				VOLUME.setValue(VOLUME.getMinimum() * (1 - GAIN));
				}
	
			// Save feedback trigger time stamp in external file
			Object content[]; 
			BufferedWriter varout = null;
			endTimeNs2 = (System.nanoTime() - startTime2 - dStartTime2); // compute elapsed time in nanoseconds (relative from start time first loop)
			
			// define content of external file
			content = new Object[] {relDistLast,endTimeNs2};			
			String dataPre = Arrays.toString(content);
			String data = dataPre.replace("[","").replace("]","").replaceAll(",",""); // set proper format of output file, because Arrays.toString() has a standard format of adding brackets and commas
			try {
				String audioFileString = "C:\\Users\\Public/sesAudioFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
				File audioFile = 
						new File(audioFileString);
				varout = new BufferedWriter(new FileWriter(audioFile, true));
				varout.write(data); // save content as array
				varout.newLine(); // save content at new line
				varout.flush();
				varout.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (varout != null) { 
						varout.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} // end check if sesFile exists
	} // end method generate sound

	// SESSION 5 - pseudo-random presentation of 10s 39,49,59 dB (3x each)
	void playSoundDiscrRandom(int COUNTER, ArrayList<Integer> intRand) throws IOException {
		double delayMin = 0.5* 60; // minimum delay in sec
		double delayMax = tonePeriod - toneDur ; // maximum delay in sec

		// execute tasks till each stimulus is presented 3 times (n tasks = 3x3, pseudo-random)
		double delayDouble = ((Math.random() * (delayMax-delayMin)) + delayMin) * 1000; // delay in ms
		long delay = (long) delayDouble; // delay in ms
		System.out.println(delay);
		System.out.println(COUNTER);
		if (COUNTER < intRand.size()) {
			Timer timerAudio = new Timer();
			timerAudio.schedule(new TimerTask() {
				public void run() {
					if (intRand.get(COUNTER) == 0 || intRand.get(COUNTER) == 3 || intRand.get(COUNTER) == 6) {
						GAIN = 0.6f;
						System.out.println("39 dB");
					} else if (intRand.get(COUNTER) == 1 || intRand.get(COUNTER) == 4 || intRand.get(COUNTER) == 7) {
						GAIN = 0.8f;
						System.out.println("49 dB");
					} else if (intRand.get(COUNTER) == 2 || intRand.get(COUNTER) == 5 || intRand.get(COUNTER) == 8) {
						GAIN = 1f;
						System.out.println("65 dB");
					}
					FloatControl volume = (FloatControl) LINE.getControl(FloatControl.Type.MASTER_GAIN);
					volume.setValue(volume.getMinimum() * (1 - GAIN));

					// Generate sound tone
					init();
					play(createSineWave(toneFreq,toneDur)); // 3s 150 Hz with 39, 49, 65 dB volume

					// Save feedback trigger time stamp in external file
					Object content[]; 
					BufferedWriter varout = null;
					endTimeNs2 = (System.nanoTime() - startTime2 - dStartTime2); // compute elapsed time in nanoseconds (relative from start time first loop)

					// define content of external file
					content = new Object[] {relDistLast,endTimeNs2};
					String dataPre = Arrays.toString(content);
					String data = dataPre.replace("[","").replace("]","").replaceAll(",",""); // set proper format of output file, because Arrays.toString() has a standard format of adding brackets and commas
					try {
						String audioFileString = "C:\\Users\\Public\\sesAudioFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
						File audioFile = 
								new File(audioFileString);
						varout = new BufferedWriter(new FileWriter(audioFile, true));
						varout.write(data); // save content as array
						varout.newLine(); // save content at new line
						varout.flush();
						varout.close();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							if (varout != null) { 
								varout.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					close();
				}
			},delay);
		} else {
			System.out.println("Session duration reached. Mouse tracking is terminated...");
			System.exit(0);
		}
	}

	/** --- PROTOCOL 2: SPATIAL FEEDBACK IS PROVIDED AS A CONTINUOUS FREQUENCY MODULATED TONE
	 *	THAT SCALES WITH THE ANIMAL'S DISTANCE TO A TARGET LOCATION
	 * 	- Session 1: no feedback (class TriggerSound is never called)
	 *  - Session 2: feedback with close distance~frequencies 150-300-450-600-750 Hz tone 49dB
	 *  - Session 3: feedback with close distance~frequencies 750-600-450-300-150 Hz tone 49dB
	 */

	// SESSION 2 
	void playSoundContLowHigh() throws IOException {	
		// read sesFile to get last stored relative distance
		String sesFileString = "C:\\Users\\Public/sesFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
		File curFile = new File(sesFileString);
		if(curFile.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(curFile));
			String lastLine = ""; String currentLine;
			while ((currentLine = in.readLine()) != null) {
				lastLine = currentLine;
			}
			in.close();

			String[] splitLine = lastLine.split(" "); // split string after empty space
			String relDistLaststring = splitLine[splitLine.length-2]; // get relative distance
			double relDistLast = Double.parseDouble(relDistLaststring);

			init();
			GAIN = .8f;
			FloatControl volume = (FloatControl) LINE.getControl(FloatControl.Type.MASTER_GAIN);
			volume.setValue(volume.getMinimum() * (1 - GAIN));

			// if animal is in area 1-5
			if (relDistLast < targetZoneRad1) {
				play(createSineWave(toneFreq,toneDur));
				toneFreq = toneFreq1;
			} else if ((relDistLast < targetZoneRad2) && ((relDistLast > targetZoneRad1))) {
				toneFreq = toneFreq2;
				play(createSineWave(toneFreq,toneDur)); 
			} else if ((relDistLast < targetZoneRad3) && (relDistLast > targetZoneRad2)) {
				toneFreq = toneFreq3;
				play(createSineWave(toneFreq,toneDur)); 
			} else if ((relDistLast < targetZoneRad4) && (relDistLast > targetZoneRad3)) {
				toneFreq = toneFreq4;
				play(createSineWave(toneFreq,toneDur)); 
			} else {
				toneFreq = toneFreq5;
				play(createSineWave(toneFreq,toneDur)); 
			}

			// Save feedback trigger time stamp in external file
			Object content[]; 
			BufferedWriter varout = null;
			endTimeNs2 = (System.nanoTime() - startTime2 - dStartTime2); // compute elapsed time in nanoseconds (relative from start time first loop)

			// define content of external file
			content = new Object[] {relDistLast,endTimeNs2};
			String dataPre = Arrays.toString(content);
			String data = dataPre.replace("[","").replace("]","").replaceAll(",",""); // set proper format of output file, because Arrays.toString() has a standard format of adding brackets and commas
			try {
				String audioFileString = "C:\\Users\\Public/sesAudioFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
				File audioFile = 
						new File(audioFileString);
				varout = new BufferedWriter(new FileWriter(audioFile, true));
				varout.write(data); // save content as array
				varout.newLine(); // save content at new line
				varout.flush();
				varout.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (varout != null) { 
						varout.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// SESSION 3 
	void playSoundContHighLow() throws IOException {
		// read sesFile to get last stored relative distance
		File curFile = new File("C:\\Users\\Public/sesFile.txt");
		if(curFile.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(curFile));
			String lastLine = ""; String currentLine;
			while ((currentLine = in.readLine()) != null) {
				lastLine = currentLine;
			}
			in.close();

			String[] splitLine = lastLine.split(" "); // split string after empty space
			String relDistLaststring = splitLine[splitLine.length-2]; // get relative distance
			double relDistLast = Double.parseDouble(relDistLaststring);
			init();

			// if animal is in area 1-5
			if (relDistLast < targetZoneRad1) {
				play(createSineWave(toneFreq,toneDur));
				toneFreq = toneFreq5;
			} else if ((relDistLast < targetZoneRad2) && ((relDistLast > targetZoneRad1))) {
				toneFreq = toneFreq4;
				play(createSineWave(toneFreq,toneDur)); 
			} else if ((relDistLast < targetZoneRad3) && (relDistLast > targetZoneRad2)) {
				toneFreq = toneFreq3;
				play(createSineWave(toneFreq,toneDur)); 
			} else if ((relDistLast < targetZoneRad4) && (relDistLast > targetZoneRad3)) {
				toneFreq = toneFreq2;
				play(createSineWave(toneFreq,toneDur)); 
			} else {
				toneFreq = toneFreq1;
				play(createSineWave(toneFreq,toneDur)); 
			}

			// Save feedback trigger time stamp in external file
			Object content[]; 
			BufferedWriter varout = null;
			endTimeNs2 = (System.nanoTime() - startTime2 - dStartTime2); // compute elapsed time in nanoseconds (relative from start time first loop)

			// define content of external file
			content = new Object[] {relDistLast,endTimeNs2};
			String dataPre = Arrays.toString(content);
			String data = dataPre.replace("[","").replace("]","").replaceAll(",",""); // set proper format of output file, because Arrays.toString() has a standard format of adding brackets and commas
			try {
				String audioFileString = "C:\\Users\\Public/sesAudioFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
				File audioFile = 
						new File(audioFileString);
				varout = new BufferedWriter(new FileWriter(audioFile, true));
				varout.write(data); // save content as array
				varout.newLine(); // save content at new line
				varout.flush();
				varout.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (varout != null) { 
						varout.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// close();
		} // end check if sesFileJK exists
	} 
}

public class PolyTouch {
	private static JWinPointerReader pointerReader;

	JFrame FRAME;
	PolyTouchGUI GUI;
	Container TOOLPANEL;

	/** --- (1) Create user interface --- */
	private void pointerUI() {
		if ( ! SwingUtilities.isEventDispatchThread() ) {
			System.out.println(
					"Warning: UI is not being created in the Event Dispatch Thread!");
			assert false;
		}
		JMenuBar MENUBAR = new JMenuBar();

		// set UI frame properties and add canvas to frame
		FRAME = new JFrame( "PolyTouch" );
		FRAME.setVisible( true );
		FRAME.setJMenuBar(MENUBAR);

		GUI = new PolyTouchGUI(
				MENUBAR
				);		
		Container PANE = FRAME.getContentPane();
		PANE.setLayout( new BoxLayout( PANE, BoxLayout.X_AXIS ) );
		PANE.add( GUI );
		pointerReader = new JWinPointerReader(FRAME);
		pointerReader.addPointerEventListener(GUI);

		// terminate tracking if UI frame is closed
		FRAME.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices(); // get available screen devices
		if (screens.length > 1) {
			screens[1].setFullScreenWindow(FRAME); // display UI fullscreen on extended screen
		} else if( screens.length > 0 ) {
			screens[0].setFullScreenWindow(FRAME); // display UI fullscreen on extended screen
		} else {
			throw new RuntimeException( "No Screens Found" );
		}
	}

	// read session parameters from ReadSesFile
	public static int COUNTER = -1;
	public static ArrayList<Integer> intRand = new ArrayList<>();
	public static String[] readParams = ReadSesFile.readSesParams();
	public static int animalID = Integer.parseInt(readParams[0]);	
	public static int protocolID = Integer.parseInt(readParams[1]);
	public static int sessionID = Integer.parseInt(readParams[2]);
	public static int sessionDur = Integer.parseInt(readParams[3]);	

	/** --- (2) RUN TRACKING AND FEEDBACK THREADS IN PARALLEL --- */
	public static void main( String[] arg ) throws AWTException, InterruptedException {
		// remove old file if already exists
		String sesFileString = "C:\\Users\\Public/sesFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
		File sesFile = new File(sesFileString);
		sesFile.delete();

		String audioFileString = "C:\\Users\\Public/sesAudioFile_" + "A" + animalID + "P" + protocolID + "S" + sessionID + ".txt";
		File audioFile = new File(audioFileString);
		audioFile.delete();

		/** --- SCHEDULE POSITION TRACKING THREAD --- */		
		// schedule update of pointerUI for the event-dispatch thread 
		javax.swing.SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						PolyTouch newPolyTouchGUI = new PolyTouch();
						newPolyTouchGUI.pointerUI();
						System.out.println("Initiated new pointerGUI...");						
					}
				}
				);

		/** --- CLOSE-LOOP FEEDBACK THREAD --- */
		if (sessionID != 1) {
			// schedule feedback protocol 1: 
			// generate discrete 450Hz tone after delay of 0 ms (arg2) every 1 ms (arg3)
			if (protocolID == 1) {
				if (sessionID != 5) { // session 2-4; only provide feedback for non-baseline sessions
					Timer timerInitAudio = new Timer();
					timerInitAudio.schedule(
							new TimerTask() {
								public void run() {
									TriggerSound changeSound = new TriggerSound();
									changeSound.playSoundStatic(); // with screeching sound
								}
							},0);
	
					Timer timerChangeAudio = new Timer();
					timerChangeAudio.scheduleAtFixedRate(
							new TimerTask() {
								public void run() {
									TriggerSound changeSound = new TriggerSound();
									try {
										 changeSound.playSoundDiscr(); // with screeching sound
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							},0,1);
					// 2nd arg = run delay; run first occurrence immediately
					// 3rd arg = period in ms; run every three seconds (e.g. 3000)
					System.out.println("Initializing discrete feedback protocol...");
				} else if (sessionID == 5) {
					// create 9x pseudo-randomised stimulus vector: 3x 39/49/59dB
					for (int i = 0; i < 9; ++i ){
						intRand.add(i);
					} 
					// generate pseudo-random numbers by shuffling array of numbers 1:9
					Collections.shuffle(intRand);

					Timer timerAudio = new Timer();
					timerAudio.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							TriggerSound playTest = new TriggerSound();
							try {
								COUNTER++;
								playTest.playSoundDiscrRandom(COUNTER,intRand);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} // end run 
					},0,sessionDur*1000); // end TimerTask (arg = delay)
					System.out.println("Initializing random feedback protocol...");
				}
			}			
			// schedule feedback protocol 2: 
			// generate continuous tone after delay of 0 ms (arg2) every 1 ms (arg3)
			else if (protocolID == 2) {
				Timer timerFeedback = new Timer();
				timerFeedback.scheduleAtFixedRate(
						new TimerTask() {
							public void run() {
								TriggerSound playNow = new TriggerSound();
								try {
									if (sessionID == 2) {
										playNow.playSoundContLowHigh();
									}
									if (sessionID == 3) {
										playNow.playSoundContHighLow();
									}
								} catch (IOException e) {
									e.printStackTrace();
								} 
							} 
						},0,1); // schedule timerAudio; arg2=delay(ms); arg3=period(ms);
				System.out.println("Initializing continuous feedback protocol...");
			}
		}
	}
}
