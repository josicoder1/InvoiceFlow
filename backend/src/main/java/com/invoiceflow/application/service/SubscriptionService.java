package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.CancelSubscriptionUseCase;
import com.invoiceflow.domain.port.in.GetSubscriptionStatusUseCase;
import com.invoiceflow.domain.port.in.UpgradePlanUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class SubscriptionService implements
        GetSubscriptionStatusUseCase,
        UpgradePlanUseCase,
        CancelSubscriptionUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    public SubscriptionService(
            UserRepositoryPort userRepositoryPort,
            InvoiceRepositoryPort invoiceRepositoryPort,
            ClientRepositoryPort clientRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public GetSubscriptionStatusUseCase.Result getStatus(GetSubscriptionStatusUseCase.Query query) {
        User user = loadUser(query.userId());
        return buildResult(user);
    }

    @Override
    @Transactional
    public GetSubscriptionStatusUseCase.Result upgrade(UpgradePlanUseCase.Command command) {
        User user = loadUser(command.userId());
        user.updatePlan(command.plan());
        userRepositoryPort.save(user);
        return buildResult(user);
    }

    @Override
    @Transactional
    public GetSubscriptionStatusUseCase.Result cancel(CancelSubscriptionUseCase.Command command) {
        User user = loadUser(command.userId());
        user.updatePlan(Plan.FREE);
        userRepositoryPort.save(user);
        return buildResult(user);
    }

    private User loadUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private GetSubscriptionStatusUseCase.Result buildResult(User user) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        int invoicesThisMonth = (int) invoiceRepositoryPort.countByUserIdAndCreatedAtBetween(
                user.getId(), monthStart, monthEnd);
        long totalClients = clientRepositoryPort.countByUserId(user.getId());
        return new GetSubscriptionStatusUseCase.Result(user.getPlan(), invoicesThisMonth, totalClients);
    }
}
