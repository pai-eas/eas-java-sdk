package com.aliyun.openservices.eas.discovery.core;

import java.util.List;

public interface Listener {

    /**
     * 传入新的endpoint列表,不进行过滤
     *
     * @param endpoints
     */
    void onChange(List<Endpoint> endpoints);
}
