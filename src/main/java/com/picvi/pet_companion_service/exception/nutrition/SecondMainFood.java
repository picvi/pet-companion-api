package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class SecondMainFood extends ApiException {
    public SecondMainFood() {
        super("A main food is already being given. Choose to feed both or to replace it", "SECOND_MAIN_FOOD");
    }
}
