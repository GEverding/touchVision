import sys, getopt
import time
from glove import Glove
from playback import Playback

def main(argv):
    ex = "touchvision"
    glove_tty = ""
    playback_tty = ""
    debug = False
    try:
        opts, args = getopt.getopt(argv,"hdg:p:",["glove=","playback="])
    except getopt.GetoptError:
        print 'main.py -d -v <glove tty> -p <playback tty>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print 'main.py -d -v <glove tty> -p <playback tty>'
            sys.exit()
        elif opt == '-d':
            debug = True
        elif opt in ("-g", "--glove"):
            glove_tty = arg
        elif opt in ("-p", "--playback"):
            playback_tty = arg
    glove = Glove(ex, glove_tty, debug)
    # playback = Playback(ex, playback_tty)
    try:
        glove.start()
        # playback.start();
        while True:
            time.sleep(.3)


    except KeyboardInterrupt:
        print "Stopping"
        glove.stop()
        # playback.stop()

if __name__ == '__main__':
    main(sys.argv[1:])
