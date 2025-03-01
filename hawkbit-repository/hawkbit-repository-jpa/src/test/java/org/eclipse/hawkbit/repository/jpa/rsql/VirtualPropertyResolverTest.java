/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@ExtendWith(SpringExtension.class)
@Feature("Unit Tests - Repository")
@Story("Placeholder resolution for virtual properties")
public class VirtualPropertyResolverTest {

    @Spy
    private final VirtualPropertyResolver resolverUnderTest = new VirtualPropertyResolver();

    @MockBean
    private TenantConfigurationManagement confMgmt;

    private StrSubstitutor substitutor;

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:07:37").build();

    @Configuration
    static class Config {
        @Bean
        TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
            return TenantConfigurationManagementHolder.getInstance();
        }
    }

    @BeforeEach
    public void before() {
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);

        substitutor = new StrSubstitutor(resolverUnderTest, StrSubstitutor.DEFAULT_PREFIX,
                StrSubstitutor.DEFAULT_SUFFIX, StrSubstitutor.DEFAULT_ESCAPE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "${NOW_TS}", "${OVERDUE_TS}", "${overdue_ts}" })
    @Description("Tests resolution of NOW_TS by using a StrSubstitutor configured with the VirtualPropertyResolver.")
    void resolveNowTimestampPlaceholder(final String placeholder) {
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("'%s' placeholder was not replaced", placeholder)
                .doesNotContain(placeholder);
    }

    @Test
    @Description("Tests VirtualPropertyResolver with a placeholder unknown to VirtualPropertyResolver.")
    public void handleUnknownPlaceholder() {
        final String placeholder = "${unknown}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("unknown should not be resolved!").contains(placeholder);
    }

    @Test
    @Description("Tests escape mechanism for placeholders (syntax is $${SOME_PLACEHOLDER}).")
    public void handleEscapedPlaceholder() {
        final String placeholder = "${OVERDUE_TS}";
        final String escaptedPlaceholder = StrSubstitutor.DEFAULT_ESCAPE + placeholder;
        final String testString = "lhs=lt=" + escaptedPlaceholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("Escaped OVERDUE_TS should not be resolved!").contains(placeholder);
    }
}
