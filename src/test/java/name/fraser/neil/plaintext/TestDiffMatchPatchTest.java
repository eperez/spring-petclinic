package name.fraser.neil.plaintext;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;

public class TestDiffMatchPatchTest {

    private final diff_match_patch diff_match_patch = new diff_match_patch();
    {
        diff_match_patch.Match_Threshold = 0.1f;
    }

    @Test
    public void testDiffMatchPatchTest() throws Exception {
        final LinkedList<diff_match_patch.Patch> patches = diff_match_patch.patch_make(
            "This is a long line of text",
            "This is a long line of boring text");
        final Object[] objects = diff_match_patch.patch_apply(patches, "That is a long line of text");
        final String patched = (String) objects[0];
        final boolean[] successes = (boolean[]) objects[1];
        Assert.assertEquals("That is a long line of boring text", patched);
        Assert.assertArrayEquals(new boolean[]{true}, successes);
    }

    @Test
    public void testDiffMatchPatchConflictTest() throws Exception {
        final LinkedList<diff_match_patch.Patch> patches = diff_match_patch.patch_make("Test 1", "Test 2");
        final Object[] objects = diff_match_patch.patch_apply(patches, "Test 3");
        final boolean[] successes = (boolean[]) objects[1];
        Assert.assertArrayEquals(new boolean[]{false}, successes);
    }

}
