package lsfusion.server.logics;

import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.FixedSizeUnderscoreDBNamingPolicy;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class FixedSizeUnderscoreDBNamingPolicyTest {
    @Test
    public void testTransformPropertyCNToDBName() {
        DBNamingPolicy policy = new FixedSizeUnderscoreDBNamingPolicy(63, "_auto");
        assertEquals(policy.transformActionOrPropertyCNToDBName("NS.name[NS.cls,?,NS.cls2"), "NS_name_NS_cls_null_NS_cls2");
        assertEquals(policy.transformActionOrPropertyCNToDBName("NS.name[NS.cls,?,STRING[10]]"), "NS_name_NS_cls_null_STRING_10");
    } 
}