import serial
import pika
import json
import random
import time as t

# ser = serial.Serial('/dev/ttyACM0', 9600)

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()
channel.exchange_declare(exchange='touchvision',
                         type='direct')
data = {}

for j in range(1000):
  for i in range(1000):
    pressure = random.randint(0, 5)
    x = random.uniform(0, 100)
    y = random.uniform(0, 100)
    z = random.uniform(0, 100)
    time = random.uniform(i, i+100)
    data = {
        'x': x,
        'y': y,
        'z': z,
        'timestamp': time,
        'pressure': pressure
        }
    print data
    channel.basic_publish(exchange='touchvision',
        routing_key='glove',
        body=json.dumps(data))
    t.sleep(5)

connection.close()
