package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private PetService petService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        this.service = new AccountDeletionService(this.usersRepository, this.nutritionApi, this.petService,
                this.refreshTokenService);
    }

    @Test
    void removesEverythingInOrder() {
        when(this.usersRepository.existsById(USER_ID)).thenReturn(true);
        when(this.petService.deleteAllOwnedBy(USER_ID)).thenReturn(1);

        this.service.delete(USER_ID);

        InOrder order = inOrder(this.nutritionApi, this.petService, this.refreshTokenService, this.usersRepository);
        order.verify(this.nutritionApi).forgetUser(USER_ID);
        order.verify(this.petService).deleteAllOwnedBy(USER_ID);
        order.verify(this.refreshTokenService).deleteAll(USER_ID);
        order.verify(this.usersRepository).deleteById(USER_ID);
    }

    @Test
    void unknownUserTouchesNothing() {
        when(this.usersRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> this.service.delete(USER_ID)).isInstanceOf(UserDoesNotExist.class);

        verifyNoInteractions(this.nutritionApi, this.petService, this.refreshTokenService);
    }
}
