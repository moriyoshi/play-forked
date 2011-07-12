package play.utils;

import java.util.Map;
import java.util.HashMap;

public class HTTP {
    public static class ParameterizedHeaderValue {
        public final String value;
        public final Map<String, String> parameters;

        public ParameterizedHeaderValue(String value, Map<String, String> parameters) {
            this.value = value;
            this.parameters = parameters;
        }
    }

    public static class ContentTypeWithEncoding {
        public final String contentType;
        public final String encoding;

        public ContentTypeWithEncoding(String contentType, String encoding) {
            this.contentType = contentType;
            this.encoding = encoding;
        }
    }

    public static ParameterizedHeaderValue parseParameterizedHeaderValue(String value) {
        final String[] parts = value.split(";");
        final Map<String, String> params = new HashMap<String, String>();
        final String _value = parts[0].trim().toLowerCase();
        for (int i = 1; i < parts.length; i++) {
            final String[] paramParts = parts[i].split("\\s*=\\s*", 2);
            if (paramParts.length == 2) {
                params.put(paramParts[0].trim(), paramParts[1].trim());
            } else {
                params.put(paramParts[0].trim(), null);
            }
        }
        return new ParameterizedHeaderValue(_value, params);
    }

    public static ContentTypeWithEncoding parseContentType(String contentType) {
        return parseContentType(contentType, "text/html", null);
    }

    public static ContentTypeWithEncoding parseContentType(String contentType, String defaultContentType, String defaultEncoding) {
        if (contentType == null) {
            return new ContentTypeWithEncoding(defaultContentType.intern(), defaultEncoding);
        } else {
            final ParameterizedHeaderValue v = parseParameterizedHeaderValue(contentType);
            final String encoding = v.parameters.get("charset");
            return new ContentTypeWithEncoding(v.value, encoding == null ? defaultEncoding: encoding);
        }
    }
}
