package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.FoodModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FoodPolicyTest {
    private static final Nutrients N = new Nutrients(new BigDecimal("100"), new BigDecimal("10"), null, new BigDecimal("2"));
    @Test void catalogValuesScaleFromSnapshotAndPreserveUnknown() {
        Food food = new Food("7", "catalog", "Rice", null, N, "dataset", "license", "scope", false, null, "{}", "rice");
        Item item = FoodPolicy.entry(new EntryCommand("k", "Asia/Shanghai", "lunch", "catalog", "7", null, new BigDecimal("150"), "g", null, null), food);
        assertThat(item.consumedGrams()).isEqualByComparingTo("150");
        assertThat(item.nutrients().caloriesKcal()).isEqualByComparingTo("150");
        assertThat(item.nutrients().proteinG()).isNull();
    }
    @Test void quickEntryRequiresNutritionAndMassUnitsCannotDrift() {
        assertThatThrownBy(() -> FoodPolicy.entry(new EntryCommand("k", null, "snack", "quick", null, "Tea", null, null, null, Nutrients.unknown()), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FoodPolicy.conversion("g", new BigDecimal("2"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void fingerprintIsStable() {
        assertThat(FoodPolicy.fingerprint("a", 1)).isEqualTo(FoodPolicy.fingerprint("a", 1));
    }
    @Test void defaultMealTypeUsesSnackForOvernightAndBetweenMeals() {
        assertThat(FoodPolicy.defaultMealType(4)).isEqualTo("snack");
        assertThat(FoodPolicy.defaultMealType(5)).isEqualTo("breakfast");
        assertThat(FoodPolicy.defaultMealType(12)).isEqualTo("lunch");
        assertThat(FoodPolicy.defaultMealType(16)).isEqualTo("snack");
        assertThat(FoodPolicy.defaultMealType(19)).isEqualTo("dinner");
        assertThat(FoodPolicy.defaultMealType(22)).isEqualTo("snack");
    }
    @Test void editingUsesExistingSnapshotAndKeepsUnknowns() {
        Item old = new Item("1","lunch","catalog","7","Rice",null,new BigDecimal("100"),"g",BigDecimal.ONE,new BigDecimal("100"),N,N,"rice");
        Item changed=FoodPolicy.edit(old,new EditCommand(new BigDecimal("250"),null,null,null));
        assertThat(changed.nutrients().caloriesKcal()).isEqualByComparingTo("250");
        assertThat(changed.nutrients().proteinG()).isNull();
        assertThat(changed.per100g()).isSameAs(N);
        assertThat(changed.assetKey()).isEqualTo("rice");
    }
    @Test void summarySeparatesKnownIntakeFromCompleteTotalsAndPreservesTargets() {
        LocalDate date=LocalDate.of(2026,10,19);
        Nutrients target=new Nutrients(new BigDecimal("2200"),null,null,null);
        DayState state=new DayState("1",date,"open","Asia/Shanghai",target);
        Item item=new Item("1","lunch","quick",null,"Lunch",null,null,null,null,null,Nutrients.unknown(),N,null);
        Day result=FoodPolicy.summary(date,state,new Goal("2",date,N),List.of(item));
        assertThat(result.targets()).isSameAs(target);
        assertThat(result.totals().proteinG()).isNull();
        assertThat(result.knownTotals().proteinG()).isNull();
        assertThat(result.missingCounts().proteinG()).isEqualByComparingTo("1");
        Day empty=FoodPolicy.summary(date,state,null,List.of());
        assertThat(empty.totals().caloriesKcal()).isEqualByComparingTo("0");
    }
    @Test void completeDaysRequireExplicitReopen() {
        DayState closed=new DayState("1",LocalDate.now(),"complete","Asia/Shanghai",Nutrients.unknown());
        assertThatThrownBy(() -> FoodPolicy.open(closed)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Reopen");
    }
}
