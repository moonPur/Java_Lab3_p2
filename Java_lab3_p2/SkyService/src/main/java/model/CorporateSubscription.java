package model;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString(callSuper = true)
@SuperBuilder
public class CorporateSubscription extends Subscription {
    private final int activeLicensesCount;

    public CorporateSubscription(String ownerName, double baseMonthlyPrice, String planName, int activeLicensesCount) {
        super(ownerName, baseMonthlyPrice, planName);
        this.activeLicensesCount = Math.max(1, activeLicensesCount);
    }

    @Override
    public double calculateBillingPrice() {
        final double planModifier = switch (getPlanName()) {
            case "BASIC" -> 1.0;
            case "STANDARD" -> 1.30;
            case "PREMIUM" -> 1.70;
            default -> 1.0;
        };

        // Опции подключаются на каждую корпоративную лицензию
        double singleLicensePrice = (getBaseMonthlyPrice() * planModifier) + calculateOptionsTotal();
        double finalPrice = singleLicensePrice * activeLicensesCount;

        return Math.round(finalPrice * 100.0) / 100.0;
    }
}
