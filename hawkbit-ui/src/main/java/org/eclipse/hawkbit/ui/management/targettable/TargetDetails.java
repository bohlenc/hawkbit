/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.providers.TargetMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyKeyValueDetails;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetAttributesDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractGridDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.KeyValueDetailsComponent;
import org.eclipse.hawkbit.ui.common.detailslayout.MetadataDetailsGrid;
import org.eclipse.hawkbit.ui.common.tagdetails.TargetTagToken;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Target details layout which is shown on the Deployment View.
 */
public class TargetDetails extends AbstractGridDetailsLayout<ProxyTarget> {
    private static final long serialVersionUID = 1L;

    private static final String TARGET_ID_PREFIX = "target.";

    private final transient TargetManagement targetManagement;
    private final transient DeploymentManagement deploymentManagement;

    private final TargetAttributesDetailsComponent attributesLayout;
    private final KeyValueDetailsComponent assignedDsDetails;
    private final KeyValueDetailsComponent installedDsDetails;
    private final transient TargetTagToken targetTagToken;
    private final MetadataDetailsGrid<String> targetMetadataGrid;

    private final transient TargetMetaDataWindowBuilder targetMetaDataWindowBuilder;

    TargetDetails(final CommonUiDependencies uiDependencies, final TargetTagManagement tagManagement,
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final TargetMetaDataWindowBuilder targetMetaDataWindowBuilder) {
        super(uiDependencies.getI18n());

        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.targetMetaDataWindowBuilder = targetMetaDataWindowBuilder;

        this.attributesLayout = buildAttributesLayout();

        this.assignedDsDetails = buildAssignedDsDetails();

        this.installedDsDetails = buildInstalledDsDetails();

        this.targetTagToken = new TargetTagToken(uiDependencies, tagManagement, targetManagement);

        addDetailsComponents(Arrays.asList(new SimpleEntry<>(i18n.getMessage("caption.tab.details"), entityDetails),
                new SimpleEntry<>(i18n.getMessage("caption.tab.description"), entityDescription),
                new SimpleEntry<>(i18n.getMessage("caption.attributes.tab"), attributesLayout),
                new SimpleEntry<>(i18n.getMessage("header.target.assigned"), assignedDsDetails),
                new SimpleEntry<>(i18n.getMessage("header.target.installed"), installedDsDetails),
                new SimpleEntry<>(i18n.getMessage("caption.tags.tab"), getTargetTagToken().getTagPanel()),
                new SimpleEntry<>(i18n.getMessage("caption.logs.tab"), logDetails)));

        if (uiDependencies.getPermChecker().hasReadRepositoryPermission()) {
            this.targetMetadataGrid = new MetadataDetailsGrid<>(i18n, uiDependencies.getEventBus(),
                    UIComponentIdProvider.TARGET_TYPE_PREFIX, this::showMetadataDetails,
                    new TargetMetaDataDataProvider(targetManagement));
            addDetailsComponent(new SimpleEntry<>(i18n.getMessage("caption.metadata"), targetMetadataGrid));
        } else {
            this.targetMetadataGrid = null;
        }

        buildDetails();
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.TARGET_DETAILS_TABSHEET;
    }

    @Override
    protected List<ProxyKeyValueDetails> getEntityDetails(final ProxyTarget entity) {
        return Arrays.asList(
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_CONTROLLER_ID, i18n.getMessage("label.target.id"),
                        entity.getControllerId()),
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_TYPE_ID, i18n.getMessage("label.target.type"),
                        entity.getTypeInfo() != null ? entity.getTypeInfo().getName() : ""),
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_LAST_QUERY_DT,
                        i18n.getMessage("label.target.lastpolldate"),
                        SPDateTimeUtil.getFormattedDate(entity.getLastTargetQuery())),
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_IP_ADDRESS, i18n.getMessage("label.ip"),
                        entity.getAddress() != null ? entity.getAddress().toString() : ""),
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_SECURITY_TOKEN,
                        i18n.getMessage("label.target.security.token"), entity.getSecurityToken()));
    }

    @Override
    protected String getDetailsDescriptionId() {
        return UIComponentIdProvider.TARGET_DETAILS_DESCRIPTION_ID;
    }

    private TargetAttributesDetailsComponent buildAttributesLayout() {
        final TargetAttributesDetailsComponent attributesDetails = new TargetAttributesDetailsComponent(i18n,
                targetManagement);

        binder.forField(attributesDetails).bind(target -> {
            final String controllerId = target.getControllerId();
            final boolean isRequestAttributes = target.isRequestAttributes();

            final List<Map.Entry<String, String>> targetAttributes = targetManagement
                    .getControllerAttributes(controllerId).entrySet().stream().collect(Collectors.toList());

            final List<ProxyKeyValueDetails> attributes = IntStream.range(0, targetAttributes.size())
                    .mapToObj(i -> new ProxyKeyValueDetails("target.attributes.label" + i,
                            targetAttributes.get(i).getKey(), targetAttributes.get(i).getValue()))
                    .collect(Collectors.toList());

            return new ProxyTargetAttributesDetails(controllerId, isRequestAttributes, attributes);
        }, null);

        return attributesDetails;
    }

    private KeyValueDetailsComponent buildAssignedDsDetails() {
        final KeyValueDetailsComponent assignedDsLayout = new KeyValueDetailsComponent();

        binder.forField(assignedDsLayout).bind(target -> {
            final Optional<DistributionSet> targetAssignedDs = deploymentManagement
                    .getAssignedDistributionSet(target.getControllerId());

            return targetAssignedDs.map(this::getDistributionDetails).orElse(null);
        }, null);

        return assignedDsLayout;
    }

    private List<ProxyKeyValueDetails> getDistributionDetails(final DistributionSet ds) {
        final List<ProxyKeyValueDetails> dsDetails = Arrays.asList(
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_ASSIGNED_DS_NAME_ID,
                        i18n.getMessage("label.name"), ds.getName()),
                new ProxyKeyValueDetails(UIComponentIdProvider.TARGET_ASSIGNED_DS_VERSION_ID,
                        i18n.getMessage("label.version"), ds.getVersion()));

        final Stream<ProxyKeyValueDetails> dsSmDetailsStream = ds.getModules().stream()
                .map(swModule -> new ProxyKeyValueDetails("target.assigned.ds.sm.id." + swModule.getId(),
                        swModule.getType().getName(), swModule.getName() + ":" + swModule.getVersion()));

        return Stream.concat(dsDetails.stream(), dsSmDetailsStream).collect(Collectors.toList());
    }

    private KeyValueDetailsComponent buildInstalledDsDetails() {
        final KeyValueDetailsComponent installedDsLayout = new KeyValueDetailsComponent();

        binder.forField(installedDsLayout).bind(target -> {
            final Optional<DistributionSet> targetInstalledDs = deploymentManagement
                    .getInstalledDistributionSet(target.getControllerId());

            return targetInstalledDs.map(this::getDistributionDetails).orElse(null);
        }, null);

        return installedDsLayout;
    }

    @Override
    protected String getLogLabelIdPrefix() {
        return TARGET_ID_PREFIX;
    }

    private void showMetadataDetails(final ProxyMetaData metadata) {
        if (binder.getBean() == null) {
            return;
        }

        final Window metaDataWindow = targetMetaDataWindowBuilder.getWindowForShowTargetMetaData(
                binder.getBean().getControllerId(), binder.getBean().getName(), metadata);

        UI.getCurrent().addWindow(metaDataWindow);
        metaDataWindow.setVisible(Boolean.TRUE);
    }

    @Override
    public void masterEntityChanged(final ProxyTarget entity) {
        super.masterEntityChanged(entity);

        if (targetMetadataGrid != null) {
            targetMetadataGrid.masterEntityChanged(entity != null ? entity.getControllerId() : null);
        }
        targetTagToken.masterEntityChanged(entity);
    }

    /**
     * @return Target tag token
     */
    public TargetTagToken getTargetTagToken() {
        return targetTagToken;
    }
}
