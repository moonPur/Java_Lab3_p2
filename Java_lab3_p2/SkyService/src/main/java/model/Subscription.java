package model;

import exception.InvalidSubscriptionException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@SuperBuilder
public abstract class Subscription {
    private final String ownerName;
    private final double baseMonthlyPrice;
    private final String planName;

    @Builder.Default
    private final List<Option> options = new ArrayList<>();

    @Setter
    private PromoCode appliedPromo;

    protected Subscription(String ownerName, double baseMonthlyPrice, String planName) {
        if (baseMonthlyPrice <= 0 || ownerName == null || ownerName.isBlank()) {
            throw new InvalidSubscriptionException("Некорректные данные подписки");
        }
        this.ownerName = ownerName;
        this.baseMonthlyPrice = baseMonthlyPrice;
        this.planName = planName != null ? planName.toUpperCase() : "STANDARD";
        this.options = new ArrayList<>();
    }

    public void addOption(Option option) {
        if (option == null) {
            throw new IllegalArgumentException("Опция не может быть null");
        }
        options.add(option);
    }

    // Вспомогательный метод для подсчета суммы всех опций через Stream API
    protected double calculateOptionsTotal() {
        return options.stream()
                .mapToDouble(Option::getPrice)
                .sum();
    }

    public abstract double calculateBillingPrice();
}
