import pika
import json
import serial
from functools import partial

class Playback(object):
  def __init__(self, exchange, tty):
    self.serial = serial.Serial(tty, 115200)
    self.connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
    self.channel = self.connection.channel()
    # channel.queue_declare(queue='glove')
    self.channel.exchange_declare(exchange=exchange, type='direct')
    self.playback = self.channel.queue_declare(exclusive=True)
    self.playback_queue_name = self.playback.method.queue
    self.glove = self.channel.queue_declare(exclusive=True)
    self.glove_queue_name = self.glove.method.queue
    self.channel.queue_bind(exchange=exchange,
                            routing_key="playback",
                            queue=self.playback_queue_name)
    self.channel.queue_bind(exchange=exchange,
                       routing_key="glove",
                       queue=self.glove_queue_name)
    self.channel.basic_consume(partial(playback_callback, ser=self.serial), queue=self.playback_queue_name, no_ack=True)


  def start(self):
    self.channel.start_consuming()


  def stop(self):
    self.channel.stop_consuming()


  def playback_callback(self, ch, method, properties, body):
    decoded = json.loads(body)
    pressure = int(body['pressure'])
    print " [x] %r:%r" % (method.routing_key, pressure)
    self.serial.write(str(pressure));