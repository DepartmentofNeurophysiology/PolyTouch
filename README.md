# PolyTouch - Multi-touch based GUI tracking software with close-loop experimental control
PolyTouch is an open-source software written in JAVA that enables animal tracking with real-time elementary behavioral classification while providing rapid feedback at millisecond resolution (average communication latency = 1ms). The software consists of a tracking and feedback module, where contact points are continously tracked as computerized X,Y monitor screen coordinates. The user can run the software as a standalone program or call it in the MATLAB (Mathworks) environment. It can be deployed in conjunction with any touch input device, let it be a touch screen with a fixed screen resolution, mouse, touch pen or any interface that utilizes USB Touchscreen Controller (Universal) driver.

PolyTouch.jar
MAIN FILE. Runnable PolyTouch.jar file. This GUI-based multi-touch tracking application can be run either as a standalone (by a double mouse click) or from any platform e.g. windows or MATLAB environment (see PolyTouch_MATLABwrapper). The JAR file contains external libraries contained in JWinPointer.jar (freely available from http://www.michaelmcguffin.com/code/JWinPointer/).

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
