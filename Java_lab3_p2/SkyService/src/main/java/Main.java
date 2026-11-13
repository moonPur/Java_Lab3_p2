import billing.Billing;
import exception.InvalidSubscriptionException;
import exception.SubscriptionNotFoundException;
import model.CorporateSubscription;
import model.Option;
import model.PromoCode;
import model.Subscription;
import model.UserSubscription;
import net.datafaker.Faker;
import servise.NotificationService;
import servise.impl.EmailNotification;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        NotificationService emailService = new EmailNotification();
        Billing billing = new Billing(emailService);

        // 1. Генерация 50 пользователей через Datafaker (со случайными Option и PromoCode)
        generateTestUsers(billing, 50);

        // 2. Демонстрация аналитики через Stream API
        System.out.println("=== АНАЛИТИКА STREAM API ===");
        System.out.println("Всего записей в базе: " + billing.getDatabase().size());
        System.out.println("Общая прогнозируемая выручка: " + billing.calculateTotalRevenue() + " руб.");
        System.out.println("Количество PREMIUM подписок: " + billing.countByPlan("PREMIUM"));

        // Поиск VIP-клиентов (чек > 1000 руб.)
        List<Subscription> vipClients = billing.findVipClients(1000.0);
        System.out.println("VIP-клиентов (чек > 1000 руб.): " + vipClients.size());
        vipClients.stream()
                .limit(5) // Выводим первых 5 для наглядности
                .forEach(sub -> System.out.printf("  * %s [%s] — %.2f руб. (опций: %d)%n",
                        sub.getOwnerName(), sub.getPlanName(), sub.calculateBillingPrice(), sub.getOptions().size()));

        // Группировка по тарифам
        System.out.println("\nРаспределение по тарифам:");
        Map<String, List<Subscription>> grouped = billing.groupSubscriptionsByPlan();
        grouped.forEach((plan, subs) -> System.out.printf("  - Тариф %s: %d чел.%n", plan, subs.size()));

        // 3. Демонстрация оплаты и обработки исключений (из ЛР №3 ч.1)
        System.out.println("\n=== ПРОВЕРКА ОБРАБОТКИ ИСКЛЮЧЕНИЙ ===");
        try {
            // Берем первый попавшийся реальный email из сгенерированной базы и проводим оплату
            String existingEmail = billing.getDatabase().keySet().iterator().next();
            billing.processPaymentByEmail(existingEmail);

            // Пытаемся оплатить несуществующий email (вызовет SubscriptionNotFoundException)
            billing.processPaymentByEmail("non_existent_user@sky.ru");
        } catch (SubscriptionNotFoundException e) {
            System.out.println("[Перехвачено проверяемое исключение]: " + e.getMessage());
        }

        try {
            // Пытаемся создать подписку с отрицательной ценой (вызовет InvalidSubscriptionException)
            new UserSubscription("Тест Ошибки", -500.0, "BASIC");
        } catch (InvalidSubscriptionException e) {
            System.out.println("[Перехвачено непроверяемое исключение]: " + e.getMessage());
        }
    }

    /**
     * Метод-генератор тестовых пользователей с помощью библиотеки Datafaker.
     */
    private static void generateTestUsers(Billing billing, int count) {
        Faker faker = new Faker(new Locale("ru"));
        String[] plans = {"BASIC", "STANDARD", "PREMIUM"};
        String[] availableOptions = {"Качество 4K", "Без рекламы", "Спорт-пакет", "Семейный доступ", "Офлайн-режим"};

        for (int i = 0; i < count; i++) {
            // Добавляем индекс i в начало, чтобы гарантировать 100% уникальность ключей в HashMap
            String email = i + "_" + faker.internet().emailAddress();
            String name = faker.name().fullName();
            String plan = plans[faker.random().nextInt(plans.length)];
            double basePrice = faker.number().randomDouble(2, 200, 600);

            Subscription sub;
            // Каждый 5-й клиент — корпоративный (демонстрация @SuperBuilder)
            if (i % 5 == 0) {
                sub = CorporateSubscription.builder()
                        .ownerName(faker.company().name())
                        .baseMonthlyPrice(basePrice)
                        .planName(plan)
                        .activeLicensesCount(faker.number().numberBetween(2, 15))
                        .build();
            } else {
                sub = UserSubscription.builder()
                        .ownerName(name)
                        .baseMonthlyPrice(basePrice)
                        .planName(plan)
                        .build();

                // Добавляем от 1 до 3 случайных опций (Option)
                int optionsCount = faker.number().numberBetween(1, 4);
                for (int j = 0; j < optionsCount; j++) {
                    String optName = availableOptions[faker.random().nextInt(availableOptions.length)];
                    double optPrice = faker.number().numberBetween(50, 150);
                    sub.addOption(Option.builder()
                            .name(optName)
                            .price(optPrice)
                            .build());
                }

                // С вероятностью 50% применяем один промокод (PromoCode)
                if (faker.bool().bool()) {
                    sub.setAppliedPromo(PromoCode.builder()
                            .code("SKY" + faker.number().digits(2))
                            .discountPercent(faker.number().numberBetween(10, 30))
                            .build());
                }
            }

            billing.registerSubscription(email, sub);
        }
    }
}
