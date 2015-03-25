import serial
import pika
import json
import threading
import thread
import time
import logging
from cleaner.cleaner import KinematicsSolver
from functools import partial
import cleaner.constants as const
from random import uniform, randint, randrange

log = logging.getLogger("glove")
log.setLevel(logging.DEBUG)
fh = logging.FileHandler("log/glove.log")
fh.setLevel(logging.DEBUG)
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(name)-10s: %(levelname)-8s %(message)s')
ch.setFormatter(formatter)

log.addHandler(fh)
log.addHandler(ch)

def read_from_serial(s, connection, channel, kinematics, debug):
    if not debug:
        i = 1
        log.info("Waiting for Serial Port to Clear Out")
        while True:
            incoming_data = s.readline()
            log.debug(incoming_data)
            if "DMP ready! Waiting for first interrupt..." in incoming_data:
                break

        log.info("Serial Port Cleared out")
        start = time.time()
        data_points = 0
        while True:
            stdin = s.readline()
            packet = stdin.split(const.PACKET_DELIMINATOR)
            if i % 10 == 0:
                for sub_packet in packet:
                    # print(sub_packet)
                    data = sub_packet.split(const.SUB_PACKET_DELIMINATOR)
                    kinematics.process_acceleration_sample([float(data[0]), float(data[1]), float(data[2])], float(data[3]), float(data[4]))
                    [t, x, y, z, pressure] = kinematics.get_latest_measurements()

                    payload = {}
                    payload['timestamp'] = int(time.time()*1000)
                    payload['x'] = x
                    payload['y'] = y
                    payload['z'] = z
                    payload['pressure'] = min(5, pressure)
                    p = json.dumps(payload, sort_keys=True);
                    log.debug(p)

                    data_points += 1
                    throughput = (time.time() - start) / data_points
                    log.debug("Throughput: %f",throughput)

                    channel.basic_publish(exchange='touchvision', routing_key='glove', body=p)
            i += 1
    else:
        t = 0

        start = time.time()
        data_points = 0
        while True:
            try:
                payload = {}
                payload['timestamp'] = int(time.time() * 1000)
                payload['x'] = int(uniform(0,100))
                payload['y'] = int(uniform(0,100))
                payload['z'] = int(uniform(0,100))
                payload['pressure'] = int(uniform(0,5))
                p = json.dumps(payload, sort_keys=True);
                log.debug(p)

                data_points += 1
                throughput = (time.time() - start) / data_points
                log.debug("Throughput: %d", throughput)

                channel.basic_publish(exchange='touchvision', routing_key='glove', body=p)
                t += 1
                time.sleep(.1)

            except Exception as e:
                print e


class Glove(object):
    def __init__(self, exchange, tty, debug):
        if not debug:
            log.info("Glove in Live Mode")
            self.serial = serial.Serial(tty, 115200)
        else:

            log.info("Glove in Debug Mode")
            self.serial = None

        self.debug = debug
        self.kinematics = KinematicsSolver(1)
        self.connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
        self.channel = self.connection.channel()
        # channel.queue_declare(queue='glove')
        self.channel.exchange_declare(exchange=exchange, type='direct')
        self.glove = self.channel.queue_declare(exclusive=True)
        self.glove_queue_name = self.glove.method.queue

        self.channel.queue_bind(exchange=exchange,
                        routing_key="glove-in",
                        queue=self.glove_queue_name)

        self.channel.basic_consume(self.glove_callback, queue=self.glove_queue_name, no_ack=True)
        # self.channel.basic_consume(partial(self.glove_callback, kin=self.kinematics), queue=self.glove_queue_name, no_ack=True)

    def start(self):
        log.info("Starting Glove Thread")
        self.thread = threading.Thread(target=read_from_serial, args=(self.serial, self.connection, self.channel, self.kinematics, self.debug))
        self.thread.daemon = True
        self.thread.start()
        log.info("Glove Up and Running")

    def stop(self):
        self.thread.join(0)
        self.connection.close()

    def glove_callback(self, ch, method, properties, body):
        log.warn("Resetting Kinematics...")
        self.kinematics.reset()
        log.info("Kinematics Reset Complete")
