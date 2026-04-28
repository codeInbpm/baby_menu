package com.babymenu.service;

import com.babymenu.entity.ServiceRequest;
import java.util.List;

public interface RequestService {
    ServiceRequest create(List<Long> itemIds);
    List<ServiceRequest> list(Integer status);
    ServiceRequest detail(Long id);
    ServiceRequest accept(Long id);
    ServiceRequest reject(Long id);
    ServiceRequest finish(Long id);
}
