/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.fastjson.JSON;
import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenReleaseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author hantianwei@gmail.com
 * @since 1.5.0
 */
@Slf4j
@Component("flowRuleApolloPublisher")
public class FlowRuleApolloPublisher implements DynamicRulePublisher<List<FlowRuleEntity>> {

    @Autowired
    private ApolloOpenApiClient apolloOpenApiClient;
    @Autowired
    private Converter<List<FlowRuleEntity>, String> converter;

    @Override
    public void publish(String app, List<FlowRuleEntity> rules) throws Exception {
        AssertUtil.notEmpty(app, "app name cannot be empty");
        if (rules == null) {
            return;
        }

        // Increase the configuration
        String appId = ApolloConfigUtil.APOLLO_APP_ID;
        String namespace = ApolloConfigUtil.getNamespace();

        String flowDataId = ApolloConfigUtil.getFlowDataId(app);
//        String flowDataId = app;
        OpenItemDTO openItemDTO = new OpenItemDTO();
        openItemDTO.setKey(flowDataId);
        openItemDTO.setValue(converter.convert(rules));
        openItemDTO.setComment("Program auto-join");
        // fixme 目前部署Apollo服务中只有apollo管理员，这里修改为apollo。
        //修改openItemDTO.setDataChangeCreatedBy()方法中的参数为“apollo”;
        //修改namespaceGrayDelReleaseDTO.setReleasedBy()方法中的参数为“apollo”;
        openItemDTO.setDataChangeCreatedBy("apollo");
        openItemDTO.setDataChangeLastModifiedBy("apollo");
        openItemDTO.setDataChangeCreatedTime(new Date());
        openItemDTO.setDataChangeLastModifiedTime(new Date());

        log.info("规则发布, appId: {}, namespace: {}, openItemDto: {}",
                appId, namespace, JSON.toJSONString(openItemDTO));
        apolloOpenApiClient.createOrUpdateItem(appId,  ApolloConfigUtil.getEnv(),
                ApolloConfigUtil.getCluster(), namespace, openItemDTO);

        // Release configuration
        NamespaceReleaseDTO namespaceReleaseDTO = new NamespaceReleaseDTO();
        namespaceReleaseDTO.setEmergencyPublish(true);
        namespaceReleaseDTO.setReleaseComment("Modify or add configurations");
        namespaceReleaseDTO.setReleasedBy("apollo");
        namespaceReleaseDTO.setReleaseTitle("Modify or add configurations");

        OpenReleaseDTO openReleaseDTO = apolloOpenApiClient.publishNamespace(appId,  ApolloConfigUtil.getEnv(),
                ApolloConfigUtil.getCluster(), namespace, namespaceReleaseDTO);
        log.info("规则发布, appId: {}, namespace: {}, npreleaseDto: {}, resp: {}",
                appId, namespace, JSON.toJSONString(namespaceReleaseDTO), JSON.toJSONString(openReleaseDTO));
    }
}
