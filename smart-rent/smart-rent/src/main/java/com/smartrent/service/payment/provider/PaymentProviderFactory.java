package com.smartrent.service.payment.provider;

import com.smartrent.enums.PaymentProvider;
import com.smartrent.infra.exception.PaymentProviderException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating payment provider instances
 * Uses the Factory pattern to abstract provider creation
 */
@Component
public class PaymentProviderFactory {

    private final List<com.smartrent.service.payment.provider.PaymentProvider> paymentProviders;
    private final Map<PaymentProvider, com.smartrent.service.payment.provider.PaymentProvider> providerMap;

    public PaymentProviderFactory(List<com.smartrent.service.payment.provider.PaymentProvider> paymentProviders) {
        this.paymentProviders = paymentProviders;
        this.providerMap = paymentProviders.stream()
                .collect(Collectors.toMap(
                        com.smartrent.service.payment.provider.PaymentProvider::getProviderType,
                        Function.identity()
                ));
    }

    /**
     * Get payment provider by type
     */
    public com.smartrent.service.payment.provider.PaymentProvider getProvider(PaymentProvider providerType) {
        com.smartrent.service.payment.provider.PaymentProvider provider = providerMap.get(providerType);
        if (provider == null) {
            throw PaymentProviderException.providerNotFound(providerType.toString());
        }

        if (!provider.validateConfiguration()) {
            throw PaymentProviderException.invalidConfiguration(providerType.toString());
        }

        return provider;
    }

    /**
     * Get all available payment providers
     */
    public List<com.smartrent.service.payment.provider.PaymentProvider> getAllProviders() {
        return paymentProviders.stream()
                .filter(com.smartrent.service.payment.provider.PaymentProvider::validateConfiguration)
                .collect(Collectors.toList());
    }

    /**
     * Check if provider is available and configured
     */
    public boolean isProviderAvailable(PaymentProvider providerType) {
        com.smartrent.service.payment.provider.PaymentProvider provider = providerMap.get(providerType);
        return provider != null && provider.validateConfiguration();
    }

    /**
     * Get providers that support a specific feature
     */
    public List<com.smartrent.service.payment.provider.PaymentProvider> getProvidersByFeature(
            com.smartrent.service.payment.provider.PaymentProvider.PaymentFeature feature) {
        return paymentProviders.stream()
                .filter(provider -> provider.validateConfiguration())
                .filter(provider -> provider.supportsFeature(feature))
                .collect(Collectors.toList());
    }

    /**
     * Get default payment provider
     */
    public com.smartrent.service.payment.provider.PaymentProvider getDefaultProvider() {
        // Return VNPay as default, or first available provider
        if (isProviderAvailable(PaymentProvider.VNPAY)) {
            return getProvider(PaymentProvider.VNPAY);
        }

        return getAllProviders().stream()
                .findFirst()
                .orElseThrow(() -> new PaymentProviderException("No payment providers are available"));
    }

    /**
     * Get provider configuration schemas
     */
    public Map<PaymentProvider, Map<String, Object>> getProviderSchemas() {
        return paymentProviders.stream()
                .collect(Collectors.toMap(
                        com.smartrent.service.payment.provider.PaymentProvider::getProviderType,
                        com.smartrent.service.payment.provider.PaymentProvider::getConfigurationSchema
                ));
    }
}
