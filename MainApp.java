package com.google.firebase.quickstart.fcm.java;
import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.edge.consent.Consent;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.adobe.marketing.mobile.optimize.Optimize;
import com.adobe.marketing.mobile.Analytics;
import com.adobe.marketing.mobile.optimize.*;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainApp extends Application {

    private static final String ENVIRONMENT_FILE_ID = "4ef014d90ea4/f89fd1d4741f/launch-fa15eb340083-development";
    private static final String LOG_TAG = "MainApp";

    @Override
    public void onCreate() {
        super.onCreate();
        MobileCore.setApplication(this);
        //MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);
        try {
            Messaging.registerExtension();
            Identity.registerExtension();
            Analytics.registerExtension();
            Edge.registerExtension();

           // com.adobe.marketing.mobile.Identity.registerExtension();
           // com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

            Lifecycle.registerExtension();
            Optimize.registerExtension();
            Consent.registerExtension();
            Assurance.registerExtension();
            MobileCore.start(new AdobeCallback() {
                @Override
                public void call(final Object o) {
                    // processing after start
                    MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);
        //        final DecisionScope decisionScope = DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 3);

                }
            });
        } catch (Exception e) {
            // handle the exception
        }


        try {
            Identity.getExperienceCloudId(new AdobeCallback<String>() {
                @Override
                public void call(final String ecid) {

                    //  String text = String.format("Messaging SDK setup is complete with ECID - %s. \n\nFor more details please take a look at the documentation in the github repository.", ecid);
                    Log.i("ECID", ecid);


                }
            });
        }
        catch(IllegalArgumentException e){
            Log.e("ECID", "IllegalArgumentException - Identity service error. \nError message: " + e.getLocalizedMessage());


        }

        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (!task.isSuccessful()) {
                                Log.w(LOG_TAG, "Fetching FCM registration token failed", task.getException());
                                return;
                            }

                            // Get new FCM registration token
                            String token = task.getResult();
                                     Log.i( LOG_TAG,"FCM token: "+token);
                            MobileCore.setPushIdentifier(token);
                        }
                    });
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "IllegalArgumentException - Check if google-services.json is added and is correctly configured. \nError message: " + e.getLocalizedMessage());
        }

        // Offer code



    }
}
