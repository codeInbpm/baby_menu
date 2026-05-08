package com.babymenu.service;

import com.babymenu.dto.RequestCreateDTO;
import com.babymenu.dto.RequestEvaluateDTO;
import com.babymenu.entity.ServiceRequest;
import java.util.List;

public interface RequestService {
    ServiceRequest create(RequestCreateDTO dto);
    List<ServiceRequest> list(Integer status);
    ServiceRequest detail(Long id);
    ServiceRequest accept(Long id);
    ServiceRequest reject(Long id);
    ServiceRequest finish(Long id);
    ServiceRequest evaluate(Long id, RequestEvaluateDTO dto);
}
