package lsfusion.server.logics;

import lsfusion.server.classes.NumericClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.sets.ResolveClassSet;

import java.util.Arrays;
import java.util.List;

public class PropertyCompoundNameUtils {
    static public final String signatureLBracket = "[";
    static public final String signatureRBracket = "]";

    static public final String commonStringClassName = "STRING";
    static public final String commonNumericClassName = "NUMERIC";

    static public final String UNKNOWNCLASS = "?";

    static public final String classDataPropPrefix = "_CLASS_";
    static public final String policyPropPrefix = "_POLICY_";
    static public final String fullPropPrefix = "_FULL_";
    static public final String logPropPrefix = "_LOG_";
    static public final String logDropPropPrefix = "_LOGDROP_";
    static public final String drillDownPrefix = "_DRILLDOWN_";
    static public final String objValuePrefix = "_OBJVALUE_";

    static public String createName(String namespace, String name, ResolveClassSet... signature) {
        return createName(namespace, name, Arrays.asList(signature));
    }

    /*  Позволяет создавать канонические имена, а также часть канонического имени, передавая
     *  null в качестве пространства имен либо сигнатуры         
     */
    static public String createName(String namespace, String name, List<ResolveClassSet> signature) {
        StringBuilder builder = new StringBuilder();
        if (namespace != null) {
            appendNamespace(builder, namespace);
        }
        builder.append(name);
        if (signature != null) {
            appendSignature(builder, signature);
        }
        return builder.toString();
    }

    static private void appendNamespace(StringBuilder builder, String namespace) {
        builder.append(namespace);
        builder.append(CanonicalNameUtils.DELIMITER);
    }

    static private void appendSignature(StringBuilder builder, List<ResolveClassSet> signature) {
        builder.append(signatureLBracket);
        boolean isFirst = true;
        for (ResolveClassSet cs : signature) {
            if (!isFirst) {
                builder.append(",");
            }
            isFirst = false;
            if (cs instanceof StringClass) {
                builder.append(commonStringClassName);
            } else if (cs instanceof NumericClass) {
                builder.append(commonNumericClassName);
            } else if (cs != null) {
                builder.append(cs.getCanonicalName());
            } else {
                builder.append(UNKNOWNCLASS);
            }
        }
        builder.append(signatureRBracket);
    }
}