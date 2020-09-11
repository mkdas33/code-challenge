package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is a dummy class and not fit for production. This class is introduced to segregate the transaction layer from message notification layer.
 * In production this class implementation should replace with a Message broker (Kafka, ActiveMQ)
 */

@Slf4j
public class TransactionServiceHelper {

    private static final LinkedBlockingQueue<NotificationMessage> notificationQueue = new LinkedBlockingQueue<>();
    private final NotificationService notificationService;

    public TransactionServiceHelper() {
        notificationService = new EmailNotificationService();
        startService();
    }

    private void startService() {
        Runnable runnable = this::startPublisher;
        new Thread(runnable).start();
    }
    private void startPublisher() {
        while (true){
            try {
                NotificationMessage msg = notificationQueue.take();
                notificationService.notifyAboutTransfer(msg.getAccount(), msg.getMessage());
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void notifyUser(Account account, String message) {
        addMessageToQueue(new NotificationMessage(account, message));
    }

    public void addMessageToQueue(NotificationMessage message){
        try {
            notificationQueue.put(message);
        } catch (InterruptedException e) {
            log.error("Error in sending message {}", e.getMessage());
        }
    }

    public static class NotificationMessage{
        private final Account account;
        private final String message;

        public NotificationMessage(Account account, String message) {
            this.account = account;
            this.message = message;
        }

        public Account getAccount() {
            return account;
        }

        public String getMessage() {
            return message;
        }
    }
}
