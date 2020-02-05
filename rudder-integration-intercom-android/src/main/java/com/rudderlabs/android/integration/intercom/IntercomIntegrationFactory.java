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
import io.intercom.com.google.gson.Gson;

public class IntercomIntegrationFactory extends RudderIntegration<Intercom> {

    private static final String INTERCOM_KEY = Constants.INTERCOM;

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

        Intercom.setLogLevel(rudderConfig.getLogLevel() >= RudderLogger.RudderLogLevel.DEBUG ? Intercom.LogLevel.VERBOSE
                : Intercom.LogLevel.ERROR);

        String mobileApiKey = "";
        String appId = "";
        Map<String, Object> destinationConfig = (Map<String, Object>) config;

        if (destinationConfig != null && destinationConfig.containsKey(Constants.MOBILE_API_KEY)) {
            mobileApiKey = (String) destinationConfig.get(Constants.MOBILE_API_KEY);
        }

        if (destinationConfig != null && destinationConfig.containsKey(Constants.APP_ID)) {
            appId = (String) destinationConfig.get(Constants.APP_ID);
        }

        if (TextUtils.isEmpty(mobileApiKey) || TextUtils.isEmpty(appId)) {
            RudderLogger.logError(Constants.CONFIG_ERROR);
        } else {
            Intercom.initialize(client.getApplication(), mobileApiKey, appId);
            Intercom.client().setLauncherVisibility(Visibility.VISIBLE);
        }

    }

    private void processRudderEvent(RudderMessage element) {
        if (element != null && element.getType() != null) {
            switch (element.getType()) {
            case MessageType.TRACK:
                // Track events of a particular user - track

                String eventName = element.getEventName();
                Map<String, Object> eventData = element.getProperties();

                if (!TextUtils.isEmpty(element.getEventName()) || eventData != null) {

                    if (TextUtils.isEmpty(element.getEventName())) {
                        eventName = null;
                    }

                    if (eventData != null) {
                        for (String key : eventData.keySet()) {
                            eventData.put(key, eventData.get(key));
                        }
                    }
                    Intercom.client().logEvent(eventName, eventData);
                }

                break;
            case MessageType.IDENTIFY:
                // Create/Update a user

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

                        if (key.toLowerCase() == Constants.COMPANY) {
                            List<Object> companyProperties = (List<Object>) eventProperties.get(Constants.COMPANY);
                            if (companyProperties != null) {
                                for (Object item : companyProperties) {
                                    Map<String, String> keyMap = (Map<String, String>) item;
                                    if (keyMap.containsKey(Constants.NAME)) {
                                        company.withName(keyMap.get(Constants.NAME));
                                    } else if (keyMap.containsKey(Constants.ID)) {
                                        company.withCompanyId(keyMap.get(Constants.ID));
                                    } else {
                                        company.withCustomAttribute(key, keyMap.get(key));
                                    }
                                }

                            }
                        } else {
                            if (key.toLowerCase() == Constants.NAME) {
                                userAttributes.withName(String.valueOf(eventProperties.get(key)));
                            }

                            if (key.toLowerCase() == Constants.EMAIL) {
                                userAttributes.withEmail(String.valueOf(eventProperties.get(key)));
                            }

                            if (key.toLowerCase() == Constants.PHONE) {
                                userAttributes.withPhone((String.valueOf(eventProperties.get(key))));
                            }

                            // if(key.toLowerCase() == "signedUpAt"){
                            // long date = String.valueOf(eventProperties.get(key));
                            // userAttributes.withSignedUpAt(date);
                            // }

                            if (key.toLowerCase() != Constants.NAME && key.toLowerCase() != Constants.EMAIL) {
                                String json = new Gson().toJson(eventProperties.get(key));
                                userAttributes.withCustomAttribute(key, json);
                            }
                        }
                    }
                }

                // Requires rudder Option
                // Intercom.client().setUserHash("your_hmac_of_user_id_or_email");

                Intercom.client().updateUser(userAttributes.withCompany(company.build()).build());

                break;
            default:
                RudderLogger.logWarn(Constants.NOT_SUPPORTED_MESSAGE);
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
