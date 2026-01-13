package com.cinema.commonservice.advice;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.commonservice.model.RestResponse;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Format tất cả response thành công thành cấu trúc chuẩn
 * Response sẽ có dạng:
 * {
 * "statusCode": 200,
 * "message": "Call Api Success",
 * "data": {...}
 * }
 */
@ControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();

        // Không format String và Resource (tránh lỗi conversion)
        if (body instanceof String || body instanceof Resource) {
            return body;
        }

        // Không format error response (đã được ExceptionAdvice xử lý)
        // Error response từ ExceptionAdvice sẽ giữ nguyên format
        if (status >= 400) {
            return body;
        }

        // Format success response
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(status);
        res.setData(body);

        // Lấy message từ @ApiMessage annotation hoặc dùng message mặc định
        ApiMessage message = returnType.getMethodAnnotation(ApiMessage.class);
        res.setMessage(message != null ? message.value() : "Call Api Success");

        return res;
    }
}
