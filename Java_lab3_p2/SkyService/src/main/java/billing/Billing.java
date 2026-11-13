package billing;

import exception.SubscriptionNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import model.Subscription;
import servise.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Billing {
    private final NotificationService notificationService;

    @Getter
    private final Map<String, Subscription> database = new HashMap<>();

    public void registerSubscription(String email, Subscription sub) {
        database.put(email, sub);
    }

    public void processPaymentByEmail(String email) throws SubscriptionNotFoundException {
        Subscription sub = database.get(email);
        if (sub == null) {
            throw new SubscriptionNotFoundException("Пользователь с email " + email + " не найден!");
        }
        double finalPrice = sub.calculateBillingPrice();
        String message = String.format("Счет оплачен. Тариф [%s]. Списано: %.2f руб.",
                sub.getPlanName(), finalPrice);
        notificationService.sendNotification(message, email);
    }

    // 1. Поиск VIP-клиентов с чеком выше заданной суммы (Stream API)
    public List<Subscription> findVipClients(double minAmount) {
        return database.values().stream()
                .filter(sub -> sub.calculateBillingPrice() > minAmount)
                .toList();
    }

    // 2. Агрегация общей выручки по всей базе (Stream API + ссылка на метод)
    public double calculateTotalRevenue() {
        double total = database.values().stream()
                .mapToDouble(Subscription::calculateBillingPrice)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }

    // 3. Подсчет количества подписок определенного тарифа (Stream API)
    public long countByPlan(String planName) {
        return database.values().stream()
                .filter(sub -> sub.getPlanName().equalsIgnoreCase(planName))
                .count();
    }

    // 4. Группировка клиентов по тарифам (Collectors.groupingBy)
    public Map<String, List<Subscription>> groupSubscriptionsByPlan() {
        return database.values().stream()
                .collect(Collectors.groupingBy(Subscription::getPlanName));
    }
}
