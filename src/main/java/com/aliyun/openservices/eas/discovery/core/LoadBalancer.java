package com.aliyun.openservices.eas.discovery.core;

import com.alibaba.middleware.ushura.Chooser;
import com.alibaba.middleware.ushura.Pair;
import com.aliyun.openservices.eas.discovery.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    protected static List<Endpoint> filterAndDuplicate(List<Endpoint> endpoints) {

        if (endpoints == null) {
            return null;
        }

        List<Endpoint> duplicated = new ArrayList<Endpoint>();


        for (Endpoint endpoint : endpoints) {
            if (!endpoint.isValid()) {
                continue;
            }

            for (int i = 0; i < endpoint.getWeight(); i++) {
                duplicated.add(endpoint);
            }
        }

        return duplicated;
    }

    /**
     * Return one host from the host list by random-weight.
     *
     * @param endpoints The list of the host.
     * @return The random-weight result of the host
     */
    protected static Endpoint getHostByRandomWeight(List<Endpoint> endpoints) {
        DiscoveryClient.LOG.debug("entry randomWithWeight");
        if (endpoints == null || endpoints.size() == 0) {
            DiscoveryClient.LOG.debug("endpoints == null || endpoints.size() == 0");
            return null;
        }

        Chooser<String, Endpoint> vipChooser = new Chooser<>("www.taobao.com");

        DiscoveryClient.LOG.debug("new Chooser");

        List<Pair<Endpoint>> hostsWithWeight = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.isValid()) {
                hostsWithWeight.add(new Pair<>(endpoint, endpoint.getWeight()));
            }
        }
        DiscoveryClient.LOG.debug("for (Endpoint host : endpoints)");
        vipChooser.refresh(hostsWithWeight);
        DiscoveryClient.LOG.debug("vipChooser.refresh");
        return vipChooser.randomWithWeight();
    }

    public static class RR {

        public static List<Endpoint> selectAll(Service Service) {
            List<Endpoint> endpoints = useAddressServerIfNecessary(Service);

            if (CollectionUtils.isEmpty(endpoints)) {
                throw new IllegalStateException("no host to srv for Service: " + Service.getName());
            }

            return filterAndDuplicate(endpoints);
        }


        public static List<Endpoint> useAddressServerIfNecessary(Service Service) {
            return Service.getEndpoints();
        }

        public static Endpoint selectHost(Service Service) {

            List<Endpoint> endpoints = useAddressServerIfNecessary(Service);

            if (CollectionUtils.isEmpty(endpoints)) {
                throw new IllegalStateException("no host to srv for Service: " + Service.getName());
            }

            FlowControl.enter(Service.getName());
            return getHostByRandomWeight(endpoints);
        }

        public static List<Endpoint> nothing(Service dom) {
            return dom.getEndpoints();
        }
    }
}
