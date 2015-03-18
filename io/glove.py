import serial
import pika
import json
import threading
import thread
import time
import signal
from cleaner.cleaner import KinematicsSolver
from functools import partial
import cleaner.constants as const
from random import uniform, randint, randrange

def read_from_serial(s, connection, channel, kinematics, debug):

    if not debug:
        i = 1
        while True:
            incoming_data = s.readline()
            print(incoming_data)
            if "DMP ready! Waiting for first interrupt..." in incoming_data:
                break

        start = time.time()
        data_points = 0
        while True:
            stdin = s.readline()
            packet = stdin.split(const.PACKET_DELIMINATOR)
            if i % 5 == 0:
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
                    print p

                    data_points += 1
                    throughput = (time.time() - start) / data_points
                    print throughput

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
                print p

                data_points += 1
                throughput = (time.time() - start) / data_points
                print throughput

                channel.basic_publish(exchange='touchvision', routing_key='glove', body=p)
                t += 1
                time.sleep(.1)

            except Exception as e:
                print e


class Glove(object):
    def __init__(self, exchange, tty, debug):
        if not debug:
            self.serial = serial.Serial(tty, 115200)
        else:
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
        print "starting thread"
        self.thread = threading.Thread(target=read_from_serial, args=(self.serial, self.connection, self.channel, self.kinematics, self.debug))
        self.thread.daemon = True
        self.thread.start()

    def stop(self):
        self.thread.join(0)
        self.connection.close()

    def glove_callback(self, ch, method, properties, body):
        print "glove in"
        self.kinematics.reset()
        print "reset"
