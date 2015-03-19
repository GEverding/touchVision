import sys, getopt
import time
import logging
from glove import Glove
from playback import Playback

# set up logging to file - see previous section for more details
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(name)-12s %(levelname)-8s %(message)s',
                    datefmt='%m-%d %H:%M',
                    filename='log/touchvision.log',
                    filemode='w')
logger = logging.getLogger("")
logger.setLevel(logging.INFO)
console = logging.StreamHandler()
console.setLevel(logging.INFO)
# set a format which is simpler for console use
formatter = logging.Formatter('%(name)-10s: %(levelname)-8s %(message)s')
# tell the handler to use this format
console.setFormatter(formatter)
logger.addHandler(console)

def main(argv):
    ex = "touchvision"
    glove = None
    glove_tty = ""
    playback = None
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
            glove = Glove(ex, glove_tty, debug)
        elif opt in ("-p", "--playback"):
            playback_tty = arg
            playback = Playback(ex, playback_tty, debug)
    try:
        if glove:
            logger.info("Preparing to Start Glove..")
            glove.start()
            logger.info("Started Glove")
        if playback:
            logger.info("Preparing to Start Playback...")
            playback.start();
            logger.info("Playback Started")
        while True:
            time.sleep(.3)

    except KeyboardInterrupt:
        logger.info("Preparing to stop touchVision")
        if glove: glove.stop()
        if playback: playback.stop()
        logger.info("TouchVision Stopped")

if __name__ == '__main__':
    main(sys.argv[1:])
