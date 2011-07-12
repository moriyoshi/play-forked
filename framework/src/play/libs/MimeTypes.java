package play.libs;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MimeTypes utils
 */
public class MimeTypes {

    private static Properties mimetypes = null;
    private static Pattern extPattern;

    static {
        extPattern = Pattern.compile("^.*\\.([^.]+)$");
    }

    /**
     * return the mimetype from a file name
     * @param filename the file name
     * @return the mimetype or the empty string if not found
     */
    public static String getMimeType(String filename) {
        return getMimeType(filename, "");
    }

    /**
     * return the mimetype from a file name.<br/>
     * @param filename the file name
     * @param defaultMimeType the default mime type to return when no matching mimetype is found
     * @return the mimetype
     */
    public static String getMimeType(String filename, String defaultMimeType) {
        Matcher matcher = extPattern.matcher(filename.toLowerCase());
        String ext = "";
        if (matcher.matches()) {
            ext = matcher.group(1);
        }
        if (ext.length() > 0) {
            String mimeType = mimetypes().getProperty(ext);
            if (mimeType == null) {
                return defaultMimeType;
            }
            return mimeType;
        }
        return defaultMimeType;
    }

    /**
     * return the content-type from a file name. If none is found returning application/octet-stream<br/>
     * For a text-based content-type, also return the encoding suffix eg. <em>"text/plain; charset=utf-8"</em>
     * @param filename the file name
     * @return the content-type deduced from the file extension.
     */
    public static String getContentType(String filename){
        return getContentType(filename, "application/octet-stream");
    }

    /**
     * return the content-type from a file name.<br/>
     * For a text-based content-type, also return the encoding suffix eg. <em>"text/plain; charset=utf-8"</em>
     * @param filename the file name
     * @param defaultContentType the default content-type to return when no matching content-type is found
     * @return the content-type deduced from the file extension.
     */
    public static String getContentType(String filename, String defaultContentType){
        String contentType = getMimeType(filename, null);
        if (contentType == null){
            contentType =  defaultContentType;
        }
        if (contentType != null && contentType.startsWith("text/")){
            return contentType + "; charset=" + Http.Response.current().encoding;
        }
        return contentType;
    }

    /**
     * check the mimetype is referenced in the mimetypes database
     * @param mimeType the mimeType to verify
     */
    public static boolean isValidMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        } else if (mimeType.indexOf(";") != -1) {
            return mimetypes().contains(mimeType.split(";")[0]);
        } else {
            return mimetypes().contains(mimeType);
        }
    }

    private static synchronized void initMimetypes() {
        if (mimetypes != null) return;
        // Load default mimetypes from the framework
        try {
            InputStream is = MimeTypes.class.getClassLoader().getResourceAsStream("play/libs/mime-types.properties");
            mimetypes = new Properties();
            mimetypes.load(is);
        } catch (Exception ex) {
            Logger.warn("Failed to load MIME-type table", ex);
        }
        try {
            {
                Properties props = new Properties();
                props.load(MimeTypes.class.getClassLoader().getResourceAsStream("play/libs/canonical-mime-types.properties"));
                canonicalizationMap = props;
            }
            {
                Properties props = new Properties();
                props.load(MimeTypes.class.getClassLoader().getResourceAsStream("play/libs/mime-types-rev.properties"));
                reverseMap = props;
            }
        } catch (Exception ex) {
            Logger.warn("Failed to load reverse MIME-type table", ex);
        }
        // Load mimetypes from plugins
        for (PlayPlugin plugin: Play.pluginCollection.getEnabledPlugins()) {
            Map<String, String> pluginTypes = plugin.addMimeTypes();
            for (Map.Entry<String, String> entry: pluginTypes.entrySet()) {
                mimetypes.setProperty(entry.getKey(), entry.getValue());
                reverseMap.setProperty(entry.getValue(), entry.getKey());
            }
        }
        // Load custom mimetypes from the application configuration
        for (Map.Entry<Object, Object> entry: Play.configuration.entrySet()) {
            final String key = (String)entry.getKey();
            if (key.startsWith("mimetype.")) {
                final String type = key.substring(key.indexOf('.') + 1).toLowerCase();
                final String value = (String)entry.getValue();
                mimetypes.setProperty(type, value);
                reverseMap.setProperty(value, type);
            }
        }
    }

    private static Properties mimetypes() {
        if (mimetypes == null) {
            initMimetypes();
        }
        return mimetypes;
    }

    public static boolean isTextualMimeType(String mimeType) {
        // TODO: avoid hard-coding
        return mimeType.startsWith("text/") ||
                mimeType.equals("application/xhtml+xml") ||
                mimeType.equals("application/json");
    }

    public static String canonicalizeMimeType(String mimeType) {
        String retval = (String)canonicalizationMap.get(mimeType);
        if (retval == null)
            return mimeType;
        return retval;
    }

    public static String getExtension(String mimeType) {
        return (String)reverseMap.get(mimeType);
    }

    private static Properties reverseMap;

    private static Properties canonicalizationMap;
}
