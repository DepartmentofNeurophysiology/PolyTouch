# PolyTouch - Multi-touch based GUI tracking software with close-loop experimental control
PolyTouch is an open-source software written in JAVA that enables animal tracking with real-time elementary behavioral classification while providing rapid feedback at millisecond resolution (average communication latency = 1ms). The software consists of a tracking and feedback module, where contact points are continously tracked as computerized X,Y monitor screen coordinates. The user can run the software as a standalone program or call it in the MATLAB (Mathworks) environment. It can be deployed in conjunction with any touch input device, let it be a touch screen with a fixed screen resolution, mouse, touch pen or any interface that utilizes USB Touchscreen Controller (Universal) driver. Commercially, IR sensor frames are sold as “IR touch overframe”, “touch frames”, “multi touch frames” by various manufacturers. Note that PolyTouch is not designed for notebook computers (laptops) with touch screen displays whose resolution can be adjusted with multi touch gestures.

The tracking algorithm makes use of open source jni4net (https://github.com/jni4net/jni4net/) and JWinPointer libraries (http://www.michaelmcguffin.com/code/JWinPointer/). It performs behavioral classification based on the temporal and directional changes in body motion. The user interface displays the current animal position (in X,Y), center-of-mass (COM) position, elapsed time, distance travelled, body speed, relative position of the (virtual) target, and basic behavioral states during data acquisition (Figure 1B). Depending on the relative distance between the ground and the sensor, different portions of the body including limbs, tail, and head can be detected. For the experiments described herein, the sensor was placed on a flat plexiglass sheet for limb detection (Figure 1C).  Behavioral state identification was based on motion profiling of the animal and included discrete states of animal “moving” (body speed > 1 cm/sec), “immobile” (body < 1 cm/sec), and advancing “on the ground” or “off the ground”, as the body part moves out of the 2D plane. Simple behavioral state classification allows the user to monitor animal behavior, which could also be used to trigger stimulus feedback. The touch events are updated and exported to an ASCII file as comma separated values.

# Instructions
PolyTouch.jar
MAIN FILE.

Download JAR file

Download sesFileStart and save in directory C:/Users/Public

Run JAR file by a double (left) mouse click 

Run JAR file from windows command prompt window
  - Open windows command prompt window > change directory to file location (i.e. "cd ...\PolyTouch.jar")
  - Run JAR file with "java -jar PolyTouch.jar"

Run JAR file from MATLAB environment 
  - Run JAR file with "java -jar PolyTouch.jar"
 -	Run PolyTouch_Wrapper.m

PolyTouch_MATLABwrapper
