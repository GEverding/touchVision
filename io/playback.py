import pika
import json
import serial
import logging
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
    if not debug:
      self.serial = serial.Serial(tty, 9600)
    else:
      self.serial = None

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

  def start(self):
    log.info("Subscribing to RabbitMQ Channel")
    self.channel.start_consuming()

  def stop(self):
    self.channel.stop_consuming()
    log.info("Subscription Shutdown")


  def playback_callback(self, ch, method, properties, body):
    decoded = json.loads(body)
    pressure = decoded['pressure']
    log.info("Pressure Value Received: %d", pressure)
    if not self.debug:
      log.info("Writing Pressure Value to Serial Port")
      self.serial.write(str(pressure));
