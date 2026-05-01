package com.festiva.util;

import com.festiva.user.api.UserPreferenceService;

@Deprecated
@SuppressWarnings("unused")
public class UserDateService extends com.festiva.user.api.UserDateService {

    public UserDateService(UserPreferenceService userPreferenceService) {
        super(userPreferenceService);
    }
}
