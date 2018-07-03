# --- okse ---
| Protocol 	| Port 		|
| --------- | --------: |
|AMQP 0.9.1	|	56720	|
|AMQP 1.0	|	5672	|
|WSN		|	61000	|
|STOMP 		|	61613	|
|MQTT		|	1883 	|
|MQTT-SN	|	20000 	|
|XMPP		|	5222 	|

# --- rabbitmq default ---
| Protocol 			| Port 		|
| ----------------- | --------: |
|epmd				|	4369	|
|AMQP0.9.1/1.0 		|	5672	(conflict)	|
|erlang dist		|	25672	|
|rabbitmq manage	|	15672	|
|STOMP 				|	61613	(conflict) 	|
|MQTT 				|	1883	(conflict)	|

# --- rabbitmq okse ---
| Protocol 			| Port 		|
| -----------------	| --------: |
|AMQP 0.9.1/1.0		|	20001	|
|MQTT				|	20002	|
|STOMP 				|	20003	|
|rabbitmq manage 	|	20004	|
|epmd 				|	4369	|
|erlang dist 		| 	25672	| 

see [the rabbitmq documentation](https://rabbitmq.docs.pivotal.io/36/rabbit-web-docs/plugins.html) for info on how to connect with rabbitmq using different protocols.