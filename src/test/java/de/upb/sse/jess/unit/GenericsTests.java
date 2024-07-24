package de.upb.sse.jess.unit;

import de.upb.sse.jess.util.ImportUtil;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.List;

public class GenericsTests {

    @Test
    void testGenericTypeSplitting1() {
        List<String> genericTypes = ImportUtil.getGenericTypes("Map<String, Set<ByteString>>");
        assertThat(genericTypes, hasSize(4));
        assertThat(genericTypes, containsInAnyOrder("Map", "String", "Set", "ByteString"));
    }

    @Test
    void testGenericTypeSplitting2() {
        List<String> genericTypes = ImportUtil.getGenericTypes("Map<String<Integer extends Long>>");
        assertThat(genericTypes, hasSize(4));
        assertThat(genericTypes, containsInAnyOrder("Map", "String", "Integer", "Long"));
    }

    @Test
    void testGenericTypeSplitting3() {
        List<String> genericTypes = ImportUtil.getGenericTypes("Map<?>");
        assertThat(genericTypes, hasSize(1));
        assertThat(genericTypes, containsInAnyOrder("Map"));
    }
}
