package com.aliyun.openservices.eas.discovery.core;

import java.util.List;

public interface Listener {

    void onChange(List<Endpoint> endpoints);
}
