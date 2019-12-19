package runwar.options;

import org.junit.jupiter.api.Test;
import runwar.LaunchUtil;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaVersionTest {

    @Test
    public void testTypes() {

        assertTrue(LaunchUtil.versionLowerThanOrEqualTo("1.8.0","1.8.0"));

        assertTrue(LaunchUtil.versionLowerThanOrEqualTo("1.8.0.77","1.8"));
    }

}
