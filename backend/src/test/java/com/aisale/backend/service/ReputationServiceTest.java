package com.aisale.backend.service;

import com.aisale.backend.dto.AdminReputationAdjustRequest;
import com.aisale.backend.dto.ReputationAccountResponse;
import com.aisale.backend.dto.ReputationRecordResponse;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.entity.ReputationAccount;
import com.aisale.backend.entity.ReputationRecord;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.ErrorCode;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.exception.business.ValidationException;
import com.aisale.backend.repository.ReputationAccountRepository;
import com.aisale.backend.repository.ReputationRecordRepository;
import com.aisale.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReputationService 单元测试")
class ReputationServiceTest {

    @Mock
    private ReputationAccountRepository reputationAccountRepository;

    @Mock
    private ReputationRecordRepository reputationRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReputationService reputationService;

    private final Long USER_ID = 1L;
    private final String USERNAME = "testuser";
    private final Long REVIEW_ID = 100L;
    private final Long REVIEWEE_ID = 2L;

    private User testUser;
    private ReputationAccount testAccount;
    private OrderReview testReview;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);

        testAccount = new ReputationAccount();
        testAccount.setId(1L);
        testAccount.setUserId(USER_ID);
        testAccount.setTotalScore(100);
        testAccount.setLevel(3);
        testAccount.setTotalReviews(0);
        testAccount.setGoodReviews(0);
        testAccount.setNeutralReviews(0);
        testAccount.setBadReviews(0);
        testAccount.setAvgRating(0.0);

        testReview = new OrderReview();
        testReview.setId(REVIEW_ID);
        testReview.setRevieweeId(REVIEWEE_ID);
        testReview.setRating(5);
        testReview.setContent("非常好的交易");
        testReview.setReviewType(OrderReview.ReviewType.BUYER_REVIEW);
    }

    @Nested
    @DisplayName("获取或创建账户")
    class GetOrCreateAccountTests {

        @Test
        @DisplayName("新用户自动创建账户，初始100分，等级3")
        void getOrCreateAccount_NewUser() {
            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(reputationAccountRepository.save(any(ReputationAccount.class))).thenAnswer(invocation -> {
                ReputationAccount account = invocation.getArgument(0);
                account.setId(1L);
                return account;
            });

            ReputationAccount account = reputationService.getOrCreateAccount(USER_ID);

            assertNotNull(account);
            assertEquals(USER_ID, account.getUserId());
            assertEquals(100, account.getTotalScore());
            assertEquals(3, account.getLevel());

            verify(reputationAccountRepository).save(argThat(a ->
                a.getUserId().equals(USER_ID) &&
                a.getTotalScore() == 100 &&
                a.getLevel() == 3
            ));
        }

        @Test
        @DisplayName("已有账户直接返回")
        void getOrCreateAccount_ExistingUser() {
            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            ReputationAccount account = reputationService.getOrCreateAccount(USER_ID);

            assertNotNull(account);
            assertEquals(testAccount.getId(), account.getId());
            assertEquals(100, account.getTotalScore());

            verify(reputationAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("获取账户概览")
    class GetAccountOverviewTests {

        @Test
        @DisplayName("成功获取账户概览")
        void getAccountOverview_Success() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            ReputationAccountResponse response = reputationService.getAccountOverview(USERNAME);

            assertNotNull(response);
            assertEquals(USER_ID, response.getUserId());
            assertEquals(100, response.getTotalScore());
            assertEquals(3, response.getLevel());
        }

        @Test
        @DisplayName("用户不存在抛出 NotFoundException")
        void getAccountOverview_UserNotFound() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                reputationService.getAccountOverview(USERNAME);
            });

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getErrorCode());
        }
    }
}