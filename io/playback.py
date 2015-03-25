import pika
import json
import serial
import logging
import time
import threading
from functools import partial

log = logging.getLogger("playback")
log.setLevel(logging.DEBUG)

fh = logging.FileHandler("log/playback.log")
fh.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)

ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
formatter = logging.Formatter('%(name)-10s: %(levelname)-8s %(message)s')
ch.setFormatter(formatter)

log.addHandler(fh)
log.addHandler(ch)


class Playback(object):
  def __init__(self, exchange, tty, debug):
    # if not debug:
    # else:
    #   self.serial = None

    self.serial = serial.Serial(tty, 9600)
    self.debug = debug
    self.connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
    self.channel = self.connection.channel()

    self.channel.exchange_declare(exchange=exchange, type='direct')
    self.playback = self.channel.queue_declare(exclusive=True)
    self.playback_queue_name = self.playback.method.queue
    self.channel.queue_bind(exchange=exchange,
                            routing_key="playback",
                            queue=self.playback_queue_name)
    self.channel.basic_consume(self.playback_callback, queue=self.playback_queue_name, no_ack=True)
    self.start_time = time.time()
    self.data_points = 1
    self.sent_points = 1
    self.last_value = 0

  def start(self):
    log.info("Starting Playback Capture Thread")
    self.thread = threading.Thread(target=self.read_from_serial)
    self.thread.daemon = True
    self.thread.start()
    log.info("Playback Capture Thread Up and Running")
    log.info("Subscribing to RabbitMQ Channel")
    self.channel.start_consuming()

  def stop(self):
    self.channel.stop_consuming()
    log.info("Subscription Shutdown")

  def read_from_serial(self):
    print "hey!"
    log.info("Waiting for Serial Port to Clear Out")
    while True:
        incoming_data = self.serial.readline()
        log.debug(incoming_data)

  def playback_callback(self, ch, method, properties, body):
    decoded = json.loads(body)
    pressure = decoded['pressure']
    self.data_points += 1
    # if not self.debug:
    # if(pressure > 0.5):
    if self.data_points % 10 == 0:
      log.info("throughput: %f", (time.time() - self.start_time) / self.sent_points)
      log.info("pressure: %f", pressure)
      self.sent_points += 1

      if abs(self.last_value - pressure) > 0.5:
        log.info("Writing to pressure")
        self.serial.write("{0:.2f}".format(pressure));
        self.serial.flush();

      self.last_value = pressure
