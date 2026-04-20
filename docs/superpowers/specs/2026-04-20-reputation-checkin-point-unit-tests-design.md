# Reputation/CheckIn/Point 模块单元测试设计

## 一、测试目标

为 `ReputationService`、`CheckInService`、`PointService` 三个新模块编写详细的单元测试，验证核心业务逻辑的正确性、异常处理、边界条件及事务一致性。

## 二、测试策略：混合策略

| 测试类型 | 适用场景 | 工具 |
|---------|---------|------|
| Mockito 单元测试 | 业务逻辑、异常处理、边界条件 | `@ExtendWith(MockitoExtension.class)` |
| 集成测试 | 事务一致性、数据库约束验证 | `@DataJpaTest` + H2 |

**混合策略原因**：
- 信誉模块涉及事务性要求（评价→信誉更新在同一事务）
- 信誉分不能为负数的约束需要数据库层面验证
- 积分扣减需要验证余额不足的边界条件

## 三、测试文件结构

```
backend/src/test/java/com/aisale/backend/service/
├── ReputationServiceTest.java        # Mockito单元测试
├── CheckInServiceTest.java           # Mockito单元测试
├── PointServiceTest.java             # Mockito单元测试
└── integration/
    └── ReputationIntegrationTest.java # 集成测试
```

---

## 四、ReputationService 单元测试

### 4.1 测试用例清单

| 测试场景 | 测试方法 | 验证点 |
|---------|---------|-------|
| 获取/创建账户 | `getOrCreateAccount_NewUser` | 新用户自动创建账户，初始100分，等级3 |
| 获取/创建账户 | `getOrCreateAccount_ExistingUser` | 已有账户直接返回 |
| 获取账户概览 | `getAccountOverview_Success` | 用户名查询返回正确数据 |
| 获取账户概览 | `getAccountOverview_UserNotFound` | 用户不存在抛出 NotFoundException |
| 信誉计算 | `calculateScoreChange_Rating5` | 5分 → +10 |
| 信誉计算 | `calculateScoreChange_Rating4` | 4分 → +5 |
| 信誉计算 | `calculateScoreChange_Rating3` | 3分 → +0 |
| 信誉计算 | `calculateScoreChange_Rating2` | 2分 → -5 |
| 信誉计算 | `calculateScoreChange_Rating1` | 1分 → -10 |
| 评价更新信誉 | `updateReputationFromReview_Success` | 评价后信誉正确更新，创建流水记录 |
| 评价更新信誉 | `updateReputationFromReview_GoodReview` | 好评数+1，信誉分增加 |
| 评价更新信誉 | `updateReputationFromReview_BadReview` | 差评数+1，信誉分扣减 |
| 信誉分下限 | `updateReputationFromReview_NegativeScoreProtection` | 信誉分扣减后不能为负 |
| 管理员调整 | `adminAdjust_Success` | 正向调整成功 |
| 管理员调整 | `adminAdjust_NegativeAdjust` | 负向调整成功 |
| 管理员调整 | `adminAdjust_ZeroScore` | 调整分数为0抛出 ValidationException |
| 管理员调整 | `adminAdjust_EmptyReason` | 原因为空抛出 ValidationException |
| 等级计算 | `calculateLevel_NoReviews` | 无评价 → 3级 |
| 等级计算 | `calculateLevel_Level5` | avgRating>=4.8 且 reviews>=50 → 5级 |
| 等级计算 | `calculateLevel_Level4` | avgRating>=4.5 且 reviews>=20 → 4级 |
| 等级计算 | `calculateLevel_Level2` | avgRating<4.0 → 2级 |
| 等级计算 | `calculateLevel_Level1` | avgRating<3.0 → 1级 |
| 流水查询 | `getRecords_Success` | 分页查询返回正确数据 |

### 4.2 测试数据准备

```java
private final Long USER_ID = 1L;
private final Long REVIEW_ID = 100L;
private final Long REVIEWEE_ID = 2L;
private User testUser;
private ReputationAccount testAccount;
private OrderReview testReview;
```

---

## 五、CheckInService 单元测试

### 5.1 测试用例清单

| 测试场景 | 测试方法 | 验证点 |
|---------|---------|-------|
| 签到成功 | `checkIn_Success` | 创建签到记录，调用 PointService.addPoints |
| 今日已签到 | `checkIn_AlreadyCheckedIn` | 抛出 ConflictException |
| 连续签到计算 | `calculateContinuousDays_FirstDay` | 首次签到 → 连续1天 |
| 连续签到计算 | `calculateContinuousDays_Continuous` | 昨天签到 → 连续天数+1 |
| 连续签到计算 | `calculateContinuousDays_Broken` | 昨天未签到 → 重置为1天 |
| 积分奖励计算 | `calculateRewardPoints_Day1` | 第1天 → 5分 |
| 积分奖励计算 | `calculateRewardPoints_Day2To6` | 第2-6天 → 10分 |
| 积分奖励计算 | `calculateRewardPoints_Day7Plus` | 第7天及以上 → 20分 |
| 签到状态查询 | `getCheckInStatus_Success` | 返回今日是否签到、连续天数、累计天数 |
| 签到状态查询 | `getCheckInStatus_TodayCheckedIn` | 今日已签到，返回正确状态 |
| 签到状态查询 | `getCheckInStatus_NotCheckedIn` | 今日未签到，连续天数基于昨日 |
| 签到日历 | `getCheckInCalendar_Success` | 返回指定月份签到日期列表 |
| 用户不存在 | `checkIn_UserNotFound` | 抛出 NotFoundException |

### 5.2 测试数据准备

```java
private final Long USER_ID = 1L;
private final String USERNAME = "testuser";
private final LocalDate TODAY = LocalDate.now();
private User testUser;
private CheckIn testCheckIn;
```

---

## 六、PointService 单元测试

### 6.1 测试用例清单

| 测试场景 | 测试方法 | 验证点 |
|---------|---------|-------|
| 获取/创建账户 | `getOrCreateAccount_NewUser` | 新用户自动创建账户，初始积分0 |
| 获取/创建账户 | `getOrCreateAccount_ExistingUser` | 已有账户直接返回 |
| 获取账户概览 | `getAccountOverview_Success` | 返回积分统计、签到次数、订单次数 |
| 增加积分 | `addPoints_Success` | 积分增加，创建流水记录 |
| 增加积分 | `addPoints_ZeroOrNegative` | 积分<=0 抛出 ValidationException |
| 扣减积分 | `deductPoints_Success` | 积分扣减，创建流水记录 |
| 扣减积分 | `deductPoints_InsufficientBalance` | 余额不足抛出 ValidationException |
| 扣减积分 | `deductPoints_AccountNotFound` | 账户不存在抛出 NotFoundException |
| 扣减积分 | `deductPoints_ZeroOrNegative` | 扣减<=0 抛出 ValidationException |
| 管理员调整 | `adminAdjustPoints_AddPositive` | 正向调整调用 addPoints |
| 管理员调整 | `adminAdjustPoints_DeductNegative` | 负向调整调用 deductPoints |
| 管理员调整 | `adminAdjustPoints_ZeroPoints` | 调整为0抛出 ValidationException |
| 管理员调整 | `adminAdjustPoints_EmptyReason` | 原因为空抛出 ValidationException |
| 查询可用积分 | `getAvailablePoints_Success` | 返回正确可用积分 |
| 查询可用积分 | `getAvailablePoints_NoAccount` | 无账户返回0 |
| 流水查询 | `getPointRecords_AllTypes` | 查询所有类型流水 |
| 流水查询 | `getPointRecords_ByType` | 按类型筛选流水 |

### 6.2 测试数据准备

```java
private final Long USER_ID = 1L;
private final String USERNAME = "testuser";
private final int POINTS = 10;
private User testUser;
private PointAccount testAccount;
private PointRecord testRecord;
```

---

## 七、集成测试：ReputationIntegrationTest

### 7.1 测试场景

| 测试场景 | 验证点 |
|---------|-------|
| 评价→信誉事务一致性 | 评价创建和信誉更新在同一事务，任一失败全部回滚 |
| 信誉分下限保护 | 差评扣减后信誉分不能为负（最小0） |
| 账户唯一性约束 | user_id UNIQUE 约束生效 |
| 流水记录完整性 | balance_after 与账户余额一致 |

### 7.2 测试配置

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReputationIntegrationTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReputationAccountRepository accountRepository;

    @Autowired
    private ReputationRecordRepository recordRepository;
}
```

---

## 八、测试覆盖率目标

| 模块 | 目标覆盖率 |
|-----|-----------|
| ReputationService | >= 90% |
| CheckInService | >= 85% |
| PointService | >= 85% |

重点覆盖：
- 所有公共方法
- 所有异常分支
- 所有边界条件

---

## 九、依赖的测试工具

| 工具 | 版本 | 用途 |
|-----|-----|-----|
| JUnit 5 | 5.x | 测试框架 |
| Mockito | 5.x | Mock依赖 |
| AssertJ | 3.x | 断言库（可选，使用JUnit断言） |
| Spring Data JPA Test | - | 集成测试 |

---

## 十、执行命令

```bash
cd backend

# 运行所有新模块测试
mvn test -Dtest=ReputationServiceTest,CheckInServiceTest,PointServiceTest,ReputationIntegrationTest

# 运行并生成覆盖率报告
mvn test jacoco:report
```