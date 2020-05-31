package com.rudderlabs.android.integration.intercom;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderContext;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;
import com.rudderstack.android.sdk.core.RudderProperty;
import com.rudderstack.android.sdk.core.RudderTraits;

import java.awt.font.TextAttribute;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.UserAttributes;
import io.intercom.android.sdk.Intercom.Visibility;
import io.intercom.android.sdk.identity.Registration;
import io.intercom.android.sdk.Company;
import io.intercom.com.google.gson.Gson;

public class IntercomIntegrationFactory extends RudderIntegration<Intercom> {
    private static final String INTERCOM_KEY = "Intercom";
    private static final String MOBILE_API_KEY = "mobileApiKeyAndroid";
    private static final String APP_ID = "appId";
    private static final String USER_ID = "userId";
    private static final String CONFIG_ERROR = "wrong config";
    private static final String COMPANY = "company";
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";
    private static final String CREATED_AT = "createdAt";
    private static final String NOT_SUPPORTED_MESSAGE = "IntercomIntegrationFactory: MessageType is not supported";

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

        if (destinationConfig != null && destinationConfig.containsKey(MOBILE_API_KEY)) {
            mobileApiKey = (String) destinationConfig.get(MOBILE_API_KEY);
        }

        if (destinationConfig != null && destinationConfig.containsKey(APP_ID)) {
            appId = (String) destinationConfig.get(APP_ID);
        }

        if (TextUtils.isEmpty(mobileApiKey) || TextUtils.isEmpty(appId)) {
            RudderLogger.logError(CONFIG_ERROR);
        } else {
            Intercom.initialize(client.getApplication(), mobileApiKey, appId);
            Intercom.client().setLauncherVisibility(Visibility.VISIBLE);
        }

        RudderContext context = client.getRudderContext();
        String userId = null;
        if (context != null && context.getTraits() != null) {
            Map<String, Object> traits = context.getTraits();
            if (traits.containsKey(USER_ID)) {
                userId = (String) traits.get(USER_ID);
            } else if (traits.containsKey(ID)) {
                userId = (String) traits.get(ID);
            }
        }
        if (!TextUtils.isEmpty(userId)) {
            Registration registration = Registration.create().withUserId(userId);
            Intercom.client().registerIdentifiedUser(registration);
        } else {
            Intercom.client().registerUnidentifiedUser();
        }
    }

    private void processRudderEvent(@NonNull RudderMessage element) {
        if (element.getType() != null) {
            switch (element.getType()) {
                case MessageType.TRACK:
                    if (element.getProperties() != null) {
                        Intercom.client().logEvent(element.getEventName(), element.getProperties());
                    } else {
                        Intercom.client().logEvent(element.getEventName());
                    }
                    break;
                case MessageType.IDENTIFY:
                    UserAttributes.Builder userAttributesBuilder = new UserAttributes.Builder();

                    if (!TextUtils.isEmpty(element.getUserId())) {
                        Registration registration = Registration.create().withUserId(element.getUserId());
                        Intercom.client().registerIdentifiedUser(registration);
                    } else {
                        Intercom.client().registerUnidentifiedUser();
                    }

                    Map<String, Object> userTraits = element.getTraits();
                    if (userTraits != null) {
                        for (String key : userTraits.keySet()) {
                            if (key.toLowerCase().equals(COMPANY)) {
                                Map<String, Object> companyProperties = (Map<String, Object>) userTraits.get(COMPANY);
                                if (companyProperties != null) {
                                    Company.Builder companyBuilder = new Company.Builder();
                                    for (String companyKey : companyProperties.keySet()) {
                                        if (companyKey.equalsIgnoreCase(NAME)) {
                                            companyBuilder.withName((String) companyProperties.get(companyKey));
                                        } else if (companyKey.equalsIgnoreCase(ID)) {
                                            companyBuilder.withCompanyId((String) companyProperties.get(companyKey));
                                        } else {
                                            companyBuilder.withCustomAttribute(companyKey, companyProperties.get(companyKey));
                                        }
                                    }
                                    userAttributesBuilder.withCompany(companyBuilder.build());
                                }
                            } else if (key.toLowerCase().equals(NAME)) {
                                userAttributesBuilder.withName(String.valueOf(userTraits.get(key)));
                            } else if (key.toLowerCase().equals(EMAIL)) {
                                userAttributesBuilder.withEmail(String.valueOf(userTraits.get(key)));
                            } else if (key.toLowerCase().equals(PHONE)) {
                                userAttributesBuilder.withPhone((String.valueOf(userTraits.get(key))));
                            } else if (key.toLowerCase().equals(CREATED_AT)) {
                                try {
                                    String createdAt = (String) userTraits.get(key);
                                    userAttributesBuilder.withSignedUpAt(
                                            new SimpleDateFormat("YYYY-MM-ddTHH:mm:ss.SSSZ", Locale.US)
                                                    .parse(createdAt == null ? "" : createdAt));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                userAttributesBuilder.withCustomAttribute(key, userTraits.get(key));
                            }
                        }
                    }
                    Intercom.client().updateUser(userAttributesBuilder.build());
                    break;
                default:
                    RudderLogger.logDebug(NOT_SUPPORTED_MESSAGE);
                    break;
            }
        }
    }

    @Override
    public void reset() {
        Intercom.client().logout();
    }

    @Override
    public void dump(@Nullable RudderMessage element) {
        if (element != null) {
            processRudderEvent(element);
        }
    }

    @Override
    public Intercom getUnderlyingInstance() {
        return Intercom.client();
    }
}
