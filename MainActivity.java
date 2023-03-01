/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.quickstart.fcm.java;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Spinner;
import android.widget.Toast;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.EdgeCallback;
import com.adobe.marketing.mobile.EdgeEventHandle;
import com.adobe.marketing.mobile.ExperienceEvent;
import com.adobe.marketing.mobile.optimize.DecisionScope;
import com.adobe.marketing.mobile.optimize.Offer;
import com.adobe.marketing.mobile.optimize.Optimize;
import com.adobe.marketing.mobile.optimize.Proposition;
import com.google.firebase.quickstart.fcm.xdm.Commerce;
import com.google.firebase.quickstart.fcm.xdm.IdentityMap;
import com.google.firebase.quickstart.fcm.xdm.Items;
import com.google.firebase.quickstart.fcm.xdm.MobileSDKCommerceSchema;
import com.google.firebase.quickstart.fcm.xdm.Order;
import com.google.firebase.quickstart.fcm.xdm.PaymentsItem;
import com.google.firebase.quickstart.fcm.xdm.ProductListAdds;
import com.google.firebase.quickstart.fcm.xdm.ProductListItemsItem;
import com.google.firebase.quickstart.fcm.xdm.Purchases;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.edge.consent.Consent;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.quickstart.fcm.R;
import androidx.fragment.app.FragmentActivity;
import com.google.firebase.quickstart.fcm.databinding.ActivityMainBinding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.webkit.WebViewClient;
import android.webkit.WebView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this, "FCM can't post notifications without POST_NOTIFICATIONS permission",
                            Toast.LENGTH_LONG).show();
                }
            });


    @Override
    public void onPause() {
        super.onPause();
        MobileCore.lifecyclePause();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobileCore.setApplication(getApplication());
        MobileCore.lifecycleStart(null);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        WebView myWebView = (WebView) findViewById(R.id.offer_view);
        myWebView.setWebViewClient(new Callback());
        myWebView.loadUrl("https://picsum.photos/200/300");

        Map<String, String> additionalContextData = new HashMap<String, String>();
        additionalContextData.put("customKey", "value");
        MobileCore.trackAction("viewedHomeScreen", additionalContextData);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]


        binding.logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // show ecid

                //  final Activity activity = getActivity();

                // if (activity == null) return;


                // Get token
                // [START log_reg_token]
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                                    return;
                                }

                                // Get new FCM registration token
                                String token = task.getResult();

                                // Log and toast
                                String msg = getString(R.string.msg_token_fmt, token);
                                Log.d(TAG, msg);
                                MobileCore.setPushIdentifier(token);

                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                // [END log_reg_token]
            }
        });


        binding.inappMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Messaging.refreshInAppMessages();



            }
        });



        askNotificationPermission();

        getConsent();

        binding.updateConsentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectConsentUpdate("y");
                getConsent();


            }
        });

        binding.updateCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendAddToCartXdmEvent();

            }
        });

        binding.trackAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                trackAnalyticsAction();

            }
        });


        binding.getOffersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getOffers();

            }
        });
    }

    private void askNotificationPermission() {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }


    private void trackAnalyticsAction() {
        Map<String, String> additionalContextData = new HashMap<String, String>();
        additionalContextData.put("button", "trackAction");
        MobileCore.trackAction("clickTrackAction", additionalContextData);

        Map<String, String> additionalContextDataS = new HashMap<String, String>();
        additionalContextDataS.put("eVar10", "mobileAppDemo");
        MobileCore.trackState("demoAppHomeScreen", additionalContextDataS);

        Analytics.sendQueuedHits();
    }


    private void collectConsentUpdate(final String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        Consent.update(new HashMap<String, Object>() {
            {
                put("consents", new HashMap<String, Object>() {
                    {
                        put("collect", new HashMap<String, Object>() {
                            {
                                put("val", value);
                            }
                        });
                        put("marketing", new HashMap<String, Object>() {
                            {
                                put("preferred", "push");
                                put("push", new HashMap<String, Object>() {
                                    {

                                        put("val", value);
                                    }
                                });

                            }
                        });
                    }
                });
            }
        });
    }

    private void getConsent() {
        Consent.getConsents(new AdobeCallback<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> map) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(map);
                Log.i(TAG, String.format("Received Consent from API = %s", json));

            }
        });
    }


    private void getOffers() {

        final DecisionScope decisionScope1 = new DecisionScope("xcore:offer-activity:161b9cd514e0cc2c", "xcore:offer-placement:15f4ff83bc702df3", 1);
        // final DecisionScope decisionScope2 = new DecisionScope("myScope");



        final List<DecisionScope> decisionScopes = new ArrayList<>();
        decisionScopes.add(decisionScope1);


        Optimize.updatePropositions(decisionScopes,null,null);


        Optimize.getPropositions(decisionScopes, new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {


            @Override
            public void fail(final AdobeError adobeError) {
                // handle error
                Log.i(TAG, "Offer Error: " + adobeError);
            }

            @Override
            public void call(Map<DecisionScope, Proposition> propositionsMap) {
                Log.i(TAG, "Proposition call response: " + propositionsMap);

                if (propositionsMap != null && !propositionsMap.isEmpty()) {
                    // get the propositions for the given decision scopes
                    final Proposition proposition1 = propositionsMap.get(decisionScope1);

                    Log.i(TAG, "Offer received: " + proposition1);
                    List<Offer>  offers = proposition1.getOffers();
                    Log.i(TAG, "offers");
                    String content = offers.get(0).getContent();

                    String unencodedHtml =  "<html><body>" + content +"</body></html>";
                  //  String encodedHtml = Base64.encodeToString(unencodedHtml.getBytes(), android.util.Base64.DEFAULT);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            WebView myWebView = (WebView) findViewById(R.id.offer_view);
                            myWebView.setWebViewClient(new Callback());
                            //  myWebView.loadData(encodedHtml, "text/html; charset=utf-8", "base64");
                            myWebView.loadDataWithBaseURL(null, unencodedHtml, "text/html", "utf-8", null);
                        }
                    });

                    /*
                    if (propositionsMap.contains(decisionScope1)) {
                        final Proposition proposition1 = propsMap.get(decisionScope1);
                        // read proposition1 offers
                    }*/

                }
            }
        });

    }

    private class Callback extends WebViewClient{  //HERE IS THE MAIN CHANGE.

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return (false);
        }

    }

    private void sendAddToCartXdmEvent() {

        /// Create list with the added items
        final ProductListItemsItem product1 = new ProductListItemsItem();
        product1.setName("test product");
        product1.setPriceTotal(100);
        product1.setSKU("TSTPRD001");
        product1.setQuantity(1);
        product1.setCurrencyCode("AUD");

        List<ProductListItemsItem> productItemsList = new ArrayList<ProductListItemsItem>() {{
            add(product1);
        }};

        ProductListAdds productListAdds = new ProductListAdds();
        productListAdds.setValue(1);

        /// Create Commerce object and add ProductListAdds details
        Commerce commerce = new Commerce();
        commerce.setProductListAdds(productListAdds);


        // xdmData.put("identityMap", identityMap);

        // Compose the XDM Schema object and set the event name
        MobileSDKCommerceSchema xdmData = new MobileSDKCommerceSchema();
        xdmData.setEventType("commerce.productListAdds");
        xdmData.setCommerce(commerce);
        xdmData.setProductListItems(productItemsList);

       /* IdentityMap imp = new IdentityMap();
        Items impItems = new Items();
        impItems.setId("sambhavjain3@yahoo.co.in");
        impItems.setPrimary(false);
        impItems.setAuthenticatedState(AUTHENTICATED);
        imp.setItems( impItems);
        xdmData.setIdentityMap(imp);*/

        // Create an Experience Event with the built schema and send it using the AEP Edge extension
        ExperienceEvent event = new ExperienceEvent.Builder()
                .setXdmSchema(xdmData)
                .build();
        Edge.sendEvent(event, new EdgeCallback() {
            @Override
            public void onComplete(List<EdgeEventHandle> list) {
                Log.d("Send XDM Event", String.format("Received response for event 'commerce.productListAdds': %s", list));
            }
        });
    }

}
