sesFileStart = PolyTouch_startWrap();
%% [POLYTOUCH_STARTWRAP] This function generates a session file (in .txt format)
% that specifies the protocol and session variables defined by the user and
% calls PolyTouch to track the spatial position (X,Y in pixels) of unrestrained
% animals and trigger feedback in close-loop based on the animal position.
% PolyTouch is an open-source tracking software available on https://github.com/DepartmentofNeurophysiology/PolyTouch. 
% 
% PolyTouch is terminated when the user-specified session duration is reached or can be interrupted anytime if the user closes the graphical user interface (GUI).
% OUTPUT
% - sessionFile (a struct) with fields
%       - track.x: is the x coordinate of mouse location in pixels.
%       - track.y: is the y coordinate of mouse location in pixels.
%       - track.ts: is the timestamp of sampled mouse coordinates in s.
%       - track.relDistTarget: is the relative distance from target location in cm.
%       - % optional: head.angle: is the polar coordinate in degrees, radius as the relative distance from target location in cm.
%       - % optional: head.speed in cm/s
% - figure file (a pdf file)
% 
% PROTOCOL DESCRIPTION
% The current wrapper provides the user with two feedback protocols:
% - In protocol 1, discrete (positional) feedback triggers an auditory
% stimulus with a principal frequency of 150 Hz whenever the animal is
% in a user defined virtual target zone.
% - In protocol 2, continuous (distance) feedback triggers a frequency modulated
% tone that scales with the relative distance of the animal to the virtual
% target zone.
%
% % Protocol 1: DISCRETE POSITIONAL FEEDBACK during free exploration in 
% open field during light conditions - session 1-5
% Session #   Session type                Description
% S1          No feedback                 No feedback is given. Baseline condition for open field behaviour. Animal is habituated to open field.
% S2          Discrete tone 150 Hz 39 dB  Feedback is delivered whenever animal is in target area.
% S3          Feedback tone 150 Hz 49 dB  Feedback is delivered whenever animal is in target area.
% S4          Feedback tone 150 Hz 59 dB  Feedback is delivered whenever animal is in target area.
% S5          Random tone 150 Hz          Random 150 Hz tone with 39, 49 or 59 dB (independent of spatial position animal)
%
% Protocol 2: CONTINUOUS SPATIAL FEEDBACK during free exploration in open
% field during light conditions - session 1-3
% Session #   Session type                            Description
% S1          No feedback                             No feedback is given. Baseline condition for open field behaviour. Animal is habituated to open field.
% S2          Ascending freq-modulated tones 49 dB    Continuous frequency modulated tone is delivered that scales with the distance of the animal relative to a virtual target area (150-300-450-600-750 Hz).
% S3          Descending freq-modulated tones 49 dB   Continuous frequency modulated tone are delivered that scales negatively with the distance of the animal relative to a virtual target area (750-600-450-300-150 Hz).

%% 0. Set cd path 
% Addpath to PolyTouch_startUpWrap
addpath(genpath('C:\Users\Gebruiker\eclipse-workspace\PolyTouch\src'))

% Set current directory to file location of PolyTouch.jar (required to call
% PolyTouch within the MATLAB environment)
cd 'C:\Users\Gebruiker\eclipse-workspace\PolyTouch\lib' % set current directory to mouseTrackJ.jar

%% 1. Request protocol and session variables
% Note: animalID, protocolID and sessionID MUST be specified.
animalID = input('Please enter the animal number (e.g. 1, 2, 3) : '); % animal identity number
protocolID = input('Please enter the protocol number (e.g. 1, 2, 3) : ');
sessionID = input('Please enter the session number (e.g. 1, 2, 3) : ');
sessionDur = input('Please enter the session duration (sec) (e.g. 60) : '); % in seconds

% check protocol and session variables
if isempty(animalID)
    error('Error: animalID is not defined by user');
    animalID = input('Please provide the animal number (e.g. 1, 2, 3) : '); % animal identity number
elseif ~isnumeric(animalID)
    error('Error: animalID is not defined by user');
    animalID = input('Please provide the animal number (e.g. 1, 2, 3) : '); % animal identity number 
end
if isempty(protocolID)
    error('Error: protocolID is not defined by user');
    protocolID = input('Please provide the protocol number (e.g. 1, 2, 3) : '); % animal identity number
elseif ~isnumeric(protocolID)
    error('Error: protocolID is not defined as a number');
    protocolID = input('Please provide the protocol number (e.g. 1, 2, 3) : '); % animal identity number
end
if isempty(sessionID)
    error('Error: sessionID is not defined by user');
    sessionID = input('Please provide the session number (e.g. 1, 2, 3) : '); % animal identity number
elseif ~isnumeric(sessionID)
    error('Error: sessionID is not defined as a number');
    sessionID = input('Please provide the session number (e.g. 1, 2, 3) : '); % animal identity number
end
if isempty(sessionDur)
    sesDur_base = 60*5; % session duration = 5 min for baseline session if unspecified by user
    sesDur_tone = 60*20; % session duration = 20 min for feedback session if unspecified by user
end
if ~isnumeric(sessionDur)
    sesDur_base = 60*5; % session duration = 5 min for baseline session if unspecified by user
    sesDur_tone = 60*20; % session duration = 20 min for feedback session if unspecified by user
end

%% 2. Generate start session file
disp('Generating sessionFile...')
datestamp = char(datetime('now','timezone','local','format','yyyyMMdd_HHmmss'));

% (a) Define target area. We randomly assigned the target zone either as the left
% or right portion of the open field arena. 
% (b) Get monitor info to convert computerized pixels to real measures (pixels -> cm) 
set(0,'units','inches')
s_i_det = get(0,'MonitorPositions');
set(0,'units','pixels')  % get screen resolution in pixels
s_r_det = get(0,'MonitorPositions');
if size(s_r_det,1) < 2 % if sensor device is connected to extended monitor screen, use this screen to track mouse location
    s_r = s_r_det; % pick extended screen if 2 monitors are detected
    s_i = s_i_det;
else
    s_r = s_r_det(2,:); % pick extended screen if 1 monitor is detected
    s_i = s_i_det(2,:);
end
pix2cm = (s_i(3)/s_r(3))*2.540; %2.540 is the conversion factor for pixels to cm

targetZoneTypes = [0 s_r(4)/2; s_r(3) s_r(4)/2]; % [x y area 1; x y area 2] coordinates in pixels
targetZoneRad = 5; % in cm, width of the target zone
pickTargetZone = randi([1 2],1,1); % randomly pick target area 1 or 2

if protocolID == 1
    if sessionID == 1
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'noTone';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    elseif sessionID == 2
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'Tone150Hz39dB';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    elseif sessionID == 3
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'Tone150Hz49dB';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    elseif sessionID == 4
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'Tone150Hz59dB';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    elseif sessionID == 5
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'ToneRandomdB';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    end
elseif protocolID == 2
    if sessionID == 1
        targetZone = targetZoneTypes(pickTargetZone,:);
        targetZoneRad = 0;
        feedbackType = 'noTone';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    elseif sessionID == 2
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'ToneAscendFreq49dB';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    elseif sesID == 3
        targetZone = targetZoneTypes(pickTargetZone,:);
        feedbackType = 'ToneDescendFreq49dB';
        toneAmp = 1;
        toneFreq = 150;
        toneDur = 1;
        toneFs = 14400;
        tonePeriod = 1/toneDur;
    end
end
disp('...')

% Create sessionFile
sessionFile.animalID = animalID; % animal identity number
sessionFile.protocolID = protocolID; % protocol number
sessionFile.sessionID = sessionID;  % session number
sessionFile.sessionDur = sessionDur; % session duration, in sec
sessionFile.date = datestamp;
sessionFile.targetZone = targetZone; % xy coordinates target zone, in pixels
sessionFile.targetZoneRad = targetZoneRad; % radius target zone, in cm
sessionFile.tag = strcat(char(datetime('now','TimeZone','local','Format','y-MM-d')),'-',num2str(animalID), '-P', num2str(protocolID), '-S',num2str(sessionID)); % for labeling purposes
sessionFile.track.x = []; % in pixels
sessionFile.track.y = []; % in pixels
sessionFile.track.pressure = [];
sessionFile.track.pointerID = []; % identity of touch point
sessionFile.track.relDistTarget = []; % relative distance to target zone, in cm
sessionFile.track.ts = []; % elapsed time from start, in sec
sessionFile.track.eventType = []; % behavioral state (mobile, immobile)
sessionFile.track.elapDistTot = []; % total elapsed distance, in cm
sessionFile.head.angle = []; % heading angle, in degrees
sessionFile.head.speed = []; % heading speed, in cm/sec
sessionFile.toneAmp = toneAmp; % amplitude of feedback tone
sessionFile.toneFreq = toneFreq; % frequency of feedback tone
sessionFile.toneDur = toneDur; % duration of feedback tone
sessionFile.toneFs = toneFs; % sampling frequency of feedback tone
sessionFile.tonePeriod = tonePeriod; % period of feedback tone
disp('...')

% Generate start session file 
targetZoneX = targetZone(1); % in pixels
targetZoneY = targetZone(2); % in pixels
sesArray = [animalID;protocolID;sessionID;sessionDur;targetZoneX;targetZoneY;targetZoneRad;pix2cm;toneAmp;toneFreq;toneDur;toneFs;tonePeriod];
tempSpec = '%d\r\n'; % make sure no spacings are included
formatSpec = repmat(tempSpec,1,size(sesArray,1));
filename = 'C:\Users\Public\sesFileStart.txt';
fileID = fopen(filename, 'w');
if exist('fileID')
    fprintf(fileID,formatSpec,sesArray);
else
    error('fileID does not exist')
end
fclose(fileID);

% if startbutton is clicked
disp('Initialising PolyTouch...')
system('java -jar PolyTouch.jar')

% if sessionDur is reached
disp('Mouse tracking terminated...')

%% 3. Generate post-tracking session file
filenameJ = sprintf('C:\Users\Public\sesFile_%.0fP%.0fS%.0f.txt',animalID,protocolID,sessionID);
delimiterJ = ' ';
fileIDJ = fopen(filenameJ,'r');
tempSpecJ = '%f';

numVars = 12; % specify number of output variables sesFileJ - {x, y, xCOM, yCOM,relHead,pressure,pointerID,eventType,elapDistTot,bodySpeed,relDist,endTime};
formatSpecJ = strcat(repmat([tempSpecJ],1,numVars),'%[^\n\r]');
dataArray = textscan(fileIDJ, formatSpecJ, 'Delimiter', delimiterJ, 'MultipleDelimsAsOne', true, 'EmptyValue' ,NaN, 'ReturnOnError', false);
fclose(fileIDJ);

% Update session file: store tracking variables from JAVA-generated session file
sessionFile.track.x = dataArray{:,1}; % in pixels
sessionFile.track.y = dataArray{:,2}; % in pixels
sessionFile.track.xCOM = dataArray{:,3}; % center-of-mass X in pixels
sessionFile.track.yCOM = dataArray{:,4}; % center-of-mass Y in pixels
sessionFile.track.pressure = dataArray{:,6};
sessionFile.track.pointerID = dataArray{:,7}; % identity of touch point
sessionFile.track.relDistTarget = dataArray{:,11}; % relative distance to target zone, in cm
sessionFile.track.ts = dataArray{:,12}; % elapsed time from start, in sec
sessionFile.track.eventType = dataArray{:,8}; % behavioral state 
sessionFile.track.elapDistTot = dataArray{:,9}; % total elapsed distance, in cm
sessionFile.head.angle = dataArray{:,5}; % heading angle, in degrees
sessionFile.head.speed = dataArray{:,10}; % heading speed, in cm/sec
sessionFile.toneAmp = toneAmp; % amplitude of feedback tone
sessionFile.toneFreq = toneFreq; % frequency of feedback tone
sessionFile.toneDur = toneDur; % duration of feedback tone
sessionFile.toneFs = toneFs; % sampling frequency of feedback tone
sessionFile.tonePeriod = tonePeriod; % period of feedback tone

% Do post-track computions of behavioural read-outs here
save ([sessionFile.tag '.mat'],'sessionFile');
disp('sessionFile saved')

figure
plot(sessionFile.track.x,sessionFile.track.y,'.','MarkerSize',13)
print(gcf, '-dpdf', ['Navigation ' sessionFile.tag '.pdf']);

disp('Saving session file completed!')
