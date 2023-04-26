package com.queues.quickstart;

import com.azure.identity.*;
import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import java.io.*;
import java.time.Duration;

/**
 * Hello world!
 *
 */
public class App 
{
    static String AZURE_STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=dkstorageaccountaz204;AccountKey=j5vSztVLrcMLmC/gwIT467wJg7+qpw6I5DTJ2cZ9MVLQTHszZf06IWp4WuL6v8Ad/Q3AfY1/TcLk+AStjv6LZw==;EndpointSuffix=core.windows.net";
    public static void main( String[] args )
    {
        // Create a unique name for the queue
        String queueName = "dkquickstartqueue1";

        System.out.println("Creating queue: " + queueName);

// Instantiate a QueueClient
// We'll use this client object to create and interact with the queue
        QueueClient queueClient = new QueueClientBuilder()
                .connectionString(AZURE_STORAGE_CONNECTION_STRING)
                .queueName(queueName)
                .buildClient();

       queueClient.create();




// Send several messages to the queue
        queueClient.sendMessage("First message");
        queueClient.sendMessage("Second message");
        queueClient.sendMessage("Hai, hello from dhinesh");

// Save the result so we can update this message later
        SendMessageResult result = queueClient.sendMessage("Third message");


        queueClient.sendMessage("last message");



        System.out.println("\nAdding messages to the queue...");

        System.out.println("\nPeek at the messages in the queue...");

// Peek at messages in the queue
        queueClient.peekMessages(3, null, null).forEach(
                peekedMessage -> System.out.println("Message: " + peekedMessage.getMessageText()));

        queueClient.updateMessage(result.getMessageId(),
                result.getPopReceipt(),
                "Third message has been updated by dk",
                Duration.ofSeconds(1));


        // Get messages from the queue
        queueClient.receiveMessages(10).forEach(
                // "Process" the message
                receivedMessage -> {
                    System.out.println("Message: " + receivedMessage.getMessageText());

                    // Let the service know we're finished with
                    // the message and it can be safely deleted.
                    queueClient.deleteMessage(receivedMessage.getMessageId(), receivedMessage.getPopReceipt());
                }
        );


        // Clean up
        System.out.println("Deleting queue: " + queueClient.getQueueName());
        queueClient.delete();

        System.out.println("Done");
    }
}
