package com.picvi.pet_companion_service.exception;

public class UserDoesNotExist extends ApiException {
  public UserDoesNotExist() {
      super("User does not exist", "USER_DOES_NOT_EXIST");
    }
}
