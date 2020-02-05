package com.rudderlabs.android.integration.intercom;

import android.text.TextUtils;

import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;
import com.rudderstack.android.sdk.core.RudderProperty;
import com.rudderstack.android.sdk.core.RudderTraits;

import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.UserAttributes;
import io.intercom.android.sdk.Intercom.Visibility;
import io.intercom.android.sdk.identity.Registration;
import io.intercom.android.sdk.Company;

public class IntercomIntegrationFactory extends RudderIntegration<Intercom> {

    private static final String INTERCOM_KEY = "Intercom";
    private Map<String, String> eventMap = new HashMap<>();

    public static Factory FACTORY = new Factory() {
        @Override
        public RudderIntegration<?> create(Object settings, RudderClient client, RudderConfig rudderConfig) {
            return new IntercomIntegrationFactory(settings, client, rudderConfig);
        }

        @Override
        public String key() {
            return INTERCOM_KEY;
        }
    };

    private IntercomIntegrationFactory(Object config, final RudderClient client, RudderConfig rudderConfig) {

        String mobileApiKey = "";
        Map<String, Object> destinationConfig = (Map<String, Object>) config;
        if (destinationConfig != null && destinationConfig.containsKey("mobileApiKey")) {
            mobileApiKey = (String) destinationConfig.get("mobileApiKey");
        }
        String appId = "";
        if (destinationConfig != null && destinationConfig.containsKey("appId")) {
            appId = (String) destinationConfig.get("appId");
        }

        if(TextUtils.isEmpty(mobileApiKey) && TextUtils.isEmpty(appId)) {
            // throw error. wrong config
            throw new Error("wrong config");
        } else {
            Intercom.initialize(client.getApplication(), mobileApiKey, appId);
            Intercom.client().setLauncherVisibility(Visibility.VISIBLE);
            Intercom.client().setBottomPadding(40);
        }
        if (destinationConfig != null && destinationConfig.containsKey("customMappings")) {
            List<Object> eventList = (List<Object>) destinationConfig.get("customMappings");
            if (eventList != null && !eventList.isEmpty()) {
                for (Object item : eventList) {
                    Map<String, String> keyMap = (Map<String, String>) item;
                    if (keyMap != null && keyMap.containsKey("from") && keyMap.containsKey("to")) {
                        eventMap.put(keyMap.get("from"), keyMap.get("to"));
                    }
                }
            }
        }
        double delay = 0;
        if (destinationConfig != null && destinationConfig.containsKey("delay")) {
            Double delayTime = (Double) destinationConfig.get("delay");
            if (delayTime != null) {
                delay = delayTime;
            }
            if (delay < 0) {
                delay = 0;
            } else if (delay > 10) {
                delay = 10;
            }
        }

        Intercom.setLogLevel(rudderConfig.getLogLevel() >= RudderLogger.RudderLogLevel.DEBUG ? Intercom.LogLevel.VERBOSE : Intercom.LogLevel.ERROR);
    }


    private void processRudderEvent(RudderMessage element) {
        if (element != null && element.getType() != null) {
            switch (element.getType()) {
                case MessageType.TRACK:
                    //Track events of a particular user - track

                    String eventName = element.getEventName();

                     if(element.getProperties() != null){
                         Map<String, Object> eventData = new HashMap<>();
                         for (String key: eventData.keySet()){
                             eventData.put(key, eventData.get(key));
                         }
                         Intercom.client().logEvent(eventName, eventData);
                     }


                    break;
                case MessageType.IDENTIFY:
//                    Create/Update a user

                    UserAttributes.Builder userAttributes = new UserAttributes.Builder();
                    Company.Builder company = new Company.Builder();
                    userAttributes.withUserId(element.getUserId());

                    if (TextUtils.isEmpty(element.getUserId())) {
                        Intercom.client().registerUnidentifiedUser();
                    } else {
                        Registration registration = Registration.create().withUserId(element.getUserId());
                        Intercom.client().registerIdentifiedUser(registration);
                    }


                    Map<String, Object> eventProperties = element.getTraits();
                    if (eventProperties != null) {

                        for (String key : eventProperties.keySet()) {

                            if(key.toLowerCase() == "company" ){
                                List<Object> companyProperties = (List<Object>) eventProperties.get("company");
                                if(companyProperties != null){
                                    for (Object item : companyProperties) {
                                        Map<String, String> keyMap = (Map<String, String>) item;
                                        if (keyMap.containsKey("name")){
                                            company.withName(keyMap.get("name"));
                                        } else if (keyMap.containsKey("id")){
                                            company.withCompanyId(keyMap.get("id"));
                                        }else {
                                            company.withCustomAttribute(key, keyMap.get(key));
                                        }
                                    }

                                }
                            }
                            else {
                                if (key.toLowerCase() == "name"){
                                    userAttributes.withName(String.valueOf(eventProperties.get(key)));
                                }

                                if(key.toLowerCase() == "email"){
                                    userAttributes.withEmail(String.valueOf(eventProperties.get(key)));
                                }

                                if(key.toLowerCase() == "phone"){
                                    userAttributes.withPhone((String.valueOf(eventProperties.get(key))));
                                }

//                                if(key.toLowerCase() == "signedUpAt"){
//                                    long date = String.valueOf(eventProperties.get(key));
//                                    userAttributes.withSignedUpAt(date);
//                                }

                                if(key.toLowerCase() != "name" && key.toLowerCase() != "email"){
                                    userAttributes.withCustomAttribute(key, String.valueOf(eventProperties.get(key)));
                                }
                            }
                        }
                    }


//                    Requires rudder Option
//                    Intercom.client().setUserHash("your_hmac_of_user_id_or_email");

                    Intercom.client().updateUser(userAttributes.withCompany(company.build()).build());



                    break;
                case MessageType.SCREEN:
                    RudderLogger.logWarn("IntercomIntegrationFactory: MessageType is not supported");
                    break;
                default:
                    RudderLogger.logWarn("IntercomIntegrationFactory: MessageType is not specified");
                    break;
            }
        }
    }

    @Override
    public void reset() {
        Intercom.client().logout();
    }

    @Override
    public void dump(RudderMessage element) {
        processRudderEvent(element);
    }

    @Override
    public Intercom getUnderlyingInstance() {
        return Intercom.client();
    }
}
