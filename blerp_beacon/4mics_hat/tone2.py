from time import sleep
import pyaudio
import matplotlib.pyplot as plt 
import numpy as np
import struct 
import sys
import threading
import atexit
from scipy.signal import butter, lfilter
from pixels import pixels
import collections

CHUNK = 1024
FORMAT = pyaudio.paInt16
CHANNELS = 4
RATE = 44100
RECORD_SECONDS = 1

SHORT_NORMALIZE = (1.0/32768.0)

LOW_CUT = 18950.0
HIGH_CUT = 19050.0
ORDER = 5



SOUND_SPEED = 340.0

MIC_DISTANCE_4 = 0.081
MAX_TDOA_4 = MIC_DISTANCE_4 / float(SOUND_SPEED)

def gcc_phat(sig, refsig, fs=1, max_tau=None, interp=1):
    '''
    This function computes the offset between the signal sig and the reference signal refsig
    using the Generalized Cross Correlation - Phase Transform (GCC-PHAT)method.
    '''
    
    # make sure the length for the FFT is larger or equal than len(sig) + len(refsig)
    n = sig.shape[0] + refsig.shape[0]

    # Generalized Cross Correlation Phasee Transform
    SIG = np.fft.rfft(sig, n=n)
    REFSIG = np.fft.rfft(refsig, n=n)
    R = SIG * np.conj(REFSIG)

    cc = np.fft.irfft(R / np.abs(R), n=(interp * n))

    max_shift = int(interp * n / 2)
    if max_tau:
        max_shift = np.minimum(int(interp * fs * max_tau), max_shift)

    cc = np.concatenate((cc[-max_shift:], cc[:max_shift+1]))

    # find max cross correlation index
    shift = np.argmax(np.abs(cc)) - max_shift

    tau = shift / float(interp * fs)
    
    return tau, cc

pair = [[0, 2], [1, 3]]

def get_direction(buf):
	tau = [0, 0]
	theta = [0, 0]

	#buf = b''.join(buf)
	#buf = np.fromstring(buf, dtype='int16')
	for i, v in enumerate(pair):
		tau[i], _ = gcc_phat(buf[v[0]::4], buf[v[1]::4], fs=RATE, max_tau=MAX_TDOA_4, interp=1)
		theta[i] = np.arcsin(tau[i] / MAX_TDOA_4) * 180 / np.pi

	if np.abs(theta[0]) < np.abs(theta[1]):
		if theta[1] > 0:
			best_guess = (theta[0] + 360) % 360
		else:
			best_guess = (180 - theta[0])
	else:
		if theta[0] < 0:
			best_guess = (theta[1] + 360) % 360
		else:
			best_guess = (180 - theta[1])

		best_guess = (best_guess + 270) % 360

	best_guess = (-best_guess + 120) % 360

	return best_guess

def butter_bandpass(lowcut, highcut, fs, order=5):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = butter(order, [low, high], btype='band')
    return b, a

def butter_bandpass_filter(data, lowcut, highcut, fs, order=5):
    b, a = butter_bandpass(lowcut, highcut, fs, order=order)
    y = lfilter(b, a, data)
    return y

def plot_4ch(ch1, ch2, ch3, ch4):
	f, (ax1, ax2, ax3, ax4) = plt.subplots(4, sharex=True, sharey=True)
	ax1.plot(ch1)
	ax2.plot(ch2)
	ax3.plot(ch3)
	ax4.plot(ch4)

	plt.show()	

def get_id(pa):
	for i in range(pa.get_device_count()):
		
		devinfo = pa.get_device_info_by_index(i)
		print("Device %d: %s" % (i, devinfo["name"]))

		for keyword in ["mic", "input"]:
			if keyword in devinfo["name"].lower():
				print( "Found an input: device %d - %s"% (i, devinfo["name"]) )
				device_index = i
				return i

# class taken from the SciPy 2015 Vispy talk opening example
# see https://github.com/vispy/vispy/pull/928
class MicrophoneRecorder(object):
	def __init__(self, rate=4000, chunksize=1024):
		self.rate = rate
		self.chunksize = chunksize
		self.p = pyaudio.PyAudio()
		self.stream = self.p.open(format=FORMAT,
								  channels=CHANNELS,
								  rate=self.rate,
								  input=True,
								  frames_per_buffer=self.chunksize,
								  input_device_index=get_id(self.p),
								  stream_callback=self.new_frame)
		self.lock = threading.Lock()
		self.stop = False
		self.frames = []
		atexit.register(self.close)

	def new_frame(self, data, frame_count, time_info, status):
		
		
		
		#data = np.fromstring(data, 'int16')
		with self.lock:
			self.frames.append(data)
			if self.stop:
				return None, pyaudio.paComplete
		return None, pyaudio.paContinue

	def get_frames(self):
		with self.lock:
			frames = self.frames
			self.frames = []
			return frames

	def start(self):
		self.stream.start_stream()

	def close(self):
		with self.lock:
			self.stop = True

		self.stream.close()
		self.p.terminate() 


def list_splice(S, step):
	return [S[i::step] for i in range(step)]

def main():
	
	mic = MicrophoneRecorder(RATE, 100 * CHUNK)
	mic.start()
		
	ch = mic.get_frames()
	
	while(1):
		ch = mic.get_frames()
		if(len(ch) > 0):
			
			t = range(0, len(ch))
			
			ch = ''.join(ch)
			ted = np.fromstring(ch, np.int16)
			
			ch = list_splice(ted, 4)
	
			ch1 = ch[:][0] # Bottom Left
			ch2 = ch[:][1] # Top Left
			ch3 = ch[:][2] # Top Right 
			ch4 = ch[:][3] # Bottom Right

			ch1 = butter_bandpass_filter(ch1, LOW_CUT, HIGH_CUT, RATE, ORDER)
			ch2 = butter_bandpass_filter(ch2, LOW_CUT, HIGH_CUT, RATE, ORDER)
			ch3 = butter_bandpass_filter(ch3, LOW_CUT, HIGH_CUT, RATE, ORDER)
			ch4 = butter_bandpass_filter(ch4, LOW_CUT, HIGH_CUT, RATE, ORDER)					

			temp = []
			for i in range(0, len(ch1)):
				temp.append(ch1[i])
				temp.append(ch2[i])
				temp.append(ch3[i])								
				temp.append(ch4[i])

			direction = get_direction(np.array(temp))
			pixels.wakeup(direction)

			a1 = np.average((np.absolute(ch1)))
			a2 = np.average((np.absolute(ch2)))
			a3 = np.average((np.absolute(ch3)))
			a4 = np.average((np.absolute(ch4)))
			
			if(a1 > a2 and a1 > a3 and a1 > a4):
				print("Yes")
			else:
				print("No")
							
			print("Bottom Left: %f" % a1)
			print("Top Left: %f" % a2)
			print("Top Right : %f" % a3)
			print("Bottom Right: %f" % a4)
						
			#plot_4ch(ch1, ch2, ch3, ch4)
		else:
			pass
		
	mic.close()
	
main() 



