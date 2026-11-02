package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.CancelSubscriptionUseCase;
import com.invoiceflow.domain.port.in.GetSubscriptionStatusUseCase;
import com.invoiceflow.domain.port.in.UpgradePlanUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {

    private final GetSubscriptionStatusUseCase getSubscriptionStatusUseCase;
    private final UpgradePlanUseCase upgradePlanUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;

    public SubscriptionController(
            GetSubscriptionStatusUseCase getSubscriptionStatusUseCase,
            UpgradePlanUseCase upgradePlanUseCase,
            CancelSubscriptionUseCase cancelSubscriptionUseCase) {
        this.getSubscriptionStatusUseCase = getSubscriptionStatusUseCase;
        this.upgradePlanUseCase = upgradePlanUseCase;
        this.cancelSubscriptionUseCase = cancelSubscriptionUseCase;
    }

    @GetMapping
    public ResponseEntity<SubscriptionStatusDto> getStatus(@AuthenticationPrincipal User user) {
        GetSubscriptionStatusUseCase.Result result = getSubscriptionStatusUseCase.getStatus(
                new GetSubscriptionStatusUseCase.Query(user.getId()));
        return ResponseEntity.ok(SubscriptionStatusDto.from(result));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionStatusDto> upgrade(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpgradePlanRequest request) {
        GetSubscriptionStatusUseCase.Result result = upgradePlanUseCase.upgrade(
                new UpgradePlanUseCase.Command(user.getId(), request.plan()));
        return ResponseEntity.ok(SubscriptionStatusDto.from(result));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionStatusDto> cancel(@AuthenticationPrincipal User user) {
        GetSubscriptionStatusUseCase.Result result = cancelSubscriptionUseCase.cancel(
                new CancelSubscriptionUseCase.Command(user.getId()));
        return ResponseEntity.ok(SubscriptionStatusDto.from(result));
    }

    public record UpgradePlanRequest(@NotNull(message = "Plan is required") Plan plan) {}

    public record SubscriptionStatusDto(String plan, int invoicesThisMonth, long totalClients) {
        static SubscriptionStatusDto from(GetSubscriptionStatusUseCase.Result result) {
            return new SubscriptionStatusDto(
                    result.plan().name(),
                    result.invoicesThisMonth(),
                    result.totalClients());
        }
    }
}
