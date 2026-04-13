package com.College.timetable.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.util.HtmlUtils;

import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * Global input sanitization — strips HTML/script tags from all string fields
 * in JSON request bodies before they reach controllers.
 *
 * Prevents stored XSS attacks by escaping dangerous characters:
 *   <script>alert(1)</script>  →  &lt;script&gt;alert(1)&lt;/script&gt;
 */
@RestControllerAdvice
public class InputSanitizationAdvice extends RequestBodyAdviceAdapter {

    private final ObjectMapper objectMapper;

    public InputSanitizationAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Apply to all controllers
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
                                MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        try {
            // Convert to JSON tree, sanitize all string values, convert back
            JsonNode tree = objectMapper.valueToTree(body);
            sanitizeNode(tree);
            return objectMapper.treeToValue(tree, body.getClass());
        } catch (Exception e) {
            // If sanitization fails, return original body — don't break the request
            return body;
        }
    }

    private void sanitizeNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> fieldNames = obj.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode value = obj.get(field);
                if (value.isTextual()) {
                    String sanitized = HtmlUtils.htmlEscape(value.asText().trim());
                    obj.set(field, new TextNode(sanitized));
                } else if (value.isObject() || value.isArray()) {
                    sanitizeNode(value);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode item = arr.get(i);
                if (item.isTextual()) {
                    arr.set(i, new TextNode(HtmlUtils.htmlEscape(item.asText().trim())));
                } else if (item.isObject() || item.isArray()) {
                    sanitizeNode(item);
                }
            }
        }
    }
}
