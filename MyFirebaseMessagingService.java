/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.quickstart.fcm.java;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import java.util.concurrent.Future;

import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MessagingPushPayload;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.quickstart.fcm.R;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Map;
import java.util.List;
import java.util.Random;
import com.bumptech.glide.Glide;
import java.util.concurrent.ExecutionException;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 *
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 *
 * <intent-filter>
 *   <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private final String CHANNEL_ID = "messaging_notification_channel";
    private final String CHANNEL_NAME = "Messaging Notifications Channel";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(  RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        sendNotification(remoteMessage);

        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());



        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    // [START on_new_token]
    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);

    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance(this).beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any
     * server-side account maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(RemoteMessage remoteMessage) {
        // Use the MessagingPushPayload object to extract the payload attributes for creating notification
        MessagingPushPayload payload = new MessagingPushPayload(remoteMessage);
        // Setting the channel
        String channelId = payload.getChannelId() == null ? CHANNEL_ID : payload.getChannelId();

        // Understanding whats the importance from priority
        int importance = getImportanceFromPriority(payload.getNotificationPriority());

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, CHANNEL_NAME, importance);
            notificationManager.createNotificationChannel(channel);
        }

        Map<String, String> data = remoteMessage.getData();

        String title = payload.getTitle();
        String body = payload.getBody();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("TAB",  5); // 5 == Messaging tab
        intent.putExtra("From_Push", true);

        Messaging.addPushTrackingDetails(intent, remoteMessage.getMessageId(), data);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, PendingIntent.FLAG_MUTABLE);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder;
        notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        String url = payload.getImageUrl();
        if (url!= null && !url.isEmpty()) {
            Future<Bitmap> bitmapTarget = Glide.with(this).asBitmap().load(url).submit();
            Bitmap image;
            try {
                image = bitmapTarget.get();
                notificationBuilder.setLargeIcon(image).setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon(null));
            } catch (ExecutionException | InterruptedException e) {
                Log.d("MyFirebaseService", e.getMessage());
            }
        }

        if (payload.getActionButtons() != null) {
            List<MessagingPushPayload.ActionButton> buttons = payload.getActionButtons();
            for (int i = 0; i< buttons.size(); i++) {
                MessagingPushPayload.ActionButton obj = buttons.get(i);
                String buttonName = obj.getLabel();
                notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_assurance_active, buttonName, null));
            }
        }

        notificationManager.notify(new Random().nextInt(100), notificationBuilder.build());
    }

    private int getImportanceFromPriority(int priority) {
        switch (priority) {
            case NotificationCompat.PRIORITY_DEFAULT:
                return NotificationManager.IMPORTANCE_DEFAULT;
            case NotificationCompat.PRIORITY_MIN:
                return NotificationManager.IMPORTANCE_MIN;
            case NotificationCompat.PRIORITY_LOW:
                return NotificationManager.IMPORTANCE_LOW;
            case NotificationCompat.PRIORITY_HIGH:
                return NotificationManager.IMPORTANCE_HIGH;
            case NotificationCompat.PRIORITY_MAX:
                return NotificationManager.IMPORTANCE_MAX;
            default: return NotificationManager.IMPORTANCE_NONE;
        }
    }
}
