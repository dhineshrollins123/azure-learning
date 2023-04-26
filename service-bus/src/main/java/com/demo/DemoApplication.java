package main.java.com.demo;

/*import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}*/

import com.azure.messaging.servicebus.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;
public class DemoApplication {
	static String connectionString = "Endpoint=sb://servicebusdhinesh.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Zm8Dyc5eWF68bj9P0o5f+IYyv0kTfScu0+ASbCZtBZg=";
	static String queueName = "dkqueue";

	public static void main(String[] args) throws InterruptedException {
		//sendMessage();
	//	sendMessageBatch();
		receiveMessages();
	}


	static void sendMessage() {
		// create a Service Bus Sender client for the queue
		ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.sender()
				.queueName(queueName)
				.buildClient();

		// send one message to the queue
		senderClient.sendMessage(new ServiceBusMessage("Hello, World from dk!"));
		System.out.println("Sent a single message to the queue: " + queueName);
	}

	static List<ServiceBusMessage> createMessages() {
		// create a list of messages and return it to the caller
		ServiceBusMessage[] messages = {
				new ServiceBusMessage("First message from dhinesh"),
				new ServiceBusMessage("Second message"),
				new ServiceBusMessage("Third message")
		};
		return Arrays.asList(messages);
	}

	static void sendMessageBatch() {
		// create a Service Bus Sender client for the queue
		ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.sender()
				.queueName(queueName)
				.buildClient();

		// Creates an ServiceBusMessageBatch where the ServiceBus.
		ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();

		// create a list of messages
		List<ServiceBusMessage> listOfMessages = createMessages();

		// We try to add as many messages as a batch can fit based on the maximum size and send to Service Bus when
		// the batch can hold no more messages. Create a new batch for next set of messages and repeat until all
		// messages are sent.
		for (ServiceBusMessage message : listOfMessages) {
			if (messageBatch.tryAddMessage(message)) {
				continue;
			}

			// The batch is full, so we create a new batch and send the batch.
			senderClient.sendMessages(messageBatch);
			System.out.println("Sent a batch of messages to the queue: " + queueName);

			// create a new batch
			messageBatch = senderClient.createMessageBatch();

			// Add that message that we couldn't before.
			if (!messageBatch.tryAddMessage(message)) {
				System.err.printf("Message is too large for an empty batch. Skipping. Max size: %s.", messageBatch.getMaxSizeInBytes());
			}
		}

		if (messageBatch.getCount() > 0) {
			senderClient.sendMessages(messageBatch);
			System.out.println("Sent a batch of messages to the queue: " + queueName);
		}

		//close the client
		senderClient.close();
	}

	private static void processMessage(ServiceBusReceivedMessageContext context) {
		ServiceBusReceivedMessage message = context.getMessage();
		System.out.printf("Processing message. Session: %s, Sequence #: %s. Contents: %s%n", message.getMessageId(),
				message.getSequenceNumber(), message.getBody());
	}
	private static void processError(ServiceBusErrorContext context, CountDownLatch countdownLatch) {
		System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
				context.getFullyQualifiedNamespace(), context.getEntityPath());

		if (!(context.getException() instanceof ServiceBusException)) {
			System.out.printf("Non-ServiceBusException occurred: %s%n", context.getException());
			return;
		}

		ServiceBusException exception = (ServiceBusException) context.getException();
		ServiceBusFailureReason reason = exception.getReason();

		if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
				|| reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
				|| reason == ServiceBusFailureReason.UNAUTHORIZED) {
			System.out.printf("An unrecoverable error occurred. Stopping processing with reason %s: %s%n",
					reason, exception.getMessage());

			countdownLatch.countDown();
		} else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
			System.out.printf("Message lock lost for message: %s%n", context.getException());
		} else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
			try {
				// Choosing an arbitrary amount of time to wait until trying again.
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				System.err.println("Unable to sleep for period of time");
			}
		} else {
			System.out.printf("Error source %s, reason %s, message: %s%n", context.getErrorSource(),
					reason, context.getException());
		}
	}

	// handles received messages
	static void receiveMessages() throws InterruptedException
	{
		CountDownLatch countdownLatch = new CountDownLatch(1);

		// Create an instance of the processor through the ServiceBusClientBuilder
		ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.processor()
				.queueName(queueName)
				.processMessage(DemoApplication::processMessage)
				.processError(context -> processError(context, countdownLatch))
				.buildProcessorClient();

		System.out.println("Starting the processor");
		processorClient.start();

		TimeUnit.SECONDS.sleep(10);
		System.out.println("Stopping and closing the processor");
		processorClient.close();
	}
}